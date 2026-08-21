package uz.pulsepay.settlement.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementBatch(
        UUID id,
        BatchType batchType,
        UUID merchantAccountId,
        Integer paymentNetworkId,
        LocalDate operationalDate,
        long totalAmount,
        SettlementStatus status,
        Instant generatedAt,
        Instant settledAt
) {}
