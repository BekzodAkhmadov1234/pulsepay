package uz.pulsepay.transfer.adapter.in.rest.dto;

import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.model.TransferSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        BigDecimal amountUzs,
        BigDecimal feeAmountUzs,
        String status,
        String channel,
        String idempotencyKey,
        String initiatedAt,
        String completedAt,
        String senderName,
        String senderMaskedPan,
        String recipientName,
        String recipientMaskedPan,
        String processedAt,
        String direction
) {
    private static final BigDecimal TIYIN_PER_UZS = BigDecimal.valueOf(100);

    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.id(),
                BigDecimal.valueOf(t.amount().amount()).divide(TIYIN_PER_UZS),
                BigDecimal.valueOf(t.feeAmount().amount()).divide(TIYIN_PER_UZS),
                t.status().name().toLowerCase(),
                t.channel().name().toLowerCase(),
                t.idempotencyKey(),
                toIsoMs(t.initiatedAt()),
                toIsoMs(t.completedAt()),
                null, null, null, null, null, null
        );
    }

    public static TransferResponse from(TransferSummary s) {
        return new TransferResponse(
                s.id(),
                BigDecimal.valueOf(s.amount().amount()).divide(TIYIN_PER_UZS),
                BigDecimal.valueOf(s.feeAmount().amount()).divide(TIYIN_PER_UZS),
                s.status().name().toLowerCase(),
                s.channel().name().toLowerCase(),
                s.idempotencyKey(),
                s.initiatedAt(),
                s.completedAt(),
                s.senderName(),
                s.senderMaskedPan(),
                s.recipientName(),
                s.recipientMaskedPan(),
                s.processedAt(),
                s.direction()
        );
    }

    private static String toIsoMs(Instant instant) {
        if (instant == null) return null;
        return instant.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
