package uz.pulsepay.ledger.domain.model;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        UUID ledgerTransactionId,
        UUID ledgerAccountId,
        EntryDirection direction,
        long amount,
        String currencyCode,
        String status,
        Instant createdAt
) {}
