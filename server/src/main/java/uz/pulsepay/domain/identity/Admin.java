package uz.pulsepay.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record Admin(
        UUID id,
        String fullName,
        String email,
        String passwordHash,
        AdminRole role,
        String status,
        Instant createdAt,
        Instant lastLoginAt
) {
    public boolean isActive() {
        return "active".equals(status);
    }
}
