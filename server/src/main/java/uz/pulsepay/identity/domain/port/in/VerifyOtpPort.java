package uz.pulsepay.identity.domain.port.in;

import uz.pulsepay.identity.application.usecase.VerifyOtpUseCase;
import uz.pulsepay.identity.domain.model.OtpPurpose;

import java.util.UUID;

public interface VerifyOtpPort {

    /**
     * @param platform ios or android
     * @return session + biometric step-up flag (true if new device — AUTH-02 requires step-up)
     */
    VerifyOtpUseCase.VerifyOtpResult verifyOtp(UUID userId, String code, OtpPurpose purpose,
                                                String deviceFingerprint, String platform, String ipAddress);
}
