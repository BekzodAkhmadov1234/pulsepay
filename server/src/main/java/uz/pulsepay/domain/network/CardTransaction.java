package uz.pulsepay.domain.network;

import java.time.Instant;
import java.util.UUID;

public record CardTransaction(
        UUID id,
        UUID transferId,
        int transactionTypeId,
        UUID cardId,
        long amount,
        String networkStatusCode,
        Integer failureReasonId,
        String status,
        Instant processedAt,
        Integer paymentNetworkId
) {}
