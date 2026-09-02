package uz.pulsepay.domain.merchant;

public record MerchantCategory(
        int id,
        String mccCode,
        String nameUz,
        String riskTier
) {}
