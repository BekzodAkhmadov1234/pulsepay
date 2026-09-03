package uz.pulsepay.domain.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p2s_transfer_details")
public class P2STransferDetailsEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "service_code", nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "service_fields", nullable = false, columnDefinition = "TEXT")
    private String serviceFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected P2STransferDetailsEntity() {}

    public P2STransferDetailsEntity(UUID id, UUID transferId, String serviceCode,
                                    String serviceFields, Instant createdAt) {
        this.id            = id;
        this.transferId    = transferId;
        this.serviceCode   = serviceCode;
        this.serviceFields = serviceFields;
        this.createdAt     = createdAt;
    }

    public P2STransferDetails toDomain() {
        return new P2STransferDetails(id, transferId, serviceCode, serviceFields, createdAt);
    }

    public static P2STransferDetailsEntity fromDomain(P2STransferDetails d) {
        return new P2STransferDetailsEntity(d.id(), d.transferId(), d.serviceCode(),
                d.serviceFields(), d.createdAt());
    }
}
