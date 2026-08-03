package uz.pulsepay.transfer.domain.port.in;

import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferChannel;

import java.util.UUID;

public interface InitiateTransferPort {
    Transfer initiate(UUID senderId, UUID senderInstrumentId, String senderCardNetwork,
                      UUID recipientId, UUID recipientInstrumentId, String recipientCardNetwork,
                      Money amount, int transferTypeId, Integer purposeCodeId,
                      TransferChannel channel, String idempotencyKey);
}
