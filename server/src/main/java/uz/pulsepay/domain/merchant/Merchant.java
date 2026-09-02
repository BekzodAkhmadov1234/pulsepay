package uz.pulsepay.domain.merchant;

import java.time.Instant;
import java.util.UUID;

public record Merchant(
        UUID id,
        String legalTradeName,
        Integer categoryId,
        UUID acquiringBankId,
        KybStatus kybStatus,
        MerchantStatus status,
        boolean uzqrEnabled,
        String email,
        String passwordHash,
        Instant createdAt
) {
    public boolean isActive() {
        return status == MerchantStatus.ACTIVE && kybStatus == KybStatus.VERIFIED;
    }
}
