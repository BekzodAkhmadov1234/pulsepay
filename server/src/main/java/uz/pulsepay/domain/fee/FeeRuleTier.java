package uz.pulsepay.domain.fee;

import java.util.UUID;

public record FeeRuleTier(
        UUID id,
        UUID feeRuleId,
        long tierMinAmount,
        Long tierMaxAmount,
        Long fixedAmount,
        Integer percentageBps
) {}
