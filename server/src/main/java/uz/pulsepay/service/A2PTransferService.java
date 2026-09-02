package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleEntity;
import uz.pulsepay.domain.fee.FeePayer;
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
import uz.pulsepay.domain.routing.TransferRoute;
import uz.pulsepay.domain.shared.CurrencyCode;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.Money;
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
import uz.pulsepay.utils.gateway.BankAccountPullGateway;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A2P (Account-to-Person / bank pull) transfer service.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #initiate} — validates, creates transfer in OTP_PENDING, generates OTP</li>
 *   <li>{@link #confirmOtp} — verifies OTP, calls bank pull gateway, credits card, posts ledger</li>
 * </ol>
 *
 * <p>Participant roles:
 * <ul>
 *   <li>SENDER  — virtual bank-account party + bank account instrument (source of pulled funds)</li>
 *   <li>RECIPIENT — the logged-in user's party + destination card instrument</li>
 * </ul>
 * This makes the transfer appear as direction='credit' in the user's transfer history.
 */
@Slf4j
@Service
public class A2PTransferService {

    private static final int A2P_TRANSFER_TYPE_ID = 6; // 6th transfer type inserted (a2p)
    private static final String SOURCE_NETWORK = "bank";

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
    private final BankAccountPullGateway bankAccountPullGateway;
    private final CardBalanceService cardBalanceService;
    private final LedgerService ledgerService;
    private final ComplianceService complianceService;

    public A2PTransferService(
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
            BankAccountPullGateway bankAccountPullGateway,
            CardBalanceService cardBalanceService,
            LedgerService ledgerService,
            ComplianceService complianceService) {
        this.idempotencyService            = idempotencyService;
        this.userRepository                = userRepository;
        this.instrumentRepository          = instrumentRepository;
        this.transferTypeRepository        = transferTypeRepository;
        this.bankRepository                = bankRepository;
        this.limitService                  = limitService;
        this.feeService                    = feeService;
        this.feeRuleRepository             = feeRuleRepository;
        this.routingService                = routingService;
        this.partyRepository               = partyRepository;
        this.bankAccountDetailsRepository  = bankAccountDetailsRepository;
        this.transferRepository            = transferRepository;
        this.participantRepository         = participantRepository;
        this.historyRepository             = historyRepository;
        this.transferOtpService            = transferOtpService;
        this.bankAccountPullGateway        = bankAccountPullGateway;
        this.cardBalanceService            = cardBalanceService;
        this.ledgerService                 = ledgerService;
        this.complianceService             = complianceService;
    }

    // ── Initiate ──────────────────────────────────────────────────────────────

    @Transactional
    public Transfer initiate(UUID userId, String sourceIban, UUID sourceBankId,
                             String sourceAccountHolderName, UUID destInstrumentId,
                             String destCardNetwork, Money amount,
                             Integer purposeCodeId, TransferChannel channel,
                             String idempotencyKey) {

        log.info("Initiating A2P transfer: user={}, iban={}, destInstrument={}, amount={}",
                userId, sourceIban, destInstrumentId, amount);

        // 1. Idempotency guard
        idempotencyService.claimKey(idempotencyKey, userId, idempotencyKey);

        // 2. Validate user
        User user = userRepository.findById(userId)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("User not found or inactive"));

        // 3. Validate destination card ownership
        Instrument destInstrument = instrumentRepository
                .findByIdAndOwnerPartyId(destInstrumentId, userId)
                .map(InstrumentEntity::toDomain)
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException(
                        "Destination instrument %s does not belong to user %s or is not usable"
                                .formatted(destInstrumentId, userId)));

        // 4. Transfer type 4 (a2p)
        TransferType transferType = transferTypeRepository.findById(A2P_TRANSFER_TYPE_ID)
                .map(TransferTypeEntity::toDomain)
                .filter(TransferType::isActive)
                .orElseThrow(() -> new DomainException("A2P transfer type is not available"));

        // 5. Limit check
        limitService.checkLimits(userId, user.kycLevel(), amount, A2P_TRANSFER_TYPE_ID);

        // 6. Fee calculation (source=bank, dest=uzcard/humo)
        Instant now = Instant.now();
        var feeResult = feeService.calculate(amount, A2P_TRANSFER_TYPE_ID,
                SOURCE_NETWORK, destCardNetwork.toLowerCase(), amount.currency().name(), now);
        if (feeResult.isEmpty()) {
            log.warn("No A2P fee rule matched: srcNet={}, dstNet={}", SOURCE_NETWORK, destCardNetwork);
        }
        Money feeAmount = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId = feeResult.map(r -> r.appliedRule().id()).orElse(null);

        // 7. Route resolution
        TransferRoute route = routingService.resolve(SOURCE_NETWORK, destCardNetwork.toLowerCase(),
                A2P_TRANSFER_TYPE_ID, amount);

        // 8. Validate source bank
        bankRepository.findById(sourceBankId)
                .map(e -> e.toDomain())
                .filter(b -> b.isActive())
                .orElseThrow(() -> new NotFoundException("Bank not found or inactive: " + sourceBankId));

        // 9. Find or create source bank account instrument (virtual party)
        BankAccountDetails bankAccount = findOrCreateBankAccount(
                sourceIban, sourceBankId, sourceAccountHolderName);
        UUID bankAccountPartyId = resolveOwnerPartyId(bankAccount);

        // 10. Persist transfer
        Transfer transfer = transferRepository.save(TransferEntity.fromDomain(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.OTP_PENDING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), A2P_TRANSFER_TYPE_ID, purposeCodeId,
                channel, Instant.now(), null))).toDomain();

        // 11. Persist participants
        // SENDER = bank account party (virtual) + bank account instrument
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                bankAccountPartyId, PartyType.PERSON, bankAccount.instrumentId(),
                InstrumentType.BANK_ACCOUNT, Instant.now())));
        // RECIPIENT = logged-in user + destination card
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                userId, PartyType.PERSON, destInstrumentId,
                destInstrument.instrumentType(), Instant.now())));

        // 12. Status history
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.OTP_PENDING, "A2P transfer initiated", Instant.now())));

        // 13. Generate OTP
        transferOtpService.generate(userId, transfer.id());

        log.info("A2P transfer created: id={}, status=OTP_PENDING, amount={}", transfer.id(), amount);
        return transfer;
    }

    // ── Confirm OTP ───────────────────────────────────────────────────────────

    @Transactional
    public Transfer confirmOtp(UUID transferId, UUID userId, String otpCode) {
        log.info("A2P OTP confirmation: transferId={}, userId={}", transferId, userId);

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

        // Load source bank account details (IBAN, bankId, holder name)
        BankAccountDetails bankAccount = bankAccountDetailsRepository
                .findByInstrumentId(senderParticipant.instrumentId())
                .map(BankAccountDetailsEntity::toDomain)
                .orElseThrow(() -> new DomainException("Bank account details not found for instrument: "
                        + senderParticipant.instrumentId()));

        // Resolve fee payer
        FeeRule appliedRule = transfer.appliedFeeRuleId() != null
                ? feeRuleRepository.findById(transfer.appliedFeeRuleId())
                        .map(FeeRuleEntity::toDomain).orElse(null)
                : null;
        FeePayer feePayer = appliedRule != null ? appliedRule.feePayer() : FeePayer.SENDER;
        String feeRecipient = appliedRule != null ? appliedRule.feeRecipient().name() : "PLATFORM";

        // For A2P, fee is deducted from the pulled amount:
        // debitAmount = principal + fee (pulled from bank)
        // creditAmount = principal (credited to card)
        Money debitAmount = feePayer == FeePayer.SENDER
                ? transfer.amount().add(transfer.feeAmount())
                : transfer.amount();
        Money creditAmount = transfer.amount();

        String pullRef = transferId.toString() + "-a2p";

        // Execute bank pull (debit bank account)
        BankAccountPullGateway.PullResult pullResult = bankAccountPullGateway.initiateDebit(
                bankAccount.iban(), bankAccount.bankId(),
                bankAccount.accountHolderName(), debitAmount, pullRef);

        if (!pullResult.success()) {
            transferRepository.updateStatus(transferId, TransferStatus.FAILED, Instant.now());
            historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                    UUID.randomUUID(), transferId,
                    TransferStatus.PROCESSING, TransferStatus.FAILED,
                    "Bank pull rejected: " + pullResult.statusMessage(), Instant.now())));
            throw new DomainException("Bank pull failed: " + pullResult.statusMessage());
        }

        // Credit destination card shadow balance
        cardBalanceService.credit(recipientParticipant.instrumentId(), creditAmount.amount());

        // Post ledger entries (bank_account → bank_clearing, card → card_clearing)
        ledgerService.postTransferEntries(transferId, transfer.amount(), transfer.feeAmount(),
                senderParticipant.instrumentType().name().toLowerCase(),
                recipientParticipant.instrumentType().name().toLowerCase(),
                feeRecipient);

        // Increment limit usage
        limitService.increment(userId, transfer.amount(), A2P_TRANSFER_TYPE_ID);

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
                "Bank pull successful: " + pullResult.gatewayTransactionId(), Instant.now())));

        log.info("A2P transfer completed: id={}, bankTxId={}", transferId, pullResult.gatewayTransactionId());
        return transfer;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BankAccountDetails findOrCreateBankAccount(String iban, UUID bankId,
                                                        String accountHolderName) {
        Optional<BankAccountDetailsEntity> existing = bankAccountDetailsRepository.findByIban(iban);
        if (existing.isPresent()) {
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
        return bankAccountDetailsRepository.save(
                BankAccountDetailsEntity.fromDomain(new BankAccountDetails(
                        UUID.randomUUID(), instrument.id(), bankId, iban, null,
                        accountHolderName, Instant.now())))
                .toDomain();
    }

    private UUID resolveOwnerPartyId(BankAccountDetails bankAccountDetails) {
        return instrumentRepository.findById(bankAccountDetails.instrumentId())
                .map(e -> e.toDomain().ownerPartyId())
                .orElseThrow(() -> new DomainException("Bank account instrument not found"));
    }

}
