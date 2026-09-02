package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request body to initiate a P2A (person-to-account) bank transfer")
public record InitiateBankTransferRequest(
        @Schema(description = "Sender's card instrument UUID") @NotNull UUID senderInstrumentId,
        @Schema(description = "Card network of the sender instrument", example = "uzcard") @NotBlank String senderCardNetwork,
        @Schema(description = "Recipient IBAN (Uzbekistan format: UZ + 25 digits)", example = "UZ123456789012345678901234") @NotBlank String recipientIban,
        @Schema(description = "Recipient's bank UUID (from GET /api/v1/banks)") @NotNull UUID recipientBankId,
        @Schema(description = "Full name of the bank account holder") @NotBlank String recipientAccountHolderName,
        @Schema(description = "Transfer amount in UZS", example = "500000.00") @NotNull @Positive BigDecimal amountUzs,
        @Schema(description = "Purpose code ID (optional)") Integer purposeCodeId,
        @Schema(description = "Originating channel", allowableValues = {"mobile_app", "pos", "e_pos", "api", "web"}, example = "mobile_app") String channel,
        @Schema(description = "Client-generated idempotency key (max 64 chars)") @NotBlank String idempotencyKey
) {}
