package uz.pulsepay.card.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for binding a UzCard or HUMO card to the authenticated user's account")
public record AddCardRequest(

        @Schema(description = "Masked card PAN. Network auto-detected from BIN: UzCard = 8600 / 5614 / 6262, HUMO = 9860.", example = "860012******3456")
        @NotBlank(message = "maskedPan must not be blank")
        String maskedPan,

        @Schema(description = "Card token returned by the PSP card-registration step (raw PAN accepted in dev)", example = "tok_uzcard_abc123")
        @NotBlank(message = "cardToken must not be blank")
        String cardToken,

        @Schema(description = "Cardholder name as printed on the card", example = "ALISHER KARIMOV")
        @NotBlank(message = "cardHolderName must not be blank")
        @Size(min = 2, max = 80, message = "cardHolderName must be between 2 and 80 characters")
        String cardHolderName,

        @Schema(description = "Expiry month (1–12)", example = "12")
        @Min(value = 1, message = "expMonth must be between 1 and 12")
        @Max(value = 12, message = "expMonth must be between 1 and 12")
        short expMonth,

        @Schema(description = "Expiry year (4-digit)", example = "2028")
        @Min(value = 2024, message = "expYear must be 2024 or later")
        @Max(value = 2040, message = "expYear must be 2040 or earlier")
        short expYear

) {}
