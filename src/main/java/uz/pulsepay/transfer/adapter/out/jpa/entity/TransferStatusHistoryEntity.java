package uz.pulsepay.transfer.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.transfer.domain.model.TransferStatus;
import uz.pulsepay.transfer.domain.model.TransferStatusHistory;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_status_history")
public class TransferStatusHistoryEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TransferStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private TransferStatus toStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected TransferStatusHistoryEntity() {}

    public TransferStatusHistoryEntity(UUID id, UUID transferId, TransferStatus fromStatus,
                                 TransferStatus toStatus, String reason, Instant changedAt) {
        this.id = id;
        this.transferId = transferId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public static TransferStatusHistoryEntity fromDomain(TransferStatusHistory h) {
        return new TransferStatusHistoryEntity(h.id(), h.transferId(), h.fromStatus(),
                h.toStatus(), h.reason(), h.changedAt());
    }
}
