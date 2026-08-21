package uz.pulsepay.merchant.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.merchant.adapter.out.jpa.MerchantJpaAdapter;
import uz.pulsepay.merchant.domain.command.OnboardMerchantCommand;
import uz.pulsepay.merchant.domain.model.KybStatus;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantAccount;
import uz.pulsepay.merchant.domain.model.MerchantAccountStatus;
import uz.pulsepay.merchant.domain.model.MerchantCategory;
import uz.pulsepay.merchant.domain.model.MerchantStatus;
import uz.pulsepay.merchant.domain.model.SettlementSchedule;
import uz.pulsepay.merchant.domain.port.in.ManageMerchantPort;
import uz.pulsepay.merchant.domain.port.out.MerchantAccountRepository;
import uz.pulsepay.merchant.domain.port.out.MerchantCategoryRepository;
import uz.pulsepay.merchant.domain.port.out.MerchantRepository;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ManageMerchantUseCase implements ManageMerchantPort {

    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final MerchantCategoryRepository merchantCategoryRepository;
    private final MerchantJpaAdapter merchantJpaAdapter;
    private final PasswordEncoder passwordEncoder;

    public ManageMerchantUseCase(MerchantRepository merchantRepository,
                                  MerchantAccountRepository merchantAccountRepository,
                                  MerchantCategoryRepository merchantCategoryRepository,
                                  MerchantJpaAdapter merchantJpaAdapter,
                                  PasswordEncoder passwordEncoder) {
        this.merchantRepository         = merchantRepository;
        this.merchantAccountRepository  = merchantAccountRepository;
        this.merchantCategoryRepository = merchantCategoryRepository;
        this.merchantJpaAdapter         = merchantJpaAdapter;
        this.passwordEncoder            = passwordEncoder;
    }

    @Override
    @Transactional
    public Merchant onboard(OnboardMerchantCommand cmd) {
        if (merchantRepository.findByEmail(cmd.email()).isPresent()) {
            throw new DomainException("Email already registered: " + cmd.email());
        }

        MerchantCategory category = merchantCategoryRepository.findByMccCode(cmd.mccCode())
                .orElseThrow(() -> new NotFoundException("MCC code not found: " + cmd.mccCode()));

        UUID merchantId = UUID.randomUUID();
        UUID accountId  = UUID.randomUUID();
        Instant now     = Instant.now();
        String hash     = passwordEncoder.encode(cmd.passwordRaw());

        Merchant merchant = merchantRepository.save(new Merchant(
                merchantId, cmd.legalTradeName(), category.id(), cmd.acquiringBankId(),
                KybStatus.PENDING, MerchantStatus.PENDING, false,
                cmd.email(), hash, now));

        // Create instrument row (instruments.id = accountId) then merchant account
        merchantJpaAdapter.upsertMerchantAccountInstrument(accountId, merchantId);
        merchantAccountRepository.save(new MerchantAccount(
                accountId, merchantId, "UZS", 0L, SettlementSchedule.DAILY,
                MerchantAccountStatus.ACTIVE, now));

        log.info("Merchant onboarded: id={}, email={}", merchantId, cmd.email());
        return merchant;
    }

    @Override
    @Transactional
    public Merchant approve(UUID id) {
        Merchant merchant = getById(id);
        Merchant updated = new Merchant(
                merchant.id(), merchant.legalTradeName(), merchant.categoryId(), merchant.acquiringBankId(),
                KybStatus.VERIFIED, MerchantStatus.ACTIVE, merchant.uzqrEnabled(),
                merchant.email(), merchant.passwordHash(), merchant.createdAt());
        Merchant saved = merchantRepository.save(updated);
        log.info("Merchant approved: id={}", id);
        return saved;
    }

    @Override
    @Transactional
    public Merchant reject(UUID id, String reason) {
        Merchant merchant = getById(id);
        Merchant updated = new Merchant(
                merchant.id(), merchant.legalTradeName(), merchant.categoryId(), merchant.acquiringBankId(),
                KybStatus.REJECTED, MerchantStatus.PENDING, merchant.uzqrEnabled(),
                merchant.email(), merchant.passwordHash(), merchant.createdAt());
        Merchant saved = merchantRepository.save(updated);
        log.info("Merchant rejected: id={}, reason={}", id, reason);
        return saved;
    }

    @Override
    @Transactional
    public Merchant suspend(UUID id, String reason) {
        Merchant merchant = getById(id);
        Merchant updated = new Merchant(
                merchant.id(), merchant.legalTradeName(), merchant.categoryId(), merchant.acquiringBankId(),
                merchant.kybStatus(), MerchantStatus.SUSPENDED, merchant.uzqrEnabled(),
                merchant.email(), merchant.passwordHash(), merchant.createdAt());
        Merchant saved = merchantRepository.save(updated);
        log.info("Merchant suspended: id={}, reason={}", id, reason);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> listAll() {
        return merchantRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Merchant getById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));
    }
}
