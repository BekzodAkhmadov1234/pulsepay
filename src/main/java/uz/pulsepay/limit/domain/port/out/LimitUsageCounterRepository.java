package uz.pulsepay.limit.domain.port.out;

import uz.pulsepay.limit.domain.model.LimitUsageCounter;

import java.util.Optional;
import java.util.UUID;

public interface LimitUsageCounterRepository {
    Optional<LimitUsageCounter> findCurrentPeriod(UUID userId, UUID limitRuleId);

    /**
     * Atomic SQL UPDATE to avoid hot-row contention (Risk #2).
     */
    void incrementUsage(UUID userId, UUID limitRuleId, long amountDelta);

    LimitUsageCounter createForCurrentPeriod(UUID userId, UUID limitRuleId, String scope);
}
