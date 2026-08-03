package uz.pulsepay.limit.adapter.out.jpa;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import uz.pulsepay.limit.adapter.out.jpa.entity.LimitUsageCounterEntity;
import uz.pulsepay.limit.domain.model.LimitUsageCounter;
import uz.pulsepay.limit.domain.port.out.LimitUsageCounterRepository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
class LimitUsageCounterJpaAdapter implements LimitUsageCounterRepository {

    private final LimitUsageCounterJpaRepository jpa;

    LimitUsageCounterJpaAdapter(LimitUsageCounterJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<LimitUsageCounter> findCurrentPeriod(UUID userId, UUID limitRuleId) {
        return jpa.findCurrentPeriod(userId, limitRuleId, Instant.now())
                .map(LimitUsageCounterEntity::toDomain);
    }

    @Override
    public void incrementUsage(UUID userId, UUID limitRuleId, long amountDelta) {
        jpa.incrementUsage(userId, limitRuleId, amountDelta, Instant.now());
    }

    @Override
    public LimitUsageCounter createForCurrentPeriod(UUID userId, UUID limitRuleId, String scope) {
        Instant now = Instant.now();
        Instant[] bounds = computePeriodBounds(scope, now);
        LimitUsageCounterEntity entity = new LimitUsageCounterEntity(
                UUID.randomUUID(), userId, limitRuleId, bounds[0], bounds[1], 0L, 0, now);
        try {
            return jpa.save(entity).toDomain();
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert on same UNIQUE(user_id, limit_rule_id, period_start) — counter already exists
            return jpa.findCurrentPeriod(userId, limitRuleId, now)
                    .map(LimitUsageCounterEntity::toDomain)
                    .orElseThrow(() -> new IllegalStateException("Counter disappeared after conflict", ex));
        }
    }

    private Instant[] computePeriodBounds(String scope, Instant now) {
        ZonedDateTime zdt = now.atZone(ZoneOffset.UTC);
        return switch (scope) {
            case "daily" -> new Instant[]{
                zdt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                zdt.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            };
            case "monthly" -> new Instant[]{
                zdt.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                zdt.withDayOfMonth(1).plusMonths(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
            };
            default -> throw new IllegalArgumentException("Unknown scope: " + scope);
        };
    }
}
