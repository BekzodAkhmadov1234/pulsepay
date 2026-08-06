package uz.pulsepay.card.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.pulsepay.card.domain.model.Card;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card bound to the authenticated user's account")
public record CardResponse(

        @Schema(description = "Card UUID") UUID id,
        @Schema(description = "Masked PAN, e.g. '8600 12** **** 3456'") String maskedPan,
        @Schema(description = "'uzcard' or 'humo'") String cardNetwork,
        @Schema(description = "Cardholder name") String cardHolderName,
        @Schema(description = "Expiry month (1–12)") short expMonth,
        @Schema(description = "Expiry year (4-digit)") short expYear,
        @Schema(description = "UNVERIFIED | VERIFIED | INACTIVE | EXPIRED | BLOCKED") String status,
        @Schema(description = "True if this is the user's default card") boolean isDefault,
        @Schema(description = "Timestamp when the card was verified; null if UNVERIFIED") Instant verifiedAt,
        @Schema(description = "Available balance in UZS (stub shadow balance)") BigDecimal balanceUzs

) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.id(),
                card.maskedPan(),
                card.cardNetwork(),
                card.cardHolderName(),
                card.expMonth(),
                card.expYear(),
                card.status().name(),
                card.isDefault(),
                card.verifiedAt(),
                BigDecimal.valueOf(card.balanceTiyin()).divide(BigDecimal.valueOf(100))
        );
    }
}
