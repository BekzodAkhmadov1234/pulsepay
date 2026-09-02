package uz.pulsepay.domain.merchant;

import java.util.UUID;

public record OnboardMerchantCommand(
        String legalTradeName,
        String mccCode,
        UUID acquiringBankId,
        String email,
        String passwordRaw
) {}
