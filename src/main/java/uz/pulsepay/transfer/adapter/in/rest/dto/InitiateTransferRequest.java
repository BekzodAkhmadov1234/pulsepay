package uz.pulsepay.transfer.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateTransferRequest(
        @NotNull UUID senderInstrumentId,
        @NotNull String senderCardNetwork,
        @NotNull UUID recipientId,
        @NotNull UUID recipientInstrumentId,
        @NotNull String recipientCardNetwork,
        @NotNull @Positive BigDecimal amountUzs,
        @NotNull Integer transferTypeId,
        Integer purposeCodeId,
        String channel,
        @NotNull String idempotencyKey
) {}
