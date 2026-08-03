package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.shared.exception.NotFoundException;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferEntity;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferStatus;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransferJpaAdapter implements TransferRepository {

    private final TransferJpaRepository jpa;

    TransferJpaAdapter(TransferJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transfer save(Transfer transfer) {
        return jpa.save(TransferEntity.fromDomain(transfer)).toDomain();
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return jpa.findById(id).map(TransferEntity::toDomain);
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(TransferEntity::toDomain);
    }

    @Override
    public Transfer updateStatus(UUID id, TransferStatus newStatus, String reason) {
        Instant completedAt = (newStatus == TransferStatus.COMPLETED || newStatus == TransferStatus.FAILED)
                ? Instant.now() : null;
        jpa.updateStatus(id, newStatus, completedAt);
        return jpa.findById(id)
                .map(TransferEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + id));
    }
}
