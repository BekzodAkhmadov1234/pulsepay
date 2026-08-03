package uz.pulsepay.reference.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Bank(
        UUID id,
        String mfoCode,
        String name,
        String tin,
        boolean roleIssuer,
        boolean roleAcquirer,
        boolean roleSettlement,
        boolean isActive,
        Instant createdAt
) {}
