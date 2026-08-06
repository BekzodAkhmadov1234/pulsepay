package uz.pulsepay.transfer.domain.model;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

public record TransferSummary(
        UUID id,
        Money amount,
        Money feeAmount,
        TransferStatus status,
        TransferChannel channel,
        String idempotencyKey,
        String initiatedAt,
        String completedAt,
        String senderName,
        String senderMaskedPan,
        String recipientName,
        String recipientMaskedPan,
        String processedAt,
        String direction
) {}
