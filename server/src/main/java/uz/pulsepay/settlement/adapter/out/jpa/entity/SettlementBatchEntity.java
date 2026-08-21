package uz.pulsepay.settlement.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.settlement.domain.model.BatchType;
import uz.pulsepay.settlement.domain.model.SettlementBatch;
import uz.pulsepay.settlement.domain.model.SettlementStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches")
public class SettlementBatchEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Convert(converter = BatchTypeConverter.class)
    @Column(name = "batch_type", nullable = false, length = 20)
    private BatchType batchType;

    @Column(name = "merchant_account_id")
    private UUID merchantAccountId;

    @Column(name = "payment_network_id")
    private Integer paymentNetworkId;

    @Column(name = "operational_date", nullable = false)
    private LocalDate operationalDate;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Convert(converter = SettlementStatusConverter.class)
    @Column(name = "status", nullable = false, length = 15)
    private SettlementStatus status;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    protected SettlementBatchEntity() {}

    public SettlementBatchEntity(UUID id, BatchType batchType, UUID merchantAccountId,
                                  Integer paymentNetworkId, LocalDate operationalDate,
                                  long totalAmount, SettlementStatus status,
                                  Instant generatedAt, Instant settledAt) {
        this.id = id;
        this.batchType = batchType;
        this.merchantAccountId = merchantAccountId;
        this.paymentNetworkId = paymentNetworkId;
        this.operationalDate = operationalDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.generatedAt = generatedAt;
        this.settledAt = settledAt;
    }

    public SettlementBatch toDomain() {
        return new SettlementBatch(id, batchType, merchantAccountId, paymentNetworkId,
                operationalDate, totalAmount, status, generatedAt, settledAt);
    }

    public static SettlementBatchEntity fromDomain(SettlementBatch b) {
        return new SettlementBatchEntity(b.id(), b.batchType(), b.merchantAccountId(),
                b.paymentNetworkId(), b.operationalDate(), b.totalAmount(), b.status(),
                b.generatedAt(), b.settledAt());
    }
}
