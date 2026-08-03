package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferEntity;
import uz.pulsepay.transfer.domain.model.TransferStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {
    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("UPDATE TransferEntity t SET t.status = :status, t.completedAt = :completedAt WHERE t.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") TransferStatus status,
                      @Param("completedAt") Instant completedAt);
}
