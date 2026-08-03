package uz.pulsepay.transfer.domain.port.in;

import uz.pulsepay.transfer.domain.model.Transfer;

import java.util.UUID;

public interface GetTransferPort {
    Transfer getById(UUID transferId, UUID requestingUserId);
}
