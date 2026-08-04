package uz.pulsepay.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        UUID sessionId,
        Instant expiresAt,
        Instant revokedAt
) {
    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
