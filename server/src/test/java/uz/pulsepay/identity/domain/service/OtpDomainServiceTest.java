package uz.pulsepay.identity.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.identity.CooldownType;
import uz.pulsepay.domain.identity.OtpCode;
import uz.pulsepay.domain.identity.OtpCodeEntity;
import uz.pulsepay.domain.identity.OtpProperties;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.identity.SecurityCooldown;
import uz.pulsepay.domain.identity.SecurityCooldownEntity;
import uz.pulsepay.repository.OtpCodeRepository;
import uz.pulsepay.repository.SecurityCooldownRepository;
import uz.pulsepay.service.OtpService;
import uz.pulsepay.utils.exception.DomainException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 1 MANDATORY tests (REG-03/AUTH-03):
 *   - 6-digit OTP generated
 *   - 59-second default expiry (configurable)
 *   - 3-attempt cap (configurable)
 *   - 15-minute lockout after max attempts
 *   - Locked user cannot request new OTP
 *   - Locked user cannot verify any code
 *   - Expired OTP is rejected
 *   - Consumed OTP is rejected
 */
class OtpDomainServiceTest {

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    private OtpCodeRepository otpCodeRepository;
    private SecurityCooldownRepository cooldownRepository;
    private OtpProperties props;
    private OtpService service;

    @BeforeEach
    void setUp() {
        otpCodeRepository   = mock(OtpCodeRepository.class);
        cooldownRepository  = mock(SecurityCooldownRepository.class);
        // Default config per spec: 59s expiry, 3 attempts, 15-min lockout
        props   = new OtpProperties(59, 3, 15);
        service = new OtpService(props, otpCodeRepository, cooldownRepository);

        when(otpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cooldownRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Default: no active lockout
        when(cooldownRepository.findActiveCooldown(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    // ── REG-03: code generation ────────────────────────────────────────────

    @Test
    void generateAndSave_produces_6_digit_code() {
        String code = service.generateAndSave(USER_ID, OtpPurpose.LOGIN, null);

        assertThat(code).matches("\\d{6}");
    }

    @Test
    void generateAndSave_saves_hash_not_plaintext() {
        AtomicReference<OtpCodeEntity> saved = new AtomicReference<>();
        when(otpCodeRepository.save(any())).thenAnswer(inv -> {
            saved.set(inv.getArgument(0));
            return inv.getArgument(0);
        });

        String plainCode = service.generateAndSave(USER_ID, OtpPurpose.LOGIN, null);

        OtpCode savedDomain = saved.get().toDomain();
        assertThat(savedDomain.codeHash()).isNotEqualTo(plainCode);
        assertThat(savedDomain.codeHash()).hasSize(64); // SHA-256 hex
    }

    @Test
    void generateAndSave_expiry_uses_configured_validity_seconds() {
        Instant before = Instant.now();
        AtomicReference<OtpCodeEntity> saved = new AtomicReference<>();
        when(otpCodeRepository.save(any())).thenAnswer(inv -> {
            saved.set(inv.getArgument(0));
            return inv.getArgument(0);
        });

        service.generateAndSave(USER_ID, OtpPurpose.LOGIN, null);

        Instant expiry = saved.get().toDomain().expiresAt();
        // Should be ~59 seconds from now (within ±2s tolerance)
        assertThat(expiry).isAfter(before.plusSeconds(57));
        assertThat(expiry).isBefore(before.plusSeconds(61));
    }

    @Test
    void generateAndSave_blocked_when_lockout_active() {
        SecurityCooldown activeLock = lockout(Instant.now().plusSeconds(900));
        when(cooldownRepository.findActiveCooldown(eq(USER_ID), eq("otp_lockout"), any(Instant.class)))
                .thenReturn(Optional.of(SecurityCooldownEntity.fromDomain(activeLock)));

        assertThatThrownBy(() -> service.generateAndSave(USER_ID, OtpPurpose.LOGIN, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("OTP locked");

        verify(otpCodeRepository, never()).save(any());
    }

    // ── REG-03: verification and lockout ──────────────────────────────────

    @Test
    void verifyCode_succeeds_on_correct_code() {
        String rawCode = "123456";
        OtpCode otp = validOtp(rawCode, 0);
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(OtpCodeEntity.fromDomain(otp)));

        OtpCode result = service.verifyCode(USER_ID, rawCode, OtpPurpose.LOGIN);

        assertThat(result.id()).isEqualTo(otp.id());
        verify(otpCodeRepository).markConsumed(eq(otp.id()), any(Instant.class));
    }

    @Test
    void verifyCode_increments_attempt_on_wrong_code() {
        String rawCode = "999999";
        OtpCode otp = validOtp("111111", 0);
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(OtpCodeEntity.fromDomain(otp)));
        when(otpCodeRepository.findAttemptCount(otp.id())).thenReturn((short) 1);

        assertThatThrownBy(() -> service.verifyCode(USER_ID, rawCode, OtpPurpose.LOGIN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invalid OTP code");

        verify(otpCodeRepository).incrementAttemptCount(otp.id());
        verify(cooldownRepository, never()).save(any());
    }

    @Test
    void verifyCode_locks_user_after_max_attempts_reached() {
        String rawCode = "wrong";
        OtpCode otp = validOtp("correct", 2); // already at 2 attempts
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(OtpCodeEntity.fromDomain(otp)));
        when(otpCodeRepository.findAttemptCount(otp.id())).thenReturn((short) 3); // now at max (3)

        assertThatThrownBy(() -> service.verifyCode(USER_ID, rawCode, OtpPurpose.LOGIN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("locked");

        // MANDATORY: lockout must be persisted
        verify(cooldownRepository).save(argThat(entity -> {
            SecurityCooldown c = entity.toDomain();
            return c.cooldownType() == CooldownType.OTP_LOCKOUT && c.userId().equals(USER_ID);
        }));
    }

    @Test
    void verifyCode_lockout_duration_is_configured_minutes() {
        OtpCode otp = validOtp("correct", 2);
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(OtpCodeEntity.fromDomain(otp)));
        when(otpCodeRepository.findAttemptCount(otp.id())).thenReturn((short) 3);

        Instant before = Instant.now();
        try { service.verifyCode(USER_ID, "wrong", OtpPurpose.LOGIN); } catch (DomainException ignored) {}

        verify(cooldownRepository).save(argThat(entity -> {
            SecurityCooldown c = entity.toDomain();
            long lockMinutes = java.time.Duration.between(before, c.lockedUntil()).toMinutes();
            return lockMinutes >= 14 && lockMinutes <= 16; // ~15 minutes
        }));
    }

    @Test
    void verifyCode_blocked_when_lockout_active() {
        SecurityCooldown activeLock = lockout(Instant.now().plusSeconds(900));
        when(cooldownRepository.findActiveCooldown(eq(USER_ID), eq("otp_lockout"), any(Instant.class)))
                .thenReturn(Optional.of(SecurityCooldownEntity.fromDomain(activeLock)));

        assertThatThrownBy(() -> service.verifyCode(USER_ID, "123456", OtpPurpose.LOGIN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("OTP locked");

        verify(otpCodeRepository, never()).findLatestByUserIdAndPurpose(any(), any());
    }

    @Test
    void verifyCode_rejects_expired_otp() {
        OtpCode expired = new OtpCode(UUID.randomUUID(), USER_ID, OtpPurpose.LOGIN,
                sha256("code"), null,
                Instant.now().minusSeconds(10), // expired
                null, (short) 0);
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(OtpCodeEntity.fromDomain(expired)));

        assertThatThrownBy(() -> service.verifyCode(USER_ID, "code", OtpPurpose.LOGIN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyCode_rejects_consumed_otp() {
        OtpCode consumed = new OtpCode(UUID.randomUUID(), USER_ID, OtpPurpose.LOGIN,
                sha256("code"), null,
                Instant.now().plusSeconds(59),
                Instant.now().minusSeconds(5), // consumed
                (short) 0);
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(OtpCodeEntity.fromDomain(consumed)));

        assertThatThrownBy(() -> service.verifyCode(USER_ID, "code", OtpPurpose.LOGIN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void verifyCode_no_pending_otp_throws() {
        when(otpCodeRepository.findLatestByUserIdAndPurpose(USER_ID, OtpPurpose.LOGIN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCode(USER_ID, "123456", OtpPurpose.LOGIN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("No pending OTP");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private OtpCode validOtp(String rawCode, int attempts) {
        return new OtpCode(UUID.randomUUID(), USER_ID, OtpPurpose.LOGIN,
                sha256(rawCode), TARGET_ID,
                Instant.now().plusSeconds(59), null, (short) attempts);
    }

    private SecurityCooldown lockout(Instant until) {
        return new SecurityCooldown(UUID.randomUUID(), USER_ID,
                CooldownType.OTP_LOCKOUT, until, "test lockout", Instant.now());
    }

    private static String sha256(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
