package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.domain.limit.LimitRule;
import uz.pulsepay.domain.limit.LimitUsageCounter;
import uz.pulsepay.domain.limit.UserLimitOverride;
import uz.pulsepay.repository.LimitRuleRepository;
import uz.pulsepay.repository.LimitUsageCounterRepository;
import uz.pulsepay.repository.UserLimitOverrideRepository;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LimitService {

    private final LimitRuleRepository limitRuleRepository;
    private final LimitUsageCounterRepository counterRepository;
    private final UserLimitOverrideRepository overrideRepository;

    public LimitService(LimitRuleRepository limitRuleRepository,
                        LimitUsageCounterRepository counterRepository,
                        UserLimitOverrideRepository overrideRepository) {
        this.limitRuleRepository = limitRuleRepository;
        this.counterRepository = counterRepository;
        this.overrideRepository = overrideRepository;
    }

    public void checkLimits(UUID userId, String kycLevel, Money amount, int transferTypeId) {
        Instant now = Instant.now();
        List<LimitRule> rules = limitRuleRepository.findActiveRules(kycLevel, transferTypeId, now)
                .stream().map(e -> e.toDomain()).toList();

        for (LimitRule rule : rules) {
            // Override > specific > default resolution
            Optional<UserLimitOverride> override = overrideRepository
                    .findActiveOverride(userId, rule.id(), now)
                    .map(e -> e.toDomain());

            switch (rule.scope()) {
                case "per_transaction" -> {
                    Long ceiling = override.map(o -> o.overrideMaxAmount() != null
                            ? o.overrideMaxAmount() : rule.maxAmount())
                            .orElse(rule.maxAmount());
                    if (ceiling != null && amount.amount() > ceiling) {
                        throw new DomainException("Per-transaction limit exceeded: max " + ceiling + " tiyin");
                    }
                }
                case "daily_amount", "monthly_amount" -> {
                    LimitUsageCounter counter = getCurrentOrCreateCounter(userId, rule, now);
                    Long ceiling = override.map(o -> o.overrideMaxAmount() != null
                            ? o.overrideMaxAmount() : rule.maxAmount())
                            .orElse(rule.maxAmount());
                    if (ceiling != null && (counter.usedAmount() + amount.amount()) > ceiling) {
                        throw new DomainException("Period amount limit exceeded for scope: " + rule.scope());
                    }
                }
                case "daily_count", "monthly_count" -> {
                    LimitUsageCounter counter = getCurrentOrCreateCounter(userId, rule, now);
                    Integer ceiling = override.map(o -> o.overrideMaxCount() != null
                            ? o.overrideMaxCount() : rule.maxCount())
                            .orElse(rule.maxCount());
                    if (ceiling != null && (counter.usedCount() + 1) > ceiling) {
                        throw new DomainException("Transfer count limit exceeded for scope: " + rule.scope());
                    }
                }
            }
        }
    }

    public void increment(UUID userId, Money amount, int transferTypeId) {
        Instant now = Instant.now();
        List<LimitRule> rules = limitRuleRepository.findActiveRules(null, transferTypeId, now)
                .stream().map(e -> e.toDomain()).toList();
        for (LimitRule rule : rules) {
            counterRepository.incrementUsage(userId, rule.id(), amount.amount(), now);
        }
    }

    private LimitUsageCounter getCurrentOrCreateCounter(UUID userId, LimitRule rule, Instant now) {
        return counterRepository.findCurrentPeriod(userId, rule.id(), now)
                .map(e -> e.toDomain())
                .orElseGet(() -> {
                    Instant[] period = computePeriod(rule.scope(), now);
                    var entity = new uz.pulsepay.domain.limit.LimitUsageCounterEntity(
                            UUID.randomUUID(), userId, rule.id(),
                            period[0], period[1], 0L, 0, now);
                    return counterRepository.save(entity).toDomain();
                });
    }

    private Instant[] computePeriod(String scope, Instant now) {
        ZonedDateTime zdt = now.atZone(ZoneOffset.UTC);
        Instant start;
        Instant end;
        if (scope.startsWith("daily")) {
            start = zdt.truncatedTo(ChronoUnit.DAYS).toInstant();
            end   = start.plus(1, ChronoUnit.DAYS);
        } else {
            // monthly
            start = zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
            end   = zdt.withDayOfMonth(1).plusMonths(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        }
        return new Instant[]{start, end};
    }
}
