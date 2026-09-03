package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Request body to initiate a P2S (person-to-savings / Paynet utility) transfer")
public record InitiateP2STransferRequest(

        @Schema(description = "Sender's card instrument UUID")
        @NotNull UUID senderInstrumentId,

        @Schema(description = "Card network of the sender instrument", example = "uzcard")
        @NotBlank String senderCardNetwork,

        @Schema(description = "Paynet service code (from GET /api/v1/paynet/providers)", example = "gas-uzb")
        @NotBlank String serviceCode,

        @Schema(description = "Provider-specific field values (e.g. {account_number: '12345'})")
        @NotNull Map<@NotBlank String, @NotBlank String> serviceFields,

        @Schema(description = "Transfer amount in UZS", example = "50000.00")
        @NotNull @Positive BigDecimal amountUzs,

        @Schema(description = "Purpose code ID (optional)")
        Integer purposeCodeId,

        @Schema(description = "Originating channel", allowableValues = {"mobile_app", "pos", "e_pos", "api", "web"}, example = "mobile_app")
        String channel,

        @Schema(description = "Client-generated idempotency key (max 64 chars)")
        @NotBlank String idempotencyKey
) {}
