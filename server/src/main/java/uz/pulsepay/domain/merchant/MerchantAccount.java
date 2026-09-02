package uz.pulsepay.domain.merchant;

import java.time.Instant;
import java.util.UUID;

public record MerchantAccount(
        UUID id,
        UUID merchantId,
        String currencyCode,
        long minPayoutThreshold,
        SettlementSchedule settlementSchedule,
        MerchantAccountStatus status,
        Instant createdAt
) {}
