package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_limit_overrides")
public class UserLimitOverride {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "limit_rule_id", nullable = false)
    private UUID limitRuleId;

    @Column(name = "override_max_amount")
    private Long overrideMaxAmount;

    @Column(name = "override_max_count")
    private Integer overrideMaxCount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_by_admin_id")
    private UUID createdByAdminId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserLimitOverride() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getLimitRuleId() { return limitRuleId; }
    public Long getOverrideMaxAmount() { return overrideMaxAmount; }
    public Integer getOverrideMaxCount() { return overrideMaxCount; }
    public String getReason() { return reason; }
    public UUID getCreatedByAdminId() { return createdByAdminId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
