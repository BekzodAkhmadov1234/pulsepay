package uz.pulsepay.domain.fee;

import java.time.Instant;
import java.util.UUID;

public record FeeRule(
        UUID id,
        String name,
        String mode,
        String sourceNetwork,
        String destinationNetwork,
        long minAmount,
        Long maxAmount,
        FeeType feeType,
        Long fixedAmount,
        Integer percentageBps,
        Long minFeeAmount,
        Long maxFeeAmount,
        String currencyCode,
        int priority,
        boolean isActive,
        Instant effectiveFrom,
        Instant effectiveTo,
        Integer transferTypeId,
        FeePayer feePayer,
        FeeRecipient feeRecipient,
        Instant createdAt,
        UUID createdByAdminId,
        UUID updatedByAdminId
) {}
