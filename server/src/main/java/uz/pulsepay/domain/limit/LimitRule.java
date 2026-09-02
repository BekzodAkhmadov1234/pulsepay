package uz.pulsepay.domain.limit;

import java.time.Instant;
import java.util.UUID;

public record LimitRule(
        UUID id,
        String name,
        String scope,
        String kycTier,
        Long maxAmount,
        Integer maxCount,
        String currencyCode,
        boolean isActive,
        Instant effectiveFrom,
        Instant effectiveTo,
        String limitCategory,
        String actorType,
        Integer transferTypeId,
        String networkCode
) {}
