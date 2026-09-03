package uz.pulsepay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.fee.FeePayer;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleEntity;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.domain.identity.UserEntity;
import uz.pulsepay.domain.party.Instrument;
import uz.pulsepay.domain.party.InstrumentEntity;
import uz.pulsepay.domain.party.InstrumentType;
import uz.pulsepay.domain.party.PartyType;
import uz.pulsepay.domain.paynet.PaynetProvider;
import uz.pulsepay.domain.paynet.PaynetProviderEntity;
import uz.pulsepay.domain.reference.TransferType;
import uz.pulsepay.domain.reference.TransferTypeEntity;
import uz.pulsepay.domain.routing.TransferRoute;
import uz.pulsepay.domain.shared.CurrencyCode;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.domain.transfer.P2STransferDetails;
import uz.pulsepay.domain.transfer.P2STransferDetailsEntity;
import uz.pulsepay.domain.transfer.ParticipantRole;
import uz.pulsepay.domain.transfer.Transfer;
import uz.pulsepay.domain.transfer.TransferChannel;
import uz.pulsepay.domain.transfer.TransferEntity;
import uz.pulsepay.domain.transfer.TransferParticipant;
import uz.pulsepay.domain.transfer.TransferParticipantEntity;
import uz.pulsepay.domain.transfer.TransferStateMachine;
import uz.pulsepay.domain.transfer.TransferStatus;
import uz.pulsepay.domain.transfer.TransferStatusHistory;
import uz.pulsepay.domain.transfer.TransferStatusHistoryEntity;
import uz.pulsepay.repository.FeeRuleRepository;
import uz.pulsepay.repository.InstrumentRepository;
import uz.pulsepay.repository.P2STransferDetailsRepository;
import uz.pulsepay.repository.PaynetProviderRepository;
import uz.pulsepay.repository.TransferParticipantRepository;
import uz.pulsepay.repository.TransferRepository;
import uz.pulsepay.repository.TransferStatusHistoryRepository;
import uz.pulsepay.repository.TransferTypeRepository;
import uz.pulsepay.repository.UserRepository;
import uz.pulsepay.utils.gateway.PaynetGateway;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * P2S (Person-to-Savings / Paynet utility) transfer service.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #initiate}   — validates, creates transfer in OTP_PENDING, generates OTP</li>
 *   <li>{@link #confirmOtp} — verifies OTP, debits sender card, calls Paynet, posts ledger</li>
 * </ol>
 *
 * <p>Participant roles:
 * <ul>
 *   <li>SENDER    — logged-in user's party + source card instrument (debited)</li>
 *   <li>RECIPIENT — virtual provider party + merchant_account instrument (Paynet provider)</li>
 * </ul>
 */
@Slf4j
@Service
public class P2STransferService {

    private static final int          P2S_TRANSFER_TYPE_ID = 7;
    private static final String       DEST_NETWORK         = "paynet";
    private static final ObjectMapper JSON_MAPPER          = new ObjectMapper();

    private final IdempotencyService            idempotencyService;
    private final UserRepository                userRepository;
    private final InstrumentRepository          instrumentRepository;
    private final TransferTypeRepository        transferTypeRepository;
    private final LimitService                  limitService;
    private final FeeService                    feeService;
    private final FeeRuleRepository             feeRuleRepository;
    private final RoutingService                routingService;
    private final PaynetProviderRepository      paynetProviderRepository;
    private final P2STransferDetailsRepository  p2sDetailsRepository;
    private final TransferRepository            transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final TransferOtpService            transferOtpService;
    private final NetworkTransactionService     networkTransactionService;
    private final PaynetGateway                 paynetGateway;
    private final LedgerService                 ledgerService;
    private final ComplianceService             complianceService;

    public P2STransferService(
            IdempotencyService idempotencyService,
            UserRepository userRepository,
            InstrumentRepository instrumentRepository,
            TransferTypeRepository transferTypeRepository,
            LimitService limitService,
            FeeService feeService,
            FeeRuleRepository feeRuleRepository,
            RoutingService routingService,
            PaynetProviderRepository paynetProviderRepository,
            P2STransferDetailsRepository p2sDetailsRepository,
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository,
            TransferOtpService transferOtpService,
            NetworkTransactionService networkTransactionService,
            PaynetGateway paynetGateway,
            LedgerService ledgerService,
            ComplianceService complianceService) {
        this.idempotencyService    = idempotencyService;
        this.userRepository        = userRepository;
        this.instrumentRepository  = instrumentRepository;
        this.transferTypeRepository = transferTypeRepository;
        this.limitService          = limitService;
        this.feeService            = feeService;
        this.feeRuleRepository     = feeRuleRepository;
        this.routingService        = routingService;
        this.paynetProviderRepository = paynetProviderRepository;
        this.p2sDetailsRepository  = p2sDetailsRepository;
        this.transferRepository    = transferRepository;
        this.participantRepository = participantRepository;
        this.historyRepository     = historyRepository;
        this.transferOtpService    = transferOtpService;
        this.networkTransactionService = networkTransactionService;
        this.paynetGateway         = paynetGateway;
        this.ledgerService         = ledgerService;
        this.complianceService     = complianceService;
    }

    // ── Initiate ──────────────────────────────────────────────────────────────

    @Transactional
    public Transfer initiate(UUID userId, UUID senderInstrumentId, String senderCardNetwork,
                             String serviceCode, Map<String, String> serviceFields,
                             Money amount, Integer purposeCodeId, TransferChannel channel,
                             String idempotencyKey) {

        log.info("Initiating P2S transfer: sender={}, service={}, amount={}, channel={}",
                userId, serviceCode, amount, channel);

        // 1. Idempotency guard
        idempotencyService.claimKey(idempotencyKey, userId, idempotencyKey);

        // 2. Validate sender active
        User sender = userRepository.findById(userId)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Sender not found or inactive"));

        // 3. Validate sender instrument ownership
        Instrument senderInstrument = instrumentRepository
                .findByIdAndOwnerPartyId(senderInstrumentId, userId)
                .map(InstrumentEntity::toDomain)
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException(
                        "Instrument %s does not belong to user %s or is not usable"
                                .formatted(senderInstrumentId, userId)));

        // 4. Assert transfer type P2S (id=7) is active
        transferTypeRepository.findById(P2S_TRANSFER_TYPE_ID)
                .map(TransferTypeEntity::toDomain)
                .filter(TransferType::isActive)
                .orElseThrow(() -> new DomainException("P2S transfer type is not available"));

        // 5. Limit check
        limitService.checkLimits(userId, sender.kycLevel(), amount, P2S_TRANSFER_TYPE_ID);

        // 6. Fee calculation (destination network = "paynet")
        Instant now = Instant.now();
        var feeResult = feeService.calculate(amount, P2S_TRANSFER_TYPE_ID,
                senderCardNetwork.toLowerCase(), DEST_NETWORK, amount.currency().name(), now);
        if (feeResult.isEmpty()) {
            log.warn("No P2S fee rule matched: srcNet={}, dstNet={}, amount={}",
                    senderCardNetwork, DEST_NETWORK, amount);
        }
        Money feeAmount        = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId  = feeResult.map(r -> r.appliedRule().id()).orElse(null);

        // 7. Route resolution
        TransferRoute route = routingService.resolve(senderCardNetwork.toLowerCase(),
                DEST_NETWORK, P2S_TRANSFER_TYPE_ID, amount);

        // 8. Validate Paynet provider
        PaynetProvider provider = paynetProviderRepository.findByServiceCode(serviceCode)
                .map(PaynetProviderEntity::toDomain)
                .filter(PaynetProvider::isActive)
                .orElseThrow(() -> new DomainException("Unknown or inactive Paynet service: " + serviceCode));

        // 9. Persist transfer
        Transfer transfer = transferRepository.save(TransferEntity.fromDomain(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.OTP_PENDING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), P2S_TRANSFER_TYPE_ID, purposeCodeId,
                channel, Instant.now(), null))).toDomain();

        // 10. Persist participants
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                userId, PartyType.PERSON, senderInstrumentId,
                senderInstrument.instrumentType(), Instant.now())));
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                provider.partyId(), PartyType.MERCHANT, provider.instrumentId(),
                InstrumentType.MERCHANT_ACCOUNT, Instant.now())));

        // 11. Persist P2S-specific details (serviceCode + serialised serviceFields)
        String serviceFieldsJson = serializeFields(serviceFields);
        p2sDetailsRepository.save(P2STransferDetailsEntity.fromDomain(new P2STransferDetails(
                UUID.randomUUID(), transfer.id(), serviceCode, serviceFieldsJson, Instant.now())));

        // 12. Status history
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.OTP_PENDING, "P2S utility payment initiated", Instant.now())));

        // 13. Generate OTP
        transferOtpService.generate(userId, transfer.id());

        log.info("P2S transfer created: id={}, status=OTP_PENDING, service={}, amount={}, fee={}",
                transfer.id(), serviceCode, amount, feeAmount);
        return transfer;
    }

    // ── Confirm OTP ───────────────────────────────────────────────────────────

    @Transactional
    public Transfer confirmOtp(UUID transferId, UUID userId, String otpCode) {
        log.info("P2S OTP confirmation: transferId={}, userId={}", transferId, userId);

        Transfer transfer = transferRepository.findById(transferId)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));

        TransferStateMachine.assertTransition(transfer.status(), TransferStatus.PROCESSING);

        // Verify OTP
        transferOtpService.verify(userId, otpCode, transferId);

        // Status → PROCESSING
        transferRepository.updateStatus(transferId, TransferStatus.PROCESSING, null);
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.OTP_PENDING, TransferStatus.PROCESSING, "OTP confirmed", Instant.now())));

        // Load participants
        TransferParticipant senderParticipant = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.SENDER)
                .map(TransferParticipantEntity::toDomain)
                .orElseThrow(() -> new DomainException("Sender participant not found"));
        TransferParticipant recipientParticipant = participantRepository
                .findByTransferIdAndRole(transferId, ParticipantRole.RECIPIENT)
                .map(TransferParticipantEntity::toDomain)
                .orElseThrow(() -> new DomainException("Recipient participant not found"));

        // Load P2S details (serviceCode + serviceFields)
        P2STransferDetails details = p2sDetailsRepository.findByTransferId(transferId)
                .map(P2STransferDetailsEntity::toDomain)
                .orElseThrow(() -> new DomainException("P2S details not found for transfer: " + transferId));

        // Resolve fee payer
        FeeRule appliedRule = transfer.appliedFeeRuleId() != null
                ? feeRuleRepository.findById(transfer.appliedFeeRuleId())
                        .map(FeeRuleEntity::toDomain).orElse(null)
                : null;
        FeePayer feePayer    = appliedRule != null ? appliedRule.feePayer() : FeePayer.SENDER;
        String feeRecipient  = appliedRule != null ? appliedRule.feeRecipient().name() : "PLATFORM";

        // SENDER pays fee: debit = principal + fee
        Money debitAmount  = feePayer == FeePayer.SENDER
                ? transfer.amount().add(transfer.feeAmount())
                : transfer.amount();

        // Resolve processor from route
        String processorName = transfer.appliedRouteId() != null
                ? routingService.findById(transfer.appliedRouteId())
                        .map(TransferRoute::processorName).orElse("stub_paynet")
                : "stub_paynet";

        // Debit sender card (via NetworkTransactionService → StubPaynetGateway.debitCard)
        networkTransactionService.execute(transferId,
                senderParticipant.instrumentId(), recipientParticipant.instrumentId(),
                debitAmount, transfer.amount(), processorName);

        // Call Paynet to credit the utility provider
        Map<String, String> serviceFieldMap = deserializeFields(details.serviceFields());
        PaynetGateway.PaynetResult paynetResult = paynetGateway.performTransaction(
                transferId.toString(), details.serviceCode(), Instant.now(),
                serviceFieldMap, transfer.amount());

        if (!paynetResult.success()) {
            transferRepository.updateStatus(transferId, TransferStatus.FAILED, Instant.now());
            historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                    UUID.randomUUID(), transferId,
                    TransferStatus.PROCESSING, TransferStatus.FAILED,
                    "Paynet rejected: " + paynetResult.statusMessage(), Instant.now())));
            throw new DomainException("Paynet payment failed: " + paynetResult.statusMessage());
        }

        // Post double-entry ledger (card_clearing → paynet_clearing)
        ledgerService.postTransferEntries(transferId, transfer.amount(), transfer.feeAmount(),
                senderParticipant.instrumentType().name().toLowerCase(),
                DEST_NETWORK,
                feeRecipient);

        // Increment limit counters
        limitService.increment(userId, transfer.amount(), P2S_TRANSFER_TYPE_ID);

        // Compliance evaluation
        complianceService.evaluate(transferId, userId, transfer.amount());

        // Status → COMPLETED
        transferRepository.updateStatus(transferId, TransferStatus.COMPLETED, Instant.now());
        transfer = transferRepository.findById(transferId)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new DomainException("Transfer disappeared after completion"));
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transferId,
                TransferStatus.PROCESSING, TransferStatus.COMPLETED,
                "P2S utility payment completed: " + paynetResult.paynetTransactionId(), Instant.now())));

        log.info("P2S transfer completed: id={}, paynetTxId={}", transferId, paynetResult.paynetTransactionId());
        return transfer;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String serializeFields(Map<String, String> fields) {
        try {
            return JSON_MAPPER.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new DomainException("Failed to serialize service fields: " + e.getMessage());
        }
    }

    private Map<String, String> deserializeFields(String json) {
        try {
            return JSON_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new DomainException("Failed to deserialize service fields: " + e.getMessage());
        }
    }
}
