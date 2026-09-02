package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.BatchTypeConverter;
import uz.pulsepay.domain.converter.SettlementStatusConverter;
import uz.pulsepay.domain.enums.BatchType;
import uz.pulsepay.domain.enums.SettlementStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches")
public class SettlementBatch {

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

    protected SettlementBatch() {}

    public SettlementBatch(UUID id, BatchType batchType, UUID merchantAccountId,
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

    public UUID getId() { return id; }
    public BatchType getBatchType() { return batchType; }
    public UUID getMerchantAccountId() { return merchantAccountId; }
    public Integer getPaymentNetworkId() { return paymentNetworkId; }
    public LocalDate getOperationalDate() { return operationalDate; }
    public long getTotalAmount() { return totalAmount; }
    public SettlementStatus getStatus() { return status; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getSettledAt() { return settledAt; }

    public void setStatus(SettlementStatus status) { this.status = status; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
}
