package uz.pulsepay.merchant.domain.model;

public record MerchantCategory(
        int id,
        String mccCode,
        String nameUz,
        String riskTier
) {}
