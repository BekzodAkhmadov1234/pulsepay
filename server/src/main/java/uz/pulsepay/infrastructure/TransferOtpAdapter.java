package uz.pulsepay.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.identity.application.usecase.DevOtpStore;
import uz.pulsepay.identity.domain.model.OtpCode;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.service.OtpDomainService;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.transfer.domain.port.out.TransferOtpPort;

import java.util.Optional;
import java.util.UUID;

/**
 * Wires the transfer module's OTP port to the identity domain's OtpDomainService.
 * Lives in infrastructure to avoid coupling transfer ↔ identity modules directly.
 */
@Slf4j
@Service
public class TransferOtpAdapter implements TransferOtpPort {

    private final OtpDomainService otpDomainService;
    private final Optional<DevOtpStore> devOtpStore;

    public TransferOtpAdapter(OtpDomainService otpDomainService,
                              Optional<DevOtpStore> devOtpStore) {
        this.otpDomainService = otpDomainService;
        this.devOtpStore = devOtpStore;
    }

    @Override
    public void generate(UUID userId, UUID transferId) {
        String rawCode = otpDomainService.generateAndSave(userId, OtpPurpose.TRANSFER, transferId);
        // TODO: dispatch rawCode via SMS gateway (PaySys/MONTRA)
        log.warn("SMS dispatch not yet implemented — OTP not delivered: userId={}", userId);
        devOtpStore.ifPresent(s -> s.put(userId, rawCode));
    }

    @Override
    public void verify(UUID userId, String rawCode, UUID transferId) {
        OtpCode otp = otpDomainService.verifyCode(userId, rawCode, OtpPurpose.TRANSFER);
        if (!transferId.equals(otp.targetId())) {
            throw new DomainException("OTP was not issued for this transfer");
        }
    }
}
