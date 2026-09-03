package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.identity.UserEntity;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.domain.party.BankAccountDetailsEntity;
import uz.pulsepay.domain.party.InstrumentEntity;
import uz.pulsepay.domain.party.PartyEntity;
import uz.pulsepay.domain.party.BankAccountDetails;
import uz.pulsepay.domain.party.Instrument;
import uz.pulsepay.domain.party.InstrumentStatus;
import uz.pulsepay.domain.party.InstrumentType;
import uz.pulsepay.domain.party.Party;
import uz.pulsepay.domain.party.PartyType;
import uz.pulsepay.domain.reference.TransferTypeEntity;
import uz.pulsepay.domain.reference.TransferType;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleEntity;
import uz.pulsepay.domain.fee.FeePayer;
import uz.pulsepay.repository.BankAccountDetailsRepository;
import uz.pulsepay.repository.BankRepository;
import uz.pulsepay.repository.FeeRuleRepository;
import uz.pulsepay.repository.InstrumentRepository;
import uz.pulsepay.repository.PartyRepository;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * P2A (Person-to-Account / bank-rail) transfer service.
 *
 * Flow:
 *   {@link #initiate}    — validates, creates transfer in OTP_PENDING, generates OTP
 *   {@link #confirmOtp}  — verifies OTP, debits sender card, credits bank account, posts ledger
 *
 * Compensation: if bank credit fails, card debit is reversed by the gateway layer.
 * Participant roles:
 *   SENDER    — logged-in user's party + source card instrument (debited)
 *   RECIPIENT — virtual bank-account party + bank account instrument (credited)
 */
@Slf4j
@Service
public class BankTransferService {

    private static final int P2A_TRANSFER_TYPE_ID = 2;
    private static final String DEST_NETWORK = "bank";

    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final TransferTypeRepository transferTypeRepository;
    private final BankRepository bankRepository;
    private final LimitService limitService;
    private final FeeService feeService;
    private final FeeRuleRepository feeRuleRepository;
    private final RoutingService routingService;
    private final PartyRepository partyRepository;
    private final BankAccountDetailsRepository bankAccountDetailsRepository;
    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final TransferOtpService transferOtpService;
    private final NetworkTransactionService networkTransactionService;
    private final LedgerService ledgerService;
    private final ComplianceService complianceService;

    public BankTransferService(
            IdempotencyService idempotencyService,
            UserRepository userRepository,
            InstrumentRepository instrumentRepository,
            TransferTypeRepository transferTypeRepository,
            BankRepository bankRepository,
            LimitService limitService,
            FeeService feeService,
            FeeRuleRepository feeRuleRepository,
            RoutingService routingService,
            PartyRepository partyRepository,
            BankAccountDetailsRepository bankAccountDetailsRepository,
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository,
            TransferOtpService transferOtpService,
            NetworkTransactionService networkTransactionService,
            LedgerService ledgerService,
            ComplianceService complianceService) {
        this.idempotencyService           = idempotencyService;
        this.userRepository               = userRepository;
        this.instrumentRepository         = instrumentRepository;
        this.transferTypeRepository       = transferTypeRepository;
        this.bankRepository               = bankRepository;
        this.limitService                 = limitService;
        this.feeService                   = feeService;
        this.feeRuleRepository            = feeRuleRepository;
        this.routingService               = routingService;
        this.partyRepository              = partyRepository;
        this.bankAccountDetailsRepository = bankAccountDetailsRepository;
        this.transferRepository           = transferRepository;
        this.participantRepository        = participantRepository;
        this.historyRepository            = historyRepository;
        this.transferOtpService           = transferOtpService;
        this.networkTransactionService    = networkTransactionService;
        this.ledgerService                = ledgerService;
        this.complianceService            = complianceService;
    }

    @Transactional
    public Transfer initiate(UUID senderId, UUID senderInstrumentId, String senderCardNetwork,
                             String recipientIban, UUID recipientBankId, String recipientAccountHolderName,
                             Money amount, Integer purposeCodeId, TransferChannel channel,
                             String idempotencyKey) {

        log.info("Initiating P2A transfer: sender={}, iban={}, amount={}, channel={}",
                senderId, recipientIban, amount, channel);

        // 1. Idempotency guard
        idempotencyService.claimKey(idempotencyKey, senderId, idempotencyKey);

        // 2. Validate sender
        User sender = userRepository.findById(senderId)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Sender not found or inactive"));

        // 3. Validate sender instrument ownership
        Instrument senderInstrument = validateInstrumentOwnership(senderInstrumentId, senderId);

        // 4. Transfer type — P2A (id=2)
        TransferType transferType = transferTypeRepository.findById(P2A_TRANSFER_TYPE_ID)
                .map(TransferTypeEntity::toDomain)
                .filter(TransferType::isActive)
                .orElseThrow(() -> new DomainException("P2A transfer type is not available"));

        // 5. Channel invariant (P2A may not enforce same rule as P2P, but check is kept for safety)
        if (channel == TransferChannel.WEB && "p2a".equals(transferType.code())) {
            throw new DomainException("P2A transfers via web channel are prohibited");
        }

        // 6. Limit check
        limitService.checkLimits(senderId, sender.kycLevel(), amount, P2A_TRANSFER_TYPE_ID);

        // 7. Fee calculation (destination network = "bank")
        Instant now = Instant.now();
        var feeResult = feeService.calculate(amount, P2A_TRANSFER_TYPE_ID,
                senderCardNetwork, DEST_NETWORK, amount.currency().name(), now);
        if (feeResult.isEmpty()) {
            log.warn("No P2A fee rule matched: srcNet={}, dstNet={}, amount={}",
                    senderCardNetwork, DEST_NETWORK, amount);
        }
        Money feeAmount = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId = feeResult.map(r -> r.appliedRule().id()).orElse(null);

        // 8. Route resolution
        TransferRoute route = routingService.resolve(senderCardNetwork, DEST_NETWORK,
                P2A_TRANSFER_TYPE_ID, amount);

        // 9. Validate destination bank
        bankRepository.findById(recipientBankId)
                .map(e -> e.toDomain())
                .filter(b -> b.isActive())
                .orElseThrow(() -> new NotFoundException("Bank not found or inactive: " + recipientBankId));

        // 10. Find or create recipient bank account (party + instrument + bank_account_details)
        BankAccountDetails bankAccountDetails = findOrCreateBankAccount(
                recipientIban, recipientBankId, recipientAccountHolderName);
        UUID recipientPartyId = resolveOwnerPartyId(bankAccountDetails);

        // 11. Persist transfer
        Transfer transfer = transferRepository.save(TransferEntity.fromDomain(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.OTP_PENDING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), P2A_TRANSFER_TYPE_ID, purposeCodeId,
                channel, Instant.now(), null))).toDomain();

        // 12. Persist participants
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                senderId, PartyType.PERSON, senderInstrumentId,
                senderInstrument.instrumentType(), Instant.now())));
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                recipientPartyId, PartyType.PERSON, bankAccountDetails.instrumentId(),
                InstrumentType.BANK_ACCOUNT, Instant.now())));

        // 13. Status history
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.OTP_PENDING, "P2A transfer initiated", Instant.now())));

        // 14. Generate OTP
        transferOtpService.generate(senderId, transfer.id());

        log.info("P2A transfer created: id={}, status=OTP_PENDING, amount={}, fee={}",
                transfer.id(), amount, feeAmount);
        return transfer;
    }

    // ── Confirm OTP ───────────────────────────────────────────────────────────

    /**
     * Verifies the OTP and executes the P2A transfer:
     *   1. Debits the sender's card (UzCard/Humo via the route's gateway)
     *   2. Credits the destination bank account (via BankTransferGateway stub → real rail in prod)
     *
     *   — debit card first, credit IABS bank account; reverse card debit if bank credit fails.
     */
    @Transactional
    public Transfer confirmOtp(UUID transferId, UUID userId, String otpCode) {
        log.info("P2A OTP confirmation: transferId={}, userId={}", transferId, userId);

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

        // Resolve fee payer from applied rule
        FeeRule appliedRule = transfer.appliedFeeRuleId() != null
                ? feeRuleRepository.findById(transfer.appliedFeeRuleId())
                        .map(FeeRuleEntity::toDomain).orElse(null)
                : null;
        FeePayer feePayer = appliedRule != null ? appliedRule.feePayer() : FeePayer.SENDER;
        String feeRecipient = appliedRule != null ? appliedRule.feeRecipient().name() : "PLATFORM";

        // SENDER pays fee: debit = principal + fee; bank receives only principal
        Money debitAmount = feePayer == FeePayer.SENDER
                ? transfer.amount().add(transfer.feeAmount())
                : transfer.amount();
        Money creditAmount = transfer.amount();

        // Resolve gateway processor from route (falls back to "stub_bank")
        String processorName = transfer.appliedRouteId() != null
                ? routingService.findById(transfer.appliedRouteId())
                        .map(r -> r.processorName()).orElse("stub_bank")
                : "stub_bank";

        // Execute: debit sender card → credit bank account
        // If bank credit fails in production, the gateway layer must reverse the card debit.
        networkTransactionService.execute(transferId,
                senderParticipant.instrumentId(), recipientParticipant.instrumentId(),
                debitAmount, creditAmount, processorName);

        // Post double-entry ledger
        ledgerService.postTransferEntries(transferId, transfer.amount(), transfer.feeAmount(),
                senderParticipant.instrumentType().name().toLowerCase(),
                recipientParticipant.instrumentType().name().toLowerCase(),
                feeRecipient);

        // Increment limit counters
        limitService.increment(userId, transfer.amount(), P2A_TRANSFER_TYPE_ID);

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
                "P2A bank transfer completed", Instant.now())));

        log.info("P2A transfer completed: id={}, amount={}", transferId, transfer.amount());
        return transfer;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Instrument validateInstrumentOwnership(UUID instrumentId, UUID partyId) {
        return instrumentRepository
                .findByIdAndOwnerPartyId(instrumentId, partyId)
                .map(InstrumentEntity::toDomain)
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException(
                        "Instrument %s does not belong to party %s or is not usable"
                                .formatted(instrumentId, partyId)));
    }

    private BankAccountDetails findOrCreateBankAccount(String iban, UUID bankId,
                                                        String accountHolderName) {
        Optional<BankAccountDetailsEntity> existing = bankAccountDetailsRepository.findByIban(iban);
        if (existing.isPresent()) {
            log.debug("Reusing existing bank account for IBAN={}", iban);
            return existing.get().toDomain();
        }

        Party party = partyRepository.save(
                PartyEntity.fromDomain(new Party(UUID.randomUUID(), PartyType.PERSON, Instant.now())))
                .toDomain();
        Instrument instrument = instrumentRepository.save(
                InstrumentEntity.fromDomain(new Instrument(
                        UUID.randomUUID(), party.id(), InstrumentType.BANK_ACCOUNT,
                        InstrumentStatus.ACTIVE, Instant.now(), null)))
                .toDomain();
        BankAccountDetails details = bankAccountDetailsRepository.save(
                BankAccountDetailsEntity.fromDomain(new BankAccountDetails(
                        UUID.randomUUID(), instrument.id(), bankId, iban, null,
                        accountHolderName, Instant.now())))
                .toDomain();

        log.debug("Created bank account: party={}, instrument={}, iban={}",
                party.id(), instrument.id(), iban);
        return details;
    }

    private UUID resolveOwnerPartyId(BankAccountDetails bankAccountDetails) {
        return instrumentRepository.findById(bankAccountDetails.instrumentId())
                .map(e -> e.toDomain().ownerPartyId())
                .orElseThrow(() -> new DomainException("Bank account instrument not found"));
    }
}
