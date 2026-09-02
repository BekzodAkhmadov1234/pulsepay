package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.fee.FeeRuleEntity;
import uz.pulsepay.domain.fee.FeePayer;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.identity.UserEntity;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.domain.party.InstrumentEntity;
import uz.pulsepay.domain.party.Instrument;
import uz.pulsepay.domain.party.PartyType;
import uz.pulsepay.domain.reference.TransferTypeEntity;
import uz.pulsepay.domain.reference.TransferType;
import uz.pulsepay.repository.FeeRuleRepository;
import uz.pulsepay.repository.InstrumentRepository;
import uz.pulsepay.repository.TransferParticipantRepository;
import uz.pulsepay.repository.TransferRepository;
import uz.pulsepay.repository.TransferStatusHistoryRepository;
import uz.pulsepay.repository.TransferTypeRepository;
import uz.pulsepay.repository.UserRepository;
import uz.pulsepay.domain.routing.TransferRoute;
import uz.pulsepay.domain.shared.CurrencyCode;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.domain.transfer.TransferEntity;
import uz.pulsepay.domain.transfer.TransferParticipantEntity;
import uz.pulsepay.domain.transfer.TransferStatusHistoryEntity;
import uz.pulsepay.domain.transfer.ParticipantRole;
import uz.pulsepay.domain.transfer.Transfer;
import uz.pulsepay.domain.transfer.TransferChannel;
import uz.pulsepay.domain.transfer.TransferParticipant;
import uz.pulsepay.domain.transfer.TransferStateMachine;
import uz.pulsepay.domain.transfer.TransferStatus;
import uz.pulsepay.domain.transfer.TransferStatusHistory;
import uz.pulsepay.domain.transfer.TransferSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core transfer business logic.
 *
 * Merges: InitiateTransferUseCase, ConfirmTransferOtpUseCase, GetTransferUseCase,
 *         ListTransfersUseCase, ListTransferSummariesUseCase,
 *         TransferOrchestrationService, TransferParticipantService.
 *
 * CRITICAL: confirmOtp() runs within ONE @Transactional boundary (Risk #1).
 */
@Slf4j
@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final TransferTypeRepository transferTypeRepository;
    private final FeeRuleRepository feeRuleRepository;
    private final IdempotencyService idempotencyService;
    private final FeeService feeService;
    private final LimitService limitService;
    private final NetworkTransactionService networkTransactionService;
    private final LedgerService ledgerService;
    private final ComplianceService complianceService;
    private final RoutingService routingService;
    private final TransferOtpService transferOtpService;

    public TransferService(
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository,
            UserRepository userRepository,
            InstrumentRepository instrumentRepository,
            TransferTypeRepository transferTypeRepository,
            FeeRuleRepository feeRuleRepository,
            IdempotencyService idempotencyService,
            FeeService feeService,
            LimitService limitService,
            NetworkTransactionService networkTransactionService,
            LedgerService ledgerService,
            ComplianceService complianceService,
            RoutingService routingService,
            TransferOtpService transferOtpService) {
        this.transferRepository       = transferRepository;
        this.participantRepository    = participantRepository;
        this.historyRepository        = historyRepository;
        this.userRepository           = userRepository;
        this.instrumentRepository     = instrumentRepository;
        this.transferTypeRepository   = transferTypeRepository;
        this.feeRuleRepository        = feeRuleRepository;
        this.idempotencyService       = idempotencyService;
        this.feeService               = feeService;
        this.limitService             = limitService;
        this.networkTransactionService = networkTransactionService;
        this.ledgerService            = ledgerService;
        this.complianceService        = complianceService;
        this.routingService           = routingService;
        this.transferOtpService       = transferOtpService;
    }

    // ── Initiate Transfer (P2P) ───────────────────────────────────────────────

    @Transactional
    public Transfer initiate(UUID senderId, UUID senderInstrumentId, String senderCardNetwork,
                             UUID recipientId, UUID recipientInstrumentId, String recipientCardNetwork,
                             Money amount, int transferTypeId, Integer purposeCodeId,
                             TransferChannel channel, String idempotencyKey) {

        log.info("Initiating transfer: sender={}, amount={}, channel={}", senderId, amount, channel);

        // 1. Idempotency guard
        idempotencyService.claimKey(idempotencyKey, senderId, idempotencyKey);

        // 2. Validate sender
        User sender = userRepository.findById(senderId)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Sender not found or inactive"));

        // 3. Validate instrument ownership (Risk #3)
        Instrument senderInstrument   = validateInstrumentOwnership(senderInstrumentId, senderId);
        Instrument recipientInstrument = validateInstrumentOwnership(recipientInstrumentId, recipientId);

        // 4. Transfer type
        TransferType transferType = transferTypeRepository.findById(transferTypeId)
                .map(TransferTypeEntity::toDomain)
                .filter(TransferType::isActive)
                .orElseThrow(() -> new DomainException("Transfer type not available"));

        // 5. Channel invariant (Risk #10)
        validateChannel(channel, transferType);

        // 6. Party-type invariant (Risk #4)
        validatePartyTypes(PartyType.PERSON.name().toLowerCase(),
                           PartyType.PERSON.name().toLowerCase(), transferType);

        // 7. Limit check
        limitService.checkLimits(senderId, sender.kycLevel(), amount, transferTypeId);

        // 8. Fee calculation
        Instant now = Instant.now();
        var feeResult = feeService.calculate(amount, transferTypeId,
                senderCardNetwork, recipientCardNetwork, amount.currency().name(), now);
        Money feeAmount = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId = feeResult.map(r -> r.appliedRule().id()).orElse(null);

        // 9. Route resolution
        TransferRoute route = routingService.resolve(senderCardNetwork, recipientCardNetwork,
                transferTypeId, amount);

        // 10. Persist transfer
        Transfer transfer = transferRepository.save(TransferEntity.fromDomain(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.OTP_PENDING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), transferTypeId, purposeCodeId,
                channel, Instant.now(), null))).toDomain();

        // 11. Persist participants
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                senderId, PartyType.PERSON, senderInstrumentId,
                senderInstrument.instrumentType(), Instant.now())));
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                recipientId, PartyType.PERSON, recipientInstrumentId,
                recipientInstrument.instrumentType(), Instant.now())));

        // 12. Status history
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.OTP_PENDING, "Transfer initiated", Instant.now())));

        // 13. Generate OTP
        transferOtpService.generate(senderId, transfer.id());

        log.info("Transfer created: id={}, status=OTP_PENDING, amount={}, fee={}",
                transfer.id(), transfer.amount(), feeAmount);
        return transfer;
    }

    // ── Confirm OTP (Risk #1 — all in ONE @Transactional) ────────────────────

    @Transactional
    public Transfer confirmOtp(UUID transferId, UUID userId, String otpCode) {
        log.info("OTP confirmation: transferId={}, userId={}", transferId, userId);

        Transfer transfer = transferRepository.findById(transferId)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));

        // State machine guard: only OTP_PENDING → PROCESSING is legal here
        TransferStateMachine.assertTransition(transfer.status(), TransferStatus.PROCESSING);

        // Verify OTP (59s expiry, 3-attempt cap, 15-min lockout)
        transferOtpService.verify(userId, otpCode, transferId);

        // Status → PROCESSING
        transferRepository.updateStatus(transferId, TransferStatus.PROCESSING, null);
        transfer = transferRepository.findById(transferId)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new DomainException("Transfer disappeared after status update"));
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.OTP_PENDING, TransferStatus.PROCESSING, "OTP confirmed", Instant.now())));

        // Get participants
        TransferParticipant sender = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.SENDER)
                .map(TransferParticipantEntity::toDomain)
                .orElseThrow(() -> new DomainException("Sender participant not found"));
        TransferParticipant recipient = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.RECIPIENT)
                .map(TransferParticipantEntity::toDomain)
                .orElseThrow(() -> new DomainException("Recipient participant not found"));

        // Reload fee rule to determine fee_payer
        FeeRule appliedRule = transfer.appliedFeeRuleId() != null
                ? feeRuleRepository.findById(transfer.appliedFeeRuleId())
                        .map(FeeRuleEntity::toDomain).orElse(null)
                : null;

        FeePayer feePayer = appliedRule != null ? appliedRule.feePayer() : FeePayer.SENDER;
        String feeRecipient = appliedRule != null ? appliedRule.feeRecipient().name() : "PLATFORM";

        // Compute debit/credit amounts
        Money debitAmount;
        if (feePayer == FeePayer.SENDER) {
            debitAmount = transfer.amount().add(transfer.feeAmount());
        } else {
            log.warn("Non-sender fee_payer '{}' not yet fully routed; debiting principal only", feePayer);
            debitAmount = transfer.amount();
        }
        Money creditAmount = transfer.amount();

        // Resolve processor from applied route
        String processorName = transfer.appliedRouteId() != null
                ? routingService.findById(transfer.appliedRouteId())
                        .map(r -> r.processorName()).orElse("uzcard")
                : "uzcard";

        // Execute network transfer
        networkTransactionService.execute(transferId,
                sender.instrumentId(), recipient.instrumentId(),
                debitAmount, creditAmount, processorName);

        // Post ledger entries
        ledgerService.postTransferEntries(transferId, transfer.amount(), transfer.feeAmount(),
                sender.instrumentType().name().toLowerCase(),
                recipient.instrumentType().name().toLowerCase(),
                feeRecipient);

        // Increment limit counters
        limitService.increment(userId, transfer.amount(), transfer.transferTypeId());

        // Compliance evaluation
        complianceService.evaluate(transferId, sender.partyId(), transfer.amount());

        // Status → COMPLETED
        transferRepository.updateStatus(transferId, TransferStatus.COMPLETED, Instant.now());
        transfer = transferRepository.findById(transferId)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new DomainException("Transfer disappeared after completion"));
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.PROCESSING, TransferStatus.COMPLETED, "Network transfer successful",
                Instant.now())));

        log.info("Transfer completed: id={}, amount={}", transferId, transfer.amount());
        return transfer;
    }

    // ── Get Transfer ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Transfer getById(UUID transferId, UUID requestingUserId) {
        Transfer transfer = transferRepository.findById(transferId)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));

        boolean isSender = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.SENDER)
                .map(e -> e.toDomain().partyId().equals(requestingUserId))
                .orElse(false);
        boolean isRecipient = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.RECIPIENT)
                .map(e -> e.toDomain().partyId().equals(requestingUserId))
                .orElse(false);

        if (!isSender && !isRecipient) {
            throw new DomainException("Access denied: not a participant in this transfer");
        }
        return transfer;
    }

    // ── List Transfers ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Transfer> listBySender(UUID senderId) {
        return transferRepository.findBySenderIdAndRole(senderId, ParticipantRole.SENDER)
                .stream().map(TransferEntity::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<TransferSummary> listSummaries(UUID userId) {
        return transferRepository.findSummariesByParticipantId(userId)
                .stream().map(row -> {
                    UUID id = (UUID) row[0];
                    long amountTiyin = ((Number) row[1]).longValue();
                    long feeTiyin    = ((Number) row[2]).longValue();
                    String currencyCode = (String) row[3];
                    TransferStatus status  = TransferStatus.valueOf(((String) row[4]).toUpperCase());
                    TransferChannel channel = TransferChannel.valueOf(((String) row[5]).toUpperCase());
                    String idempotencyKey   = (String) row[6];
                    String initiatedAt      = (String) row[7];
                    String completedAt      = (String) row[8];
                    String senderName       = (String) row[9];
                    String senderMaskedPan  = (String) row[10];
                    String recipientName    = (String) row[11];
                    String recipientMaskedPan = (String) row[12];
                    String processedAt      = (String) row[13];
                    String direction        = (String) row[14];
                    int transferTypeId      = ((Number) row[15]).intValue();
                    return new TransferSummary(id,
                            new Money(amountTiyin, CurrencyCode.valueOf(currencyCode)),
                            new Money(feeTiyin, CurrencyCode.valueOf(currencyCode)),
                            status, channel, idempotencyKey, initiatedAt, completedAt,
                            senderName, senderMaskedPan, recipientName, recipientMaskedPan,
                            processedAt, direction, transferTypeId);
                }).toList();
    }

    // ── Helpers (TransferOrchestrationService / TransferParticipantService) ───

    private Instrument validateInstrumentOwnership(UUID instrumentId, UUID partyId) {
        return instrumentRepository
                .findByIdAndOwnerPartyId(instrumentId, partyId)
                .map(InstrumentEntity::toDomain)
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException(
                        "Instrument %s does not belong to party %s or is not usable"
                                .formatted(instrumentId, partyId)));
    }

    private static void validateChannel(TransferChannel channel, TransferType transferType) {
        if (channel == TransferChannel.WEB && "p2p".equals(transferType.code())) {
            throw new DomainException("P2P transfers via web channel are prohibited");
        }
    }

    private static void validatePartyTypes(String senderPartyType, String recipientPartyType,
                                           TransferType transferType) {
        if (transferType.allowedSenderPartyTypes() != null
                && !transferType.allowedSenderPartyTypes().contains(senderPartyType)) {
            throw new DomainException(
                    "Sender party type '%s' is not allowed for transfer type '%s'"
                            .formatted(senderPartyType, transferType.code()));
        }
        if (transferType.allowedRecipientPartyTypes() != null
                && !transferType.allowedRecipientPartyTypes().contains(recipientPartyType)) {
            throw new DomainException(
                    "Recipient party type '%s' is not allowed for transfer type '%s'"
                            .formatted(recipientPartyType, transferType.code()));
        }
    }
}
