package uz.pulsepay.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String phoneE164,
        String fullName,
        String status,
        String kycLevel,
        Instant biometricVerifiedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt,
        int version
) {
    public boolean isActive() {
        return "active".equals(status) && closedAt == null;
    }
}
