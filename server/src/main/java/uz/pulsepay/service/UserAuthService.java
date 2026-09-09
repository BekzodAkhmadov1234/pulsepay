package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.identity.AccountInactiveException;
import uz.pulsepay.domain.identity.DuplicatePhoneException;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.domain.identity.UserEntity;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.repository.UserRepository;
import uz.pulsepay.service.SessionService.SessionOpenResult;
import uz.pulsepay.utils.security.JwtService;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class UserAuthService {

    private static final String STATUS_PENDING   = "pending";
    private static final String STATUS_ACTIVE    = "active";
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

    // ── Login (OTP already verified via /auth/otp + /auth/verify) ────────────

    @Transactional(readOnly = true)
    public String login(String phoneE164) {
        log.info("Login attempt: phone={}", phoneE164);
        User user = userRepository.findByPhoneE164(phoneE164)
                .map(UserEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("error.not_found.user"));
        if (!user.isActive()) {
            log.warn("Login rejected — account inactive: userId={}", user.id());
            throw new AccountInactiveException(phoneE164);
        }
        log.info("Login successful: userId={}", user.id());
        return jwtService.generateUserToken(user);
    }

    // ── Registration — Step 1: send OTP ──────────────────────────────────────

    /**
     * Creates a pending user (if phone is new) and sends a REGISTRATION OTP.
     * Idempotent: if a pending user already exists for the phone, just resends the OTP.
     */
    @Transactional
    public void registerOtp(String phoneE164) {
        log.info("Registration OTP requested: phone={}", phoneE164);

        User user = userRepository.findByPhoneE164(phoneE164)
                .map(UserEntity::toDomain)
                .map(u -> {
                    if (STATUS_ACTIVE.equals(u.status()) || "suspended".equals(u.status())) {
                        throw new DuplicatePhoneException(phoneE164);
                    }
                    log.debug("Pending user exists — resending registration OTP: userId={}", u.id());
                    return u;
                })
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    User newUser = new User(UUID.randomUUID(), phoneE164, null,
                            STATUS_PENDING, DEFAULT_KYC_LEVEL, null, now, now, null, 0);
                    userRepository.upsertParty(newUser.id());
                    User saved = userRepository.save(UserEntity.fromDomain(newUser)).toDomain();
                    log.info("Pending user created: userId={}", saved.id());
                    return saved;
                });

        String rawCode = otpService.generateAndSave(user.id(), OtpPurpose.REGISTRATION, null);
        otpService.devPut(user.id(), rawCode);
        log.debug("Registration OTP generated: userId={}", user.id());
    }

    // ── Registration — Step 2: verify OTP and activate ───────────────────────

    @Transactional
    public String registerConfirm(String phoneE164, String code, String fullName,
                                  String deviceFingerprint, String platform, String ip) {
        log.info("Registration confirm: phone={}", phoneE164);

        User user = userRepository.findByPhoneE164(phoneE164)
                .map(UserEntity::toDomain)
                .filter(u -> STATUS_PENDING.equals(u.status()))
                .orElseThrow(() -> new NotFoundException("error.not_found.user"));

        otpService.verifyCode(user.id(), code, OtpPurpose.REGISTRATION);

        Instant now = Instant.now();
        User activated = new User(user.id(), user.phoneE164(), fullName,
                STATUS_ACTIVE, user.kycLevel(), null, user.createdAt(), now, null, user.version());
        userRepository.save(UserEntity.fromDomain(activated));

        sessionService.openSession(user.id(), deviceFingerprint, platform, ip);
        log.info("User registered and activated: userId={}", user.id());
        return jwtService.generateUserToken(activated);
    }

    // ── Request OTP (login flow) ──────────────────────────────────────────────

    public void requestOtp(UUID userId, OtpPurpose purpose, UUID targetId) {
        log.info("OTP requested: userId={}, purpose={}", userId, purpose);
        userRepository.findById(userId)
                .map(UserEntity::toDomain)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("error.not_found.user"));
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

    // ── Account Self-Delete — Step 1: send OTP ───────────────────────────────

    public void closeAccountOtp(UUID userId) {
        log.info("Account close OTP requested: userId={}", userId);
        User user = findById(userId);
        if (!user.isActive()) {
            throw new DomainException("Account is not active");
        }
        String rawCode = otpService.generateAndSave(userId, OtpPurpose.ACCOUNT_DELETE, null);
        otpService.devPut(userId, rawCode);
    }

    // ── Account Self-Delete — Step 2: verify OTP and close ───────────────────

    @Transactional
    public void closeAccountConfirm(UUID userId, String code) {
        log.info("Account close confirm: userId={}", userId);
        User user = findById(userId);
        otpService.verifyCode(userId, code, OtpPurpose.ACCOUNT_DELETE);
        Instant now = Instant.now();
        User closed = new User(user.id(), user.phoneE164(), user.fullName(),
                "closed", user.kycLevel(), user.biometricVerifiedAt(),
                user.createdAt(), now, now, user.version());
        userRepository.save(UserEntity.fromDomain(closed));
        sessionService.revokeAllSessions(userId);
        log.info("Account closed: userId={}", userId);
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public User findByPhone(String phoneE164) {
        return userRepository.findByPhoneE164(phoneE164)
                .map(UserEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("error.not_found.user"));
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("error.not_found.user"));
    }

    public record VerifyOtpResult(
            uz.pulsepay.domain.identity.Session session,
            boolean requiresBiometricStepUp) {}
}
