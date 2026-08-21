package uz.pulsepay.merchant.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.infrastructure.security.JwtService;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.port.in.MerchantAuthPort;
import uz.pulsepay.merchant.domain.port.out.MerchantRepository;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

@Slf4j
@Service
public class MerchantAuthUseCase implements MerchantAuthPort {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public MerchantAuthUseCase(MerchantRepository merchantRepository,
                                PasswordEncoder passwordEncoder,
                                JwtService jwtService) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder    = passwordEncoder;
        this.jwtService         = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public String login(String email, String password) {
        log.info("Merchant login attempt: email={}", email);

        Merchant merchant = merchantRepository.findByEmail(email)
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
}
