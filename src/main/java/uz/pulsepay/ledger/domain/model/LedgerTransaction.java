package uz.pulsepay.ledger.domain.model;

import java.time.Instant;
import java.util.UUID;

public record LedgerTransaction(
        UUID id,
        UUID transferId,
        String status,
        String externalId,
        int entryTypeId,
        Instant effectiveAt,
        Instant postedAt,
        UUID reversesTxnId
) {}
