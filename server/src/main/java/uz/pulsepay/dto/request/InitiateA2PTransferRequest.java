package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request body to initiate an A2P (account-to-person / bank pull) transfer")
public record InitiateA2PTransferRequest(

        @Schema(description = "Source IBAN to pull funds from", example = "UZ123456789012345678901234")
        @NotBlank String sourceIban,

        @Schema(description = "Source bank UUID (from GET /api/v1/banks)")
        @NotNull UUID sourceBankId,

        @Schema(description = "Full name of the bank account holder (for verification)")
        @NotBlank String sourceAccountHolderName,

        @Schema(description = "Destination card instrument UUID (user's own card to credit)")
        @NotNull UUID destinationInstrumentId,

        @Schema(description = "Card network of the destination instrument", example = "uzcard")
        @NotBlank String destinationCardNetwork,

        @Schema(description = "Amount to pull in UZS", example = "1000000.00")
        @NotNull @Positive BigDecimal amountUzs,

        @Schema(description = "Purpose code ID (optional)")
        Integer purposeCodeId,

        @Schema(description = "Originating channel", example = "mobile_app")
        String channel,

        @Schema(description = "Client-generated idempotency key (max 64 chars)")
        @NotBlank String idempotencyKey
) {}
