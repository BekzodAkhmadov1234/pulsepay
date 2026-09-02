package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.domain.identity.OtpCode;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.shared.DomainException;

import java.util.UUID;

/**
 * OTP generation and verification for the transfer confirmation flow.
 * Replaces TransferOtpAdapter / TransferOtpPort from the hexagonal architecture.
 */
@Slf4j
@Service
public class TransferOtpService {

    private final OtpService otpService;

    public TransferOtpService(OtpService otpService) {
        this.otpService = otpService;
    }

    public void generate(UUID userId, UUID transferId) {
        String rawCode = otpService.generateAndSave(userId, OtpPurpose.TRANSFER, transferId);
        // TODO: dispatch rawCode via SMS gateway (PaySys/MONTRA)
        log.warn("SMS dispatch not yet implemented — OTP not delivered: userId={}", userId);
        otpService.devPut(userId, rawCode);
    }

    public void verify(UUID userId, String rawCode, UUID transferId) {
        OtpCode otp = otpService.verifyCode(userId, rawCode, OtpPurpose.TRANSFER);
        if (!transferId.equals(otp.targetId())) {
            throw new DomainException("OTP was not issued for this transfer");
        }
    }
}
