package uz.pulsepay.fee.adapter.in.rest.dto;

import uz.pulsepay.fee.domain.model.FeePayer;
import uz.pulsepay.fee.domain.model.FeeRecipient;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeeRuleResponse(
        UUID id,
        String name,
        String sourceNetwork,
        String destinationNetwork,
        long minAmount,
        Long maxAmount,
        FeeType feeType,
        Long fixedAmount,
        Integer percentageBps,
        Long minFeeAmount,
        Long maxFeeAmount,
        String currencyCode,
        int priority,
        boolean isActive,
        Instant effectiveFrom,
        Instant effectiveTo,
        Integer transferTypeId,
        FeePayer feePayer,
        FeeRecipient feeRecipient,
        Instant createdAt,
        UUID createdByAdminId,
        List<FeeRuleTierResponse> tiers
) {
    public static FeeRuleResponse from(FeeRule rule, List<FeeRuleTierResponse> tiers) {
        return new FeeRuleResponse(
                rule.id(), rule.name(),
                rule.sourceNetwork(), rule.destinationNetwork(),
                rule.minAmount(), rule.maxAmount(),
                rule.feeType(), rule.fixedAmount(), rule.percentageBps(),
                rule.minFeeAmount(), rule.maxFeeAmount(),
                rule.currencyCode(), rule.priority(), rule.isActive(),
                rule.effectiveFrom(), rule.effectiveTo(),
                rule.transferTypeId(), rule.feePayer(), rule.feeRecipient(),
                rule.createdAt(), rule.createdByAdminId(),
                tiers);
    }
}
