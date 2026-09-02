package uz.pulsepay.domain.transfer;

import uz.pulsepay.domain.shared.Money;

import java.time.Instant;
import java.util.UUID;

public record Transfer(
        UUID id,
        Money amount,
        Money feeAmount,
        TransferStatus status,
        String idempotencyKey,
        String networkReference,
        UUID appliedFeeRuleId,
        UUID appliedRouteId,
        int transferTypeId,
        Integer purposeCodeId,
        TransferChannel channel,
        Instant initiatedAt,
        Instant completedAt
) {}
