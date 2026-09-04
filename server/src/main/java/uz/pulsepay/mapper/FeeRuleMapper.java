package uz.pulsepay.mapper;

import org.springframework.stereotype.Component;
import uz.pulsepay.dto.response.FeeRuleResponse;
import uz.pulsepay.dto.response.FeeRuleTierResponse;
import uz.pulsepay.domain.fee.FeeRule;
import uz.pulsepay.domain.fee.FeeRuleTier;

import java.util.List;

@Component
public class FeeRuleMapper {

    public FeeRuleResponse toResponse(FeeRule rule, List<FeeRuleTierResponse> tiers) {
        return new FeeRuleResponse(
                rule.id(), rule.name(), rule.mode(),
                rule.sourceNetwork(), rule.destinationNetwork(),
                rule.minAmount(), rule.maxAmount(),
                rule.feeType(), rule.fixedAmount(), rule.percentageBps(),
                rule.minFeeAmount(), rule.maxFeeAmount(),
                rule.currencyCode(), rule.priority(), rule.isActive(),
                rule.effectiveFrom(), rule.effectiveTo(),
                rule.transferTypeId(), rule.feePayer(), rule.feeRecipient(),
                rule.createdAt(), rule.createdByAdminId(),
                tiers
        );
    }

    public FeeRuleTierResponse toTierResponse(FeeRuleTier tier) {
        return new FeeRuleTierResponse(
                tier.id(), tier.feeRuleId(),
                tier.tierMinAmount(), tier.tierMaxAmount(),
                tier.fixedAmount(), tier.percentageBps()
        );
    }
}
