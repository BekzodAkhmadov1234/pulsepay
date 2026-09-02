package uz.pulsepay.domain.compliance;

import java.time.Instant;
import java.util.UUID;

public record RegulatoryParameter(
        UUID id,
        String code,
        long valueAmount,
        String unit,
        String currencyCode,
        Instant effectiveFrom,
        Instant effectiveTo,
        String sourceReference,
        Instant createdAt
) {}
