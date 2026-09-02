package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.identity.UserEntity;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.domain.merchant.MerchantAccountEntity;
import uz.pulsepay.domain.merchant.MerchantEntity;
import uz.pulsepay.domain.merchant.OnboardMerchantCommand;
import uz.pulsepay.domain.merchant.KybStatus;
import uz.pulsepay.domain.merchant.Merchant;
import uz.pulsepay.domain.merchant.MerchantAccount;
import uz.pulsepay.domain.merchant.MerchantAccountStatus;
import uz.pulsepay.domain.merchant.MerchantCategory;
import uz.pulsepay.domain.merchant.MerchantStatus;
import uz.pulsepay.domain.merchant.SettlementSchedule;
import uz.pulsepay.domain.party.InstrumentEntity;
import uz.pulsepay.domain.party.Instrument;
import uz.pulsepay.domain.party.InstrumentType;
import uz.pulsepay.domain.party.PartyType;
import uz.pulsepay.repository.InstrumentRepository;
import uz.pulsepay.repository.MerchantAccountRepository;
import uz.pulsepay.repository.MerchantCategoryRepository;
import uz.pulsepay.repository.MerchantRepository;
import uz.pulsepay.repository.TransferParticipantRepository;
import uz.pulsepay.repository.TransferRepository;
import uz.pulsepay.repository.TransferStatusHistoryRepository;
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
import uz.pulsepay.domain.transfer.TransferStatus;
import uz.pulsepay.domain.transfer.TransferStatusHistory;
import uz.pulsepay.domain.transfer.TransferSummary;
import uz.pulsepay.utils.security.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Merchant lifecycle, authentication, self-service, and virtual terminal.
 *
 * Merges: ManageMerchantUseCase, MerchantAuthUseCase, MerchantSelfUseCase, VirtualTerminalUseCase.
 */
@Slf4j
@Service
public class MerchantService {

    private static final int C2B_TRANSFER_TYPE_ID = 3;

    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final MerchantCategoryRepository merchantCategoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final FeeService feeService;
    private final RoutingService routingService;
    private final TransferRepository transferRepository;
    private final TransferParticipantRepository participantRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final TransferService transferService;

    public MerchantService(
            MerchantRepository merchantRepository,
            MerchantAccountRepository merchantAccountRepository,
            MerchantCategoryRepository merchantCategoryRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserRepository userRepository,
            InstrumentRepository instrumentRepository,
            FeeService feeService,
            RoutingService routingService,
            TransferRepository transferRepository,
            TransferParticipantRepository participantRepository,
            TransferStatusHistoryRepository historyRepository,
            TransferService transferService) {
        this.merchantRepository         = merchantRepository;
        this.merchantAccountRepository  = merchantAccountRepository;
        this.merchantCategoryRepository = merchantCategoryRepository;
        this.passwordEncoder            = passwordEncoder;
        this.jwtService                 = jwtService;
        this.userRepository             = userRepository;
        this.instrumentRepository       = instrumentRepository;
        this.feeService                 = feeService;
        this.routingService             = routingService;
        this.transferRepository         = transferRepository;
        this.participantRepository      = participantRepository;
        this.historyRepository          = historyRepository;
        this.transferService            = transferService;
    }

    // ── Admin: Manage Merchant ────────────────────────────────────────────────

    @Transactional
    public Merchant onboard(OnboardMerchantCommand cmd) {
        if (merchantRepository.findByEmail(cmd.email()).isPresent()) {
            throw new DomainException("Email already registered: " + cmd.email());
        }

        MerchantCategory category = merchantCategoryRepository.findByMccCode(cmd.mccCode())
                .map(e -> e.toDomain())
                .orElseThrow(() -> new NotFoundException("MCC code not found: " + cmd.mccCode()));

        UUID merchantId = UUID.randomUUID();
        UUID accountId  = UUID.randomUUID();
        Instant now     = Instant.now();
        String hash     = passwordEncoder.encode(cmd.passwordRaw());

        // Insert party row first (merchants.id FK → parties.id)
        merchantRepository.upsertMerchantParty(merchantId);
        Merchant merchant = merchantRepository.save(MerchantEntity.fromDomain(new Merchant(
                merchantId, cmd.legalTradeName(), category.id(), cmd.acquiringBankId(),
                KybStatus.PENDING, MerchantStatus.PENDING, false,
                cmd.email(), hash, now))).toDomain();

        // Create instrument row (instruments.id = accountId) then merchant account
        merchantRepository.upsertMerchantAccountInstrument(accountId, merchantId);
        merchantAccountRepository.save(MerchantAccountEntity.fromDomain(new MerchantAccount(
                accountId, merchantId, "UZS", 0L, SettlementSchedule.DAILY,
                MerchantAccountStatus.ACTIVE, now)));

        log.info("Merchant onboarded: id={}, email={}", merchantId, cmd.email());
        return merchant;
    }

    @Transactional
    public Merchant approve(UUID id) {
        Merchant merchant = getById(id);
        Merchant updated = new Merchant(
                merchant.id(), merchant.legalTradeName(), merchant.categoryId(), merchant.acquiringBankId(),
                KybStatus.VERIFIED, MerchantStatus.ACTIVE, merchant.uzqrEnabled(),
                merchant.email(), merchant.passwordHash(), merchant.createdAt());
        Merchant saved = merchantRepository.save(MerchantEntity.fromDomain(updated)).toDomain();
        log.info("Merchant approved: id={}", id);
        return saved;
    }

    @Transactional
    public Merchant reject(UUID id, String reason) {
        Merchant merchant = getById(id);
        Merchant updated = new Merchant(
                merchant.id(), merchant.legalTradeName(), merchant.categoryId(), merchant.acquiringBankId(),
                KybStatus.REJECTED, MerchantStatus.PENDING, merchant.uzqrEnabled(),
                merchant.email(), merchant.passwordHash(), merchant.createdAt());
        Merchant saved = merchantRepository.save(MerchantEntity.fromDomain(updated)).toDomain();
        log.info("Merchant rejected: id={}, reason={}", id, reason);
        return saved;
    }

    @Transactional
    public Merchant suspend(UUID id, String reason) {
        Merchant merchant = getById(id);
        Merchant updated = new Merchant(
                merchant.id(), merchant.legalTradeName(), merchant.categoryId(), merchant.acquiringBankId(),
                merchant.kybStatus(), MerchantStatus.SUSPENDED, merchant.uzqrEnabled(),
                merchant.email(), merchant.passwordHash(), merchant.createdAt());
        Merchant saved = merchantRepository.save(MerchantEntity.fromDomain(updated)).toDomain();
        log.info("Merchant suspended: id={}, reason={}", id, reason);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Merchant> listAll() {
        return merchantRepository.findAll().stream()
                .map(MerchantEntity::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<MerchantCategory> listCategories() {
        return merchantCategoryRepository.findAll().stream()
                .map(e -> e.toDomain()).toList();
    }

    @Transactional(readOnly = true)
    public Merchant getById(UUID id) {
        return merchantRepository.findById(id)
                .map(MerchantEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));
    }

    // ── Merchant Auth ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        log.info("Merchant login attempt: email={}", email);

        Merchant merchant = merchantRepository.findByEmail(email)
                .map(MerchantEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("No merchant account found for email: " + email));

        if (!merchant.isActive()) {
            log.warn("Merchant login rejected — not active: merchantId={}", merchant.id());
            throw new DomainException("Merchant account is not active. KYB must be verified.");
        }

        if (merchant.passwordHash() == null ||
                !passwordEncoder.matches(password, merchant.passwordHash())) {
            log.warn("Merchant login failed — wrong password: merchantId={}", merchant.id());
            throw new DomainException("Invalid credentials");
        }

        log.info("Merchant login successful: merchantId={}", merchant.id());
        return jwtService.generateMerchantToken(merchant.id(), merchant.email());
    }

    // ── Merchant Self-Service ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Merchant getProfile(UUID merchantId) {
        return getById(merchantId);
    }

    @Transactional(readOnly = true)
    public MerchantAccount getMyAccount(UUID merchantId) {
        return merchantAccountRepository.findByMerchantId(merchantId)
                .map(MerchantAccountEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Merchant account not found for: " + merchantId));
    }

    @Transactional(readOnly = true)
    public List<TransferSummary> getTransfers(UUID merchantId) {
        return transferService.listSummaries(merchantId);
    }

    // ── Virtual Terminal (C2B) ────────────────────────────────────────────────

    @Transactional
    public Transfer charge(UUID merchantId, String customerPhone, UUID customerInstrumentId,
                           String cardNetwork, long amountTiyin, Integer purposeCodeId) {
        // 1. Validate merchant
        Merchant merchant = merchantRepository.findById(merchantId)
                .map(MerchantEntity::toDomain)
                .filter(Merchant::isActive)
                .orElseThrow(() -> new DomainException("Merchant is not active or KYB not verified"));

        MerchantAccount account = merchantAccountRepository.findByMerchantId(merchantId)
                .map(MerchantAccountEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Merchant account not found"));

        // 2. Validate customer
        User customer = userRepository.findByPhoneE164(customerPhone)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Customer not found or inactive: " + customerPhone));

        // 3. Validate customer instrument ownership
        Instrument instrument = instrumentRepository
                .findByIdAndOwnerPartyId(customerInstrumentId, customer.id())
                .map(InstrumentEntity::toDomain)
                .filter(Instrument::isUsable)
                .orElseThrow(() -> new DomainException("Customer instrument not found or not usable"));

        // 4. Build money
        Money amount = Money.ofTiyin(amountTiyin, CurrencyCode.UZS);

        // 5. Fee calculation
        Instant now = Instant.now();
        var feeResult = feeService.calculate(amount, C2B_TRANSFER_TYPE_ID,
                cardNetwork, "merchant", amount.currency().name(), now);
        Money feeAmount = feeResult.map(r -> r.fee()).orElse(Money.ofTiyin(0, CurrencyCode.UZS));
        UUID appliedFeeRuleId = feeResult.map(r -> r.appliedRule().id()).orElse(null);

        // 6. Route resolution
        TransferRoute route = routingService.resolve(cardNetwork, "merchant",
                C2B_TRANSFER_TYPE_ID, amount);

        // 7. Create transfer (skip OTP_PENDING — virtual terminal bypasses OTP)
        String idempotencyKey = UUID.randomUUID().toString();
        Transfer transfer = transferRepository.save(TransferEntity.fromDomain(new Transfer(
                UUID.randomUUID(), amount, feeAmount,
                TransferStatus.PROCESSING, idempotencyKey, null,
                appliedFeeRuleId, route.id(), C2B_TRANSFER_TYPE_ID, purposeCodeId,
                TransferChannel.API, now, null))).toDomain();

        // 8. Participants
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.SENDER,
                customer.id(), PartyType.PERSON, customerInstrumentId,
                instrument.instrumentType(), now)));
        participantRepository.save(TransferParticipantEntity.fromDomain(new TransferParticipant(
                UUID.randomUUID(), transfer.id(), ParticipantRole.RECIPIENT,
                merchantId, PartyType.MERCHANT, account.id(),
                InstrumentType.MERCHANT_ACCOUNT, now)));

        // 9. Status history: PROCESSING entry
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), transfer.id(), null,
                TransferStatus.PROCESSING, "C2B virtual terminal charge initiated", now)));

        // 10. Auto-complete (stub phase — no real gateway call)
        transferRepository.updateStatus(transfer.id(), TransferStatus.COMPLETED, Instant.now());
        Transfer completed = transferRepository.findById(transfer.id())
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new DomainException("Transfer disappeared after completion"));
        historyRepository.save(TransferStatusHistoryEntity.fromDomain(new TransferStatusHistory(
                UUID.randomUUID(), completed.id(), TransferStatus.PROCESSING,
                TransferStatus.COMPLETED, "Auto-completed (virtual terminal stub)", Instant.now())));

        log.info("C2B charge completed: transferId={}, merchant={}, customer={}, amount={}",
                completed.id(), merchantId, customer.id(), amount);
        return completed;
    }
}
