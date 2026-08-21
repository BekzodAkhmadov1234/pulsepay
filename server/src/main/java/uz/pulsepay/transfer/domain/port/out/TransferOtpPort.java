package uz.pulsepay.transfer.domain.port.out;

import java.util.UUID;

public interface TransferOtpPort {
    void generate(UUID userId, UUID transferId);
    void verify(UUID userId, String rawCode, UUID transferId);
}
