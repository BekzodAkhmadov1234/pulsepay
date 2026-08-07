package uz.pulsepay.fee.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.fee.domain.model.FeePayer;
import uz.pulsepay.fee.domain.model.FeeRecipient;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_rules")
public class FeeRuleEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "source_network", length = 20)
    private String sourceNetwork;

    @Column(name = "destination_network", length = 20)
    private String destinationNetwork;

    @Column(name = "min_amount", nullable = false)
    private long minAmount;

    @Column(name = "max_amount")
    private Long maxAmount;

    @Convert(converter = FeeTypeConverter.class)
    @Column(name = "fee_type", nullable = false, length = 10)
    private FeeType feeType;

    @Column(name = "fixed_amount")
    private Long fixedAmount;

    @Column(name = "percentage_bps")
    private Integer percentageBps;

    @Column(name = "min_fee_amount")
    private Long minFeeAmount;

    @Column(name = "max_fee_amount")
    private Long maxFeeAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "transfer_type_id")
    private Integer transferTypeId;

    @Convert(converter = FeePayerConverter.class)
    @Column(name = "fee_payer", nullable = false, length = 10)
    private FeePayer feePayer;

    @Convert(converter = FeeRecipientConverter.class)
    @Column(name = "fee_recipient", nullable = false, length = 20)
    private FeeRecipient feeRecipient;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by_admin_id")
    private UUID createdByAdminId;

    @Column(name = "updated_by_admin_id")
    private UUID updatedByAdminId;

    protected FeeRuleEntity() {}

    FeeRuleEntity(UUID id, String name, String sourceNetwork, String destinationNetwork,
                  long minAmount, Long maxAmount, FeeType feeType, Long fixedAmount,
                  Integer percentageBps, Long minFeeAmount, Long maxFeeAmount,
                  String currencyCode, int priority, boolean isActive,
                  Instant effectiveFrom, Instant effectiveTo, Integer transferTypeId,
                  FeePayer feePayer, FeeRecipient feeRecipient,
                  Instant createdAt, Instant updatedAt,
                  UUID createdByAdminId, UUID updatedByAdminId) {
        this.id = id;
        this.name = name;
        this.sourceNetwork = sourceNetwork;
        this.destinationNetwork = destinationNetwork;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.feeType = feeType;
        this.fixedAmount = fixedAmount;
        this.percentageBps = percentageBps;
        this.minFeeAmount = minFeeAmount;
        this.maxFeeAmount = maxFeeAmount;
        this.currencyCode = currencyCode;
        this.priority = priority;
        this.isActive = isActive;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.transferTypeId = transferTypeId;
        this.feePayer = feePayer;
        this.feeRecipient = feeRecipient;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdByAdminId = createdByAdminId;
        this.updatedByAdminId = updatedByAdminId;
    }

    public FeeRule toDomain() {
        return new FeeRule(id, name, sourceNetwork, destinationNetwork, minAmount, maxAmount,
                feeType, fixedAmount, percentageBps, minFeeAmount, maxFeeAmount, currencyCode,
                priority, isActive, effectiveFrom, effectiveTo, transferTypeId, feePayer, feeRecipient,
                createdAt, createdByAdminId, updatedByAdminId);
    }

    public static FeeRuleEntity fromDomain(FeeRule r) {
        Instant now = Instant.now();
        return new FeeRuleEntity(
                r.id(), r.name(), r.sourceNetwork(), r.destinationNetwork(),
                r.minAmount(), r.maxAmount(), r.feeType(), r.fixedAmount(),
                r.percentageBps(), r.minFeeAmount(), r.maxFeeAmount(),
                r.currencyCode(), r.priority(), r.isActive(),
                r.effectiveFrom(), r.effectiveTo(), r.transferTypeId(),
                r.feePayer(), r.feeRecipient(),
                r.createdAt() != null ? r.createdAt() : now, now,
                r.createdByAdminId(), r.updatedByAdminId());
    }
}
