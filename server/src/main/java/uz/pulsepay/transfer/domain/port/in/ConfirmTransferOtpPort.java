package uz.pulsepay.transfer.domain.port.in;

import uz.pulsepay.transfer.domain.model.Transfer;

import java.util.UUID;

public interface ConfirmTransferOtpPort {
    Transfer confirmOtp(UUID transferId, UUID userId, String otpCode);
}
