package uz.pulsepay.identity.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.identity.domain.model.*;
import uz.pulsepay.identity.domain.port.out.OtpCodeRepository;
import uz.pulsepay.identity.domain.port.out.SecurityCooldownRepository;
import uz.pulsepay.shared.exception.DomainException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * OTP lifecycle — generation, verification, lockout.
 *
 * Enforces REG-03/AUTH-03 (MANDATORY/REGULATORY):
 *   - 6-digit code
 *   - Configurable expiry (default 59 seconds)
 *   - Configurable max attempts (default 3)
 *   - 15-minute lockout after max attempts exhausted
 */
@Service
public class OtpDomainService {

    private final OtpProperties otpProperties;
    private final OtpCodeRepository otpCodeRepository;
    private final SecurityCooldownRepository cooldownRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpDomainService(OtpProperties otpProperties,
                            OtpCodeRepository otpCodeRepository,
                            SecurityCooldownRepository cooldownRepository) {
        this.otpProperties     = otpProperties;
        this.otpCodeRepository = otpCodeRepository;
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
        cooldownRepository.findActiveCooldown(userId, CooldownType.OTP_LOCKOUT)
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
        otpCodeRepository.save(otpCode);
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
        cooldownRepository.findActiveCooldown(userId, CooldownType.OTP_LOCKOUT)
                .filter(SecurityCooldown::isActive)
                .ifPresent(c -> {
                    throw new DomainException(
                            "OTP locked until " + c.lockedUntil() + " — too many failed attempts");
                });

        OtpCode otp = otpCodeRepository
                .findLatestByUserIdAndPurpose(userId, purpose)
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
            int newCount = otpCodeRepository.incrementAttemptCount(otp.id());
            if (newCount >= otpProperties.maxAttempts()) {
                applyLockout(userId, "reached " + otpProperties.maxAttempts() + " failed attempts");
                throw new DomainException("OTP locked — too many failed attempts");
            }
            throw new DomainException("Invalid OTP code");
        }

        otpCodeRepository.markConsumed(otp.id());
        return otp;
    }

    private void applyLockout(UUID userId, String reason) {
        Instant lockedUntil = Instant.now().plus(otpProperties.lockoutMinutes(), ChronoUnit.MINUTES);
        cooldownRepository.save(new SecurityCooldown(
                UUID.randomUUID(), userId, CooldownType.OTP_LOCKOUT,
                lockedUntil, reason, Instant.now()));
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
