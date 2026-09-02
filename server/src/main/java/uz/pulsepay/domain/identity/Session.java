package uz.pulsepay.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record Session(
        UUID id,
        UUID userId,
        UUID deviceId,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {
    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
