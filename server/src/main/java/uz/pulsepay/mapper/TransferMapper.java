package uz.pulsepay.mapper;

import org.springframework.stereotype.Component;
import uz.pulsepay.dto.response.TransferResponse;
import uz.pulsepay.domain.transfer.Transfer;
import uz.pulsepay.domain.transfer.TransferSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TransferMapper {

    private static final BigDecimal TIYIN_PER_UZS = BigDecimal.valueOf(100);

    public TransferResponse toResponse(Transfer t) {
        return new TransferResponse(
                t.id(),
                BigDecimal.valueOf(t.amount().amount()).divide(TIYIN_PER_UZS),
                BigDecimal.valueOf(t.feeAmount().amount()).divide(TIYIN_PER_UZS),
                t.status().name().toLowerCase(),
                t.channel().name().toLowerCase(),
                t.idempotencyKey(),
                toIsoMs(t.initiatedAt()),
                toIsoMs(t.completedAt()),
                null, null, null, null, null, null, t.transferTypeId()
        );
    }

    public TransferResponse toResponse(TransferSummary s) {
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
                s.direction(),
                s.transferTypeId()
        );
    }

    private static String toIsoMs(Instant instant) {
        if (instant == null) return null;
        return instant.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
