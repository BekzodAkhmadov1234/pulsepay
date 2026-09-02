package uz.pulsepay.dto.response;

import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.transfer.TransferChannel;
import uz.pulsepay.domain.transfer.TransferStatus;

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
