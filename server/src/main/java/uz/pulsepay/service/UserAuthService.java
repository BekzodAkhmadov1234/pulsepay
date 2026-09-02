package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.identity.UserEntity;
import uz.pulsepay.domain.identity.AccountInactiveException;
import uz.pulsepay.domain.identity.DuplicatePhoneException;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.repository.UserRepository;
import uz.pulsepay.service.SessionService.SessionOpenResult;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.utils.security.JwtService;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class UserAuthService {

    private static final String DEFAULT_STATUS    = "active";
    private static final String DEFAULT_KYC_LEVEL = "basic";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final SessionService sessionService;
    private final JwtService jwtService;

    public UserAuthService(UserRepository userRepository,
                           OtpService otpService,
                           SessionService sessionService,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.otpService     = otpService;
        this.sessionService = sessionService;
        this.jwtService     = jwtService;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String login(String phoneE164) {
        log.info("Login attempt: phone={}", phoneE164);

        User user = userRepository.findByPhoneE164(phoneE164)
                .map(UserEntity::toDomain)
                .orElseThrow(() -> new NotFoundException(
                        "No account registered for phone: " + phoneE164));

        if (!user.isActive()) {
            log.warn("Login rejected — account inactive: userId={}", user.id());
            throw new AccountInactiveException(phoneE164);
        }

        log.info("Login successful: userId={}", user.id());
        return jwtService.generateUserToken(user);
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public String register(String phoneE164, String fullName) {
        log.info("Registration attempt: phone={}", phoneE164);

        if (userRepository.findByPhoneE164(phoneE164).isPresent()) {
            log.warn("Registration rejected — phone already registered: {}", phoneE164);
            throw new DuplicatePhoneException(phoneE164);
        }

        Instant now = Instant.now();
        User newUser = new User(UUID.randomUUID(), phoneE164, fullName,
                DEFAULT_STATUS, DEFAULT_KYC_LEVEL, null, now, now, null, 0);

        // Insert the party row first (users.id FK → parties.id)
        userRepository.upsertParty(newUser.id());
        User saved = userRepository.save(UserEntity.fromDomain(newUser)).toDomain();

        log.info("User registered: id={}, phone={}", saved.id(), phoneE164);
        return jwtService.generateUserToken(saved);
    }

    // ── Request OTP ───────────────────────────────────────────────────────────

    public void requestOtp(UUID userId, OtpPurpose purpose, UUID targetId) {
        log.info("OTP requested: userId={}, purpose={}", userId, purpose);
        userRepository.findById(userId)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("User not found or inactive"));
        String rawCode = otpService.generateAndSave(userId, purpose, targetId);
        log.debug("OTP generated: userId={}, purpose={}", userId, purpose);
        // TODO: dispatch rawCode via SMS gateway
        log.warn("SMS dispatch not yet implemented: userId={}", userId);
        otpService.devPut(userId, rawCode);
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────

    @Transactional
    public VerifyOtpResult verifyOtp(UUID userId, String code, OtpPurpose purpose,
                                     String deviceFingerprint, String platform, String ipAddress) {
        log.info("OTP verification: userId={}, purpose={}, platform={}", userId, purpose, platform);

        otpService.verifyCode(userId, code, purpose);
        log.info("OTP verified: userId={}", userId);

        SessionOpenResult result =
                sessionService.openSession(userId, deviceFingerprint, platform, ipAddress);

        if (result.isNewDevice()) {
            log.warn("New device detected: userId={} — biometric step-up required", userId);
        }
        return new VerifyOtpResult(result.session(), result.isNewDevice());
    }

    public User findByPhone(String phoneE164) {
        return userRepository.findByPhoneE164(phoneE164)
                .map(UserEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("User not registered"));
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    public record VerifyOtpResult(
            uz.pulsepay.domain.identity.Session session,
            boolean requiresBiometricStepUp) {}
}
