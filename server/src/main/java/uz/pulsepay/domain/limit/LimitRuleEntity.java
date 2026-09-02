package uz.pulsepay.domain.limit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.limit.LimitRule;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "limit_rules")
public class LimitRuleEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "kyc_tier", nullable = false, length = 10)
    private String kycTier;

    @Column(name = "max_amount")
    private Long maxAmount;

    @Column(name = "max_count")
    private Integer maxCount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "limit_category", length = 20)
    private String limitCategory;

    @Column(name = "actor_type", length = 10)
    private String actorType;

    @Column(name = "transfer_type_id")
    private Integer transferTypeId;

    @Column(name = "network_code", length = 20)
    private String networkCode;

    protected LimitRuleEntity() {}

    public UUID getId() { return id; }
    public String getScope() { return scope; }

    public LimitRule toDomain() {
        return new LimitRule(id, name, scope, kycTier, maxAmount, maxCount, currencyCode,
                isActive, effectiveFrom, effectiveTo, limitCategory, actorType, transferTypeId, networkCode);
    }
}
