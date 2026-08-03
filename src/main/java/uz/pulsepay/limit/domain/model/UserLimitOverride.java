package uz.pulsepay.limit.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserLimitOverride(
        UUID id,
        UUID userId,
        UUID limitRuleId,
        Long overrideMaxAmount,
        Integer overrideMaxCount,
        String reason,
        UUID createdByAdminId,
        Instant expiresAt,
        Instant createdAt
) {
    public boolean isActive() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
