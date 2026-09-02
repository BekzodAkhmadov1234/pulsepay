package uz.pulsepay.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record SecurityCooldown(
        UUID id,
        UUID userId,
        CooldownType cooldownType,
        Instant lockedUntil,
        String reason,
        Instant createdAt
) {
    public boolean isActive() {
        return lockedUntil.isAfter(Instant.now());
    }
}
