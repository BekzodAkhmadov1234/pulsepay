package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.TransferChannelConverter;
import uz.pulsepay.domain.converter.TransferStatusConverter;
import uz.pulsepay.domain.enums.TransferChannel;
import uz.pulsepay.domain.enums.TransferStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "amount", nullable = false)
    private long amountTiyin;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "fee_amount", nullable = false)
    private long feeAmountTiyin;

    @Convert(converter = TransferStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "network_reference")
    private String networkReference;

    @Column(name = "applied_fee_rule_id")
    private UUID appliedFeeRuleId;

    @Column(name = "applied_route_id")
    private UUID appliedRouteId;

    @Column(name = "transfer_type_id", nullable = false)
    private int transferTypeId;

    @Column(name = "purpose_code_id")
    private Integer purposeCodeId;

    @Convert(converter = TransferChannelConverter.class)
    @Column(name = "channel", nullable = false, length = 10)
    private TransferChannel channel;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Transfer() {}

    public Transfer(UUID id, long amountTiyin, String currencyCode, long feeAmountTiyin,
                    TransferStatus status, String idempotencyKey, String networkReference,
                    UUID appliedFeeRuleId, UUID appliedRouteId, int transferTypeId,
                    Integer purposeCodeId, TransferChannel channel,
                    Instant initiatedAt, Instant completedAt) {
        this.id = id;
        this.amountTiyin = amountTiyin;
        this.currencyCode = currencyCode;
        this.feeAmountTiyin = feeAmountTiyin;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.networkReference = networkReference;
        this.appliedFeeRuleId = appliedFeeRuleId;
        this.appliedRouteId = appliedRouteId;
        this.transferTypeId = transferTypeId;
        this.purposeCodeId = purposeCodeId;
        this.channel = channel;
        this.initiatedAt = initiatedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public long getAmountTiyin() { return amountTiyin; }
    public String getCurrencyCode() { return currencyCode; }
    public long getFeeAmountTiyin() { return feeAmountTiyin; }
    public TransferStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getNetworkReference() { return networkReference; }
    public UUID getAppliedFeeRuleId() { return appliedFeeRuleId; }
    public UUID getAppliedRouteId() { return appliedRouteId; }
    public int getTransferTypeId() { return transferTypeId; }
    public Integer getPurposeCodeId() { return purposeCodeId; }
    public TransferChannel getChannel() { return channel; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void setStatus(TransferStatus status) { this.status = status; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setNetworkReference(String networkReference) { this.networkReference = networkReference; }
    public void setAppliedFeeRuleId(UUID appliedFeeRuleId) { this.appliedFeeRuleId = appliedFeeRuleId; }
    public void setAppliedRouteId(UUID appliedRouteId) { this.appliedRouteId = appliedRouteId; }
}
