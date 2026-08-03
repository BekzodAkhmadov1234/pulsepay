package uz.pulsepay.transfer.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferChannel;
import uz.pulsepay.transfer.domain.model.TransferStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class TransferEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "amount_tiyin", nullable = false)
    private long amountTiyin;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "fee_amount_tiyin", nullable = false)
    private long feeAmountTiyin;

    @Enumerated(EnumType.STRING)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private TransferChannel channel;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TransferEntity() {}

    public TransferEntity(UUID id, long amountTiyin, String currencyCode, long feeAmountTiyin,
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

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Transfer toDomain() {
        return new Transfer(id,
                new Money(amountTiyin, CurrencyCode.valueOf(currencyCode)),
                new Money(feeAmountTiyin, CurrencyCode.valueOf(currencyCode)),
                status, idempotencyKey, networkReference, appliedFeeRuleId, appliedRouteId,
                transferTypeId, purposeCodeId, channel, initiatedAt, completedAt);
    }

    public static TransferEntity fromDomain(Transfer t) {
        return new TransferEntity(t.id(),
                t.amount().amount(), t.amount().currency().name(),
                t.feeAmount().amount(),
                t.status(), t.idempotencyKey(), t.networkReference(),
                t.appliedFeeRuleId(), t.appliedRouteId(), t.transferTypeId(),
                t.purposeCodeId(), t.channel(), t.initiatedAt(), t.completedAt());
    }
}
