package uz.pulsepay.domain.transfer;

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
