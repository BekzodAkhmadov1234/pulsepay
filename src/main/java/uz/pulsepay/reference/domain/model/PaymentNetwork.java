package uz.pulsepay.reference.domain.model;

import java.time.Instant;

public record PaymentNetwork(
        int id,
        String code,
        String legalName,
        String tin,
        String settlementSystem,
        boolean isActive,
        Instant createdAt
) {}
