package uz.pulsepay.identity.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.model.Session;
import uz.pulsepay.identity.domain.port.in.VerifyOtpPort;
import uz.pulsepay.identity.domain.service.OtpDomainService;
import uz.pulsepay.identity.domain.service.SessionDomainService;

import java.util.UUID;

/**
 * OTP verification → session open.
 *
 * Phase 1 (AUTH-01/AUTH-02/DEV-01):
 *   - Verifies the OTP (3-attempt cap + 15-min lockout from OtpDomainService)
 *   - Opens a session and detects new devices
 *   - If the device is new, cards are deactivated (MANDATORY Phase 2) inside SessionDomainService
 *   - Returns whether biometric step-up is required (AUTH-02) so the REST layer can prompt the user
 */
@Slf4j
@Service
public class VerifyOtpUseCase implements VerifyOtpPort {

    private final OtpDomainService otpDomainService;
    private final SessionDomainService sessionDomainService;

    public VerifyOtpUseCase(OtpDomainService otpDomainService, SessionDomainService sessionDomainService) {
        this.otpDomainService  = otpDomainService;
        this.sessionDomainService = sessionDomainService;
    }

    @Override
    @Transactional
    public VerifyOtpResult verifyOtp(UUID userId, String code, OtpPurpose purpose,
                                     String deviceFingerprint, String platform, String ipAddress) {
        log.info("OTP verification attempt: userId={}, purpose={}, platform={}", userId, purpose, platform);

        otpDomainService.verifyCode(userId, code, purpose);
        log.info("OTP verified successfully: userId={}, purpose={}", userId, purpose);

        SessionDomainService.SessionOpenResult result =
                sessionDomainService.openSession(userId, deviceFingerprint, platform, ipAddress);

        if (result.isNewDevice()) {
            log.warn("New device detected: userId={}, platform={}, ip={} — biometric step-up required",
                    userId, platform, ipAddress);
        }

        // AUTH-02: if new device was detected, caller must prompt biometric step-up
        return new VerifyOtpResult(result.session(), result.isNewDevice());
    }

    public record VerifyOtpResult(Session session, boolean requiresBiometricStepUp) {}
}
