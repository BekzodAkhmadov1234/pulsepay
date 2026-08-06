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

    protected FeeRuleEntity() {}

    public FeeRule toDomain() {
        return new FeeRule(id, name, sourceNetwork, destinationNetwork, minAmount, maxAmount,
                feeType, fixedAmount, percentageBps, minFeeAmount, maxFeeAmount, currencyCode,
                priority, isActive, effectiveFrom, effectiveTo, transferTypeId, feePayer, feeRecipient);
    }
}
