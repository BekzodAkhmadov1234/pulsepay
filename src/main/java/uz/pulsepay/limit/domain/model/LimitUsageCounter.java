package uz.pulsepay.limit.domain.model;

import java.time.Instant;
import java.util.UUID;

public record LimitUsageCounter(
        UUID id,
        UUID userId,
        UUID limitRuleId,
        Instant periodStart,
        Instant periodEnd,
        long usedAmount,
        int usedCount,
        Instant updatedAt
) {}
