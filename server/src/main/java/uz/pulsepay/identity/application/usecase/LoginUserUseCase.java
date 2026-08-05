package uz.pulsepay.identity.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.domain.exception.AccountInactiveException;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.in.LoginUserPort;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.infrastructure.security.JwtService;
import uz.pulsepay.shared.exception.NotFoundException;

/**
 * Use case: phone-based login for an existing user account.
 *
 * <h3>Execution sequence</h3>
 * <ol>
 *   <li>Look up the user by E.164 phone number; throw {@link NotFoundException} if absent.</li>
 *   <li>Reject the attempt if the account is not active.</li>
 *   <li>Generate and return a JWT access token embedding {@code phone_e164} and
 *       {@code kyc_level} claims.</li>
 * </ol>
 *
 * <p><strong>Temporary:</strong> OTP verification is intentionally skipped in this phase.
 * Step 3 will be re-inserted (delegating to {@code OtpDomainService}) once the SMS gateway
 * is integrated and the {@link LoginUserPort} signature is extended to accept the OTP code.
 */
@Slf4j
@Service
public class LoginUserUseCase implements LoginUserPort {

    private final UserRepository userRepository;
    private final JwtService     jwtService;

    public LoginUserUseCase(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService     = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public String login(String phoneE164) {
        log.info("Login attempt: phone={}", phoneE164);

        User user = userRepository.findByPhoneE164(phoneE164)
                .orElseThrow(() -> new NotFoundException(
                        "No account registered for phone: " + phoneE164));

        if (!user.isActive()) {
            log.warn("Login rejected — account inactive: userId={}, phone={}", user.id(), phoneE164);
            throw new AccountInactiveException(phoneE164);
        }

        log.info("Login successful: userId={}, phone={}", user.id(), phoneE164);
        return jwtService.generateUserToken(user);
    }
}
