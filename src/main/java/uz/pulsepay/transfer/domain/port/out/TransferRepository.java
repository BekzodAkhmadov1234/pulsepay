package uz.pulsepay.transfer.domain.port.out;

import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferStatus;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(UUID id);
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
    Transfer updateStatus(UUID id, TransferStatus newStatus, String reason);
}
