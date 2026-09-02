package uz.pulsepay.domain.shared;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyKey(
        String key,
        UUID userId,
        String requestHash,
        String responseSnapshot,
        Instant createdAt,
        Instant expiresAt
) {}
