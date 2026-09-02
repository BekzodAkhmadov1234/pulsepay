package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "limit_usage_counters")
public class LimitUsageCounter {

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

    protected LimitUsageCounter() {}

    public LimitUsageCounter(UUID id, UUID userId, UUID limitRuleId,
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

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getLimitRuleId() { return limitRuleId; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public long getUsedAmount() { return usedAmount; }
    public int getUsedCount() { return usedCount; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setUsedAmount(long usedAmount) { this.usedAmount = usedAmount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
