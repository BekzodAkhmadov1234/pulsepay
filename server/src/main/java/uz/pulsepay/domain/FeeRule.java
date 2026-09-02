package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.FeePayerConverter;
import uz.pulsepay.domain.converter.FeeRecipientConverter;
import uz.pulsepay.domain.converter.FeeTypeConverter;
import uz.pulsepay.domain.enums.FeePayer;
import uz.pulsepay.domain.enums.FeeRecipient;
import uz.pulsepay.domain.enums.FeeType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_rules")
public class FeeRule {

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

    protected FeeRule() {}

    public FeeRule(UUID id, String name, String sourceNetwork, String destinationNetwork,
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

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSourceNetwork() { return sourceNetwork; }
    public String getDestinationNetwork() { return destinationNetwork; }
    public long getMinAmount() { return minAmount; }
    public Long getMaxAmount() { return maxAmount; }
    public FeeType getFeeType() { return feeType; }
    public Long getFixedAmount() { return fixedAmount; }
    public Integer getPercentageBps() { return percentageBps; }
    public Long getMinFeeAmount() { return minFeeAmount; }
    public Long getMaxFeeAmount() { return maxFeeAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public int getPriority() { return priority; }
    public boolean isActive() { return isActive; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public Integer getTransferTypeId() { return transferTypeId; }
    public FeePayer getFeePayer() { return feePayer; }
    public FeeRecipient getFeeRecipient() { return feeRecipient; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedByAdminId() { return createdByAdminId; }
    public UUID getUpdatedByAdminId() { return updatedByAdminId; }

    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setUpdatedByAdminId(UUID updatedByAdminId) { this.updatedByAdminId = updatedByAdminId; }
    public void setName(String name) { this.name = name; }
    public void setEffectiveTo(Instant effectiveTo) { this.effectiveTo = effectiveTo; }
}
