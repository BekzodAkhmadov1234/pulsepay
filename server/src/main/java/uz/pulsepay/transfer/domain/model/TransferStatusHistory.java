package uz.pulsepay.transfer.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TransferStatusHistory(
        UUID id,
        UUID transferId,
        TransferStatus fromStatus,
        TransferStatus toStatus,
        String reason,
        Instant changedAt
) {}
