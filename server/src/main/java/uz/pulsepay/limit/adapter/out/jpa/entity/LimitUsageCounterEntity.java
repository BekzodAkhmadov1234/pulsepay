package uz.pulsepay.limit.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.limit.domain.model.LimitUsageCounter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "limit_usage_counters")
public class LimitUsageCounterEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "limit_rule_id", nullable = false)
    private UUID limitRuleId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "used_amount", nullable = false)
    private long usedAmount;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LimitUsageCounterEntity() {}

    public LimitUsageCounterEntity(UUID id, UUID userId, UUID limitRuleId,
                                    Instant periodStart, Instant periodEnd,
                                    long usedAmount, int usedCount, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.limitRuleId = limitRuleId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.usedAmount = usedAmount;
        this.usedCount = usedCount;
        this.updatedAt = updatedAt;
    }

    public LimitUsageCounter toDomain() {
        return new LimitUsageCounter(id, userId, limitRuleId, periodStart, periodEnd,
                usedAmount, usedCount, updatedAt);
    }
}
