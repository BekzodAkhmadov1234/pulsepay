package uz.pulsepay.fee.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.fee.domain.model.FeeRuleTier;

import java.util.UUID;

@Entity
@Table(name = "fee_rule_tiers")
public class FeeRuleTierEntity {

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

    protected FeeRuleTierEntity() {}

    FeeRuleTierEntity(UUID id, UUID feeRuleId, long tierMinAmount, Long tierMaxAmount,
                      Long fixedAmount, Integer percentageBps) {
        this.id = id;
        this.feeRuleId = feeRuleId;
        this.tierMinAmount = tierMinAmount;
        this.tierMaxAmount = tierMaxAmount;
        this.fixedAmount = fixedAmount;
        this.percentageBps = percentageBps;
    }

    public FeeRuleTier toDomain() {
        return new FeeRuleTier(id, feeRuleId, tierMinAmount, tierMaxAmount, fixedAmount, percentageBps);
    }

    public static FeeRuleTierEntity fromDomain(FeeRuleTier t) {
        return new FeeRuleTierEntity(t.id(), t.feeRuleId(), t.tierMinAmount(),
                t.tierMaxAmount(), t.fixedAmount(), t.percentageBps());
    }
}
