package uz.pulsepay.domain.transfer;

import java.time.Instant;
import java.util.UUID;

public record P2STransferDetails(
        UUID id,
        UUID transferId,
        String serviceCode,
        String serviceFields,
        Instant createdAt
) {}
