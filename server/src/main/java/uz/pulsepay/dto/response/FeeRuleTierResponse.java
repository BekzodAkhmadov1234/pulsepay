package uz.pulsepay.dto.response;

import uz.pulsepay.domain.fee.FeeRuleTier;

import java.util.UUID;

public record FeeRuleTierResponse(
        UUID id,
        UUID feeRuleId,
        long tierMinAmount,
        Long tierMaxAmount,
        Long fixedAmount,
        Integer percentageBps
) {
    public static FeeRuleTierResponse from(FeeRuleTier tier) {
        return new FeeRuleTierResponse(tier.id(), tier.feeRuleId(),
                tier.tierMinAmount(), tier.tierMaxAmount(),
                tier.fixedAmount(), tier.percentageBps());
    }
}
