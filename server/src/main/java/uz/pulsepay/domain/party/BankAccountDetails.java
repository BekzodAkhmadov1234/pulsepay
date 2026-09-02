package uz.pulsepay.domain.party;

import java.time.Instant;
import java.util.UUID;

public record BankAccountDetails(
        UUID id,
        UUID instrumentId,
        UUID bankId,
        String iban,
        String accountNumber,
        String accountHolderName,
        Instant createdAt
) {}
