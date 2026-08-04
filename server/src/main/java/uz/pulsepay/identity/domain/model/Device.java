package uz.pulsepay.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Device(
        UUID id,
        UUID userId,
        String deviceFingerprint,
        String platform,
        String pushToken,
        Instant firstSeenAt,
        Instant lastSeenAt,
        boolean trusted
) {}
