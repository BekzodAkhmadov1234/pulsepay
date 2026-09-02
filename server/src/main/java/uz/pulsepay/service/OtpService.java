package uz.pulsepay.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uz.pulsepay.domain.identity.OtpCodeEntity;
import uz.pulsepay.domain.identity.SecurityCooldownEntity;
import uz.pulsepay.domain.identity.OtpCode;
import uz.pulsepay.domain.identity.OtpProperties;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.identity.SecurityCooldown;
import uz.pulsepay.domain.identity.CooldownType;
import uz.pulsepay.repository.OtpCodeRepository;
import uz.pulsepay.repository.SecurityCooldownRepository;
import uz.pulsepay.utils.exception.DomainException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP lifecycle — generation, verification, lockout.
 *
 * Merges OtpDomainService + DevOtpStore into a single service.
 *
 * Enforces REG-03/AUTH-03 (MANDATORY/REGULATORY):
 *   - 6-digit code
 *   - Configurable expiry (default 59 seconds)
 *   - Configurable max attempts (default 3)
 *   - 15-minute lockout after max attempts exhausted
 *
 * The dev-only in-memory store (DevOtpStore) logic is folded in here
 * and activated only when the {@code dev} Spring profile is active.
 */
@Service
public class OtpService {

    private final OtpProperties otpProperties;
    private final OtpCodeRepository otpCodeRepository;
    private final SecurityCooldownRepository cooldownRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // Dev-only in-memory OTP store — holds the most recent raw OTP code per user.
    // Only populated when the dev profile is active (used by DevController).
    private final ConcurrentHashMap<UUID, String> devStore = new ConcurrentHashMap<>();

    public OtpService(OtpProperties otpProperties,
                      OtpCodeRepository otpCodeRepository,
                      SecurityCooldownRepository cooldownRepository) {
        this.otpProperties      = otpProperties;
        this.otpCodeRepository  = otpCodeRepository;
        this.cooldownRepository = cooldownRepository;
    }

    /**
     * Generates a 6-digit OTP, hashes it, and persists it.
     * Rejects if the user is currently locked out (REG-03).
     *
     * @return the raw (plaintext) code — caller is responsible for delivering it via SMS
     */
    public String generateAndSave(UUID userId, OtpPurpose purpose, UUID targetId) {
        // REG-03: reject if the user is currently locked out
        cooldownRepository.findActiveCooldown(
                        userId, CooldownType.OTP_LOCKOUT.name().toLowerCase(), Instant.now())
                .map(SecurityCooldownEntity::toDomain)
                .filter(SecurityCooldown::isActive)
                .ifPresent(c -> {
                    throw new DomainException(
                            "OTP locked until " + c.lockedUntil() + " — too many failed attempts");
                });

        String rawCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hash = sha256(rawCode);
        OtpCode otpCode = new OtpCode(
                UUID.randomUUID(), userId, purpose, hash, targetId,
                Instant.now().plusSeconds(otpProperties.validitySeconds()), null, (short) 0);
        otpCodeRepository.save(OtpCodeEntity.fromDomain(otpCode));
        return rawCode;
    }

    /**
     * Verifies a submitted OTP code.
     * Increments attempt count on failure; locks the user after max attempts (REG-03).
     *
     * @throws DomainException on expired, consumed, locked-out, or wrong code
     */
    public OtpCode verifyCode(UUID userId, String rawCode, OtpPurpose purpose) {
        // Check lockout first (fast path)
        cooldownRepository.findActiveCooldown(
                        userId, CooldownType.OTP_LOCKOUT.name().toLowerCase(), Instant.now())
                .map(SecurityCooldownEntity::toDomain)
                .filter(SecurityCooldown::isActive)
                .ifPresent(c -> {
                    throw new DomainException(
                            "OTP locked until " + c.lockedUntil() + " — too many failed attempts");
                });

        OtpCode otp = otpCodeRepository
                .findLatestByUserIdAndPurpose(userId, purpose)
                .map(OtpCodeEntity::toDomain)
                .orElseThrow(() -> new DomainException("No pending OTP found"));

        if (otp.consumedAt() != null) {
            throw new DomainException("OTP already used");
        }
        if (otp.expiresAt().isBefore(Instant.now())) {
            throw new DomainException("OTP has expired");
        }
        if (otp.attemptCount() >= otpProperties.maxAttempts()) {
            // Should have been locked — create/refresh lockout and reject
            applyLockout(userId, "attempt count already at maximum");
            throw new DomainException("OTP locked — too many attempts");
        }

        if (!sha256(rawCode).equals(otp.codeHash())) {
            otpCodeRepository.incrementAttemptCount(otp.id());
            short newCount = otpCodeRepository.findAttemptCount(otp.id());
            if (newCount >= otpProperties.maxAttempts()) {
                applyLockout(userId, "reached " + otpProperties.maxAttempts() + " failed attempts");
                throw new DomainException("OTP locked — too many failed attempts");
            }
            throw new DomainException("Invalid OTP code");
        }

        otpCodeRepository.markConsumed(otp.id(), Instant.now());
        return otp;
    }

    // ── Dev-only store methods (used by DevController, active on dev profile) ──

    /**
     * Stores the raw OTP code in the dev-only in-memory map.
     * Should only be called when the {@code dev} profile is active.
     */
    public void devPut(UUID userId, String rawCode) {
        devStore.put(userId, rawCode);
    }

    /**
     * Retrieves the raw OTP code from the dev-only in-memory map.
     * Should only be called when the {@code dev} profile is active.
     */
    public Optional<String> devGet(UUID userId) {
        return Optional.ofNullable(devStore.get(userId));
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private void applyLockout(UUID userId, String reason) {
        Instant lockedUntil = Instant.now().plus(otpProperties.lockoutMinutes(), ChronoUnit.MINUTES);
        SecurityCooldown cooldown = new SecurityCooldown(
                UUID.randomUUID(), userId, CooldownType.OTP_LOCKOUT,
                lockedUntil, reason, Instant.now());
        cooldownRepository.save(SecurityCooldownEntity.fromDomain(cooldown));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
