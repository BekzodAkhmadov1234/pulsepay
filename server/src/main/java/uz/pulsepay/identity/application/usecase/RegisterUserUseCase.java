package uz.pulsepay.identity.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.domain.exception.DuplicatePhoneException;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.in.RegisterUserPort;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: self-registration of a new user account.
 *
 * <h3>Execution sequence (within a single {@code @Transactional} boundary)</h3>
 * <ol>
 *   <li>Reject duplicate phone numbers before any write occurs.</li>
 *   <li>Construct an immutable {@link User} record with safe defaults for a new account.</li>
 *   <li>Persist the record via the {@link UserRepository} outbound port.</li>
 *   <li>Generate and return a JWT access token so the client is immediately authenticated.</li>
 * </ol>
 *
 * <h3>Default field values for a new account</h3>
 * <ul>
 *   <li>{@code status} = {@code "active"} — new accounts are open by default.</li>
 *   <li>{@code kycLevel} = {@code "basic"} — full KYC verification is a separate flow.</li>
 *   <li>{@code biometricVerifiedAt} = {@code null} — not yet completed.</li>
 *   <li>{@code version} = {@code 0} — initial optimistic-lock version.</li>
 * </ul>
 */
@Slf4j
@Service
public class RegisterUserUseCase implements RegisterUserPort {

    private static final String DEFAULT_STATUS    = "active";
    private static final String DEFAULT_KYC_LEVEL = "basic";

    private final UserRepository userRepository;
    private final JwtService     jwtService;

    public RegisterUserUseCase(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService     = jwtService;
    }

    @Override
    @Transactional
    public String register(String phoneE164, String fullName) {
        log.info("Registration attempt: phone={}", phoneE164);

        // Guard against duplicate registrations before performing any write.
        // Using findByPhoneE164 rather than a separate existsBy call avoids an extra
        // query and keeps the check-then-insert within the same transaction.
        if (userRepository.findByPhoneE164(phoneE164).isPresent()) {
            log.warn("Registration rejected — phone already registered: {}", phoneE164);
            throw new DuplicatePhoneException(phoneE164);
        }

        Instant now = Instant.now();

        User newUser = new User(
                UUID.randomUUID(),  // id              — new random UUID
                phoneE164,          // phoneE164        — validated by the REST layer
                fullName,           // fullName
                DEFAULT_STATUS,     // status           — "active"
                DEFAULT_KYC_LEVEL,  // kycLevel         — "basic" until KYC flow completes
                null,               // biometricVerifiedAt — not yet verified
                now,                // createdAt
                now,                // updatedAt
                null,               // closedAt         — account is open
                0                   // version          — initial optimistic-lock counter
        );

        User saved = userRepository.save(newUser);
        log.info("User registered: id={}, phone={}", saved.id(), phoneE164);

        // Issue an access token immediately so the client does not need a separate login step.
        return jwtService.generateUserToken(saved);
    }
}
