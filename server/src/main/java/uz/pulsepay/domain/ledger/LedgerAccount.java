package uz.pulsepay.domain.ledger;

import java.time.Instant;
import java.util.UUID;

public record LedgerAccount(
        UUID id,
        int accountTypeId,
        String normalBalance,
        String code,
        String currencyCode,
        long postedBalance,
        long lockVersion,
        String status,
        Instant createdAt
) {}
