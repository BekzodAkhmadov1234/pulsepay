package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request body to initiate a new transfer")
public record InitiateTransferRequest(
        @Schema(description = "Sender's card/instrument UUID") @NotNull UUID senderInstrumentId,
        @Schema(description = "Card network of the sender instrument", example = "UZCARD") @NotNull String senderCardNetwork,
        @Schema(description = "Recipient's party UUID") @NotNull UUID recipientId,
        @Schema(description = "Recipient's card/instrument UUID") @NotNull UUID recipientInstrumentId,
        @Schema(description = "Card network of the recipient instrument", example = "HUMO") @NotNull String recipientCardNetwork,
        @Schema(description = "Transfer amount in UZS (converted to tiyin internally)", example = "50000.00") @NotNull @Positive BigDecimal amountUzs,
        @Schema(description = "Transfer type ID (1 = p2p)", example = "1") @NotNull Integer transferTypeId,
        @Schema(description = "Purpose code ID (required for P2P regulatory compliance)", example = "1") Integer purposeCodeId,
        @Schema(description = "Originating channel", allowableValues = {"mobile_app", "pos", "e_pos", "api", "web"}, example = "mobile_app") String channel,
        @Schema(description = "Client-generated idempotency key (max 64 chars) — re-submitting the same key returns the cached response") @NotNull String idempotencyKey
) {}
