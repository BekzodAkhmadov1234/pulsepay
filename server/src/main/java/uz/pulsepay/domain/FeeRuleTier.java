package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "fee_rule_tiers")
public class FeeRuleTier {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "fee_rule_id", nullable = false)
    private UUID feeRuleId;

    @Column(name = "tier_min_amount", nullable = false)
    private long tierMinAmount;

    @Column(name = "tier_max_amount")
    private Long tierMaxAmount;

    @Column(name = "fixed_amount")
    private Long fixedAmount;

    @Column(name = "percentage_bps")
    private Integer percentageBps;

    protected FeeRuleTier() {}

    public FeeRuleTier(UUID id, UUID feeRuleId, long tierMinAmount, Long tierMaxAmount,
                        Long fixedAmount, Integer percentageBps) {
        this.id = id;
        this.feeRuleId = feeRuleId;
        this.tierMinAmount = tierMinAmount;
        this.tierMaxAmount = tierMaxAmount;
        this.fixedAmount = fixedAmount;
        this.percentageBps = percentageBps;
    }

    public UUID getId() { return id; }
    public UUID getFeeRuleId() { return feeRuleId; }
    public long getTierMinAmount() { return tierMinAmount; }
    public Long getTierMaxAmount() { return tierMaxAmount; }
    public Long getFixedAmount() { return fixedAmount; }
    public Integer getPercentageBps() { return percentageBps; }
}
