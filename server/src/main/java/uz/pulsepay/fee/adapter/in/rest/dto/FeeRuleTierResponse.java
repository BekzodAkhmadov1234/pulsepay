package uz.pulsepay.fee.adapter.in.rest.dto;

import uz.pulsepay.fee.domain.model.FeeRuleTier;

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
