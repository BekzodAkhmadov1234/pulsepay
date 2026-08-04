package uz.pulsepay.identity.domain.port.in;

import uz.pulsepay.identity.domain.model.OtpPurpose;

import java.util.UUID;

public interface RequestOtpPort {
    void requestOtp(UUID userId, OtpPurpose purpose, UUID targetId);
}
