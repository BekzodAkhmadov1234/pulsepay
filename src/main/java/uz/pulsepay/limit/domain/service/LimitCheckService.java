package uz.pulsepay.limit.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.limit.domain.model.LimitRule;
import uz.pulsepay.limit.domain.model.LimitUsageCounter;
import uz.pulsepay.limit.domain.model.UserLimitOverride;
import uz.pulsepay.limit.domain.port.in.CheckLimitPort;
import uz.pulsepay.limit.domain.port.in.IncrementLimitUsagePort;
import uz.pulsepay.limit.domain.port.out.LimitRuleRepository;
import uz.pulsepay.limit.domain.port.out.LimitUsageCounterRepository;
import uz.pulsepay.limit.domain.port.out.UserLimitOverrideRepository;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LimitCheckService implements CheckLimitPort, IncrementLimitUsagePort {

    private final LimitRuleRepository limitRuleRepository;
    private final LimitUsageCounterRepository counterRepository;
    private final UserLimitOverrideRepository overrideRepository;

    public LimitCheckService(LimitRuleRepository limitRuleRepository,
                              LimitUsageCounterRepository counterRepository,
                              UserLimitOverrideRepository overrideRepository) {
        this.limitRuleRepository = limitRuleRepository;
        this.counterRepository = counterRepository;
        this.overrideRepository = overrideRepository;
    }

    @Override
    public void checkLimits(UUID userId, String kycLevel, Money amount, int transferTypeId) {
        List<LimitRule> rules = limitRuleRepository.findActiveRules(kycLevel, transferTypeId);

        for (LimitRule rule : rules) {
            // Override > specific > default resolution
            Optional<UserLimitOverride> override = overrideRepository.findActiveOverride(userId, rule.id());

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
                    LimitUsageCounter counter = counterRepository
                            .findCurrentPeriod(userId, rule.id())
                            .orElseGet(() -> counterRepository.createForCurrentPeriod(userId, rule.id(), rule.scope()));
                    Long ceiling = override.map(o -> o.overrideMaxAmount() != null
                            ? o.overrideMaxAmount() : rule.maxAmount())
                            .orElse(rule.maxAmount());
                    if (ceiling != null && (counter.usedAmount() + amount.amount()) > ceiling) {
                        throw new DomainException("Period amount limit exceeded for scope: " + rule.scope());
                    }
                }
                case "daily_count", "monthly_count" -> {
                    LimitUsageCounter counter = counterRepository
                            .findCurrentPeriod(userId, rule.id())
                            .orElseGet(() -> counterRepository.createForCurrentPeriod(userId, rule.id(), rule.scope()));
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

    @Override
    public void increment(UUID userId, Money amount, int transferTypeId) {
        List<LimitRule> rules = limitRuleRepository.findActiveRules(null, transferTypeId);
        for (LimitRule rule : rules) {
            counterRepository.incrementUsage(userId, rule.id(), amount.amount());
        }
    }
}
