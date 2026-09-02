package uz.pulsepay.domain.limit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.limit.UserLimitOverride;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_limit_overrides")
public class UserLimitOverrideEntity {

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

    protected UserLimitOverrideEntity() {}

    public UserLimitOverride toDomain() {
        return new UserLimitOverride(id, userId, limitRuleId, overrideMaxAmount, overrideMaxCount,
                reason, createdByAdminId, expiresAt, createdAt);
    }
}
