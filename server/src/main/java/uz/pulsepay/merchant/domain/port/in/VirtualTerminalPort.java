package uz.pulsepay.merchant.domain.port.in;

import uz.pulsepay.transfer.domain.model.Transfer;

import java.util.UUID;

public interface VirtualTerminalPort {

    record ChargeCommand(
            String customerPhone,
            UUID customerInstrumentId,
            String cardNetwork,
            long amountTiyin,
            Integer purposeCodeId
    ) {}

    Transfer charge(UUID merchantId, ChargeCommand cmd);
}
