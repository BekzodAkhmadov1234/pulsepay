package uz.pulsepay.fee.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.port.in.CalculateFeePort;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FeeCalculationService implements CalculateFeePort {

    private final FeeRuleRepository feeRuleRepository;

    public FeeCalculationService(FeeRuleRepository feeRuleRepository) {
        this.feeRuleRepository = feeRuleRepository;
    }

    @Override
    public Optional<FeeResult> calculate(Money amount, int transferTypeId,
                                         String sourceNetwork, String destNetwork) {
        List<FeeRule> rules = feeRuleRepository.findApplicableRules(
                transferTypeId, sourceNetwork, destNetwork, amount.amount());

        return rules.stream()
                .sorted(Comparator.comparingInt(FeeRule::priority))
                .findFirst()
                .map(rule -> {
                    long fee = computeFee(rule, amount.amount());
                    return new FeeResult(Money.ofTiyin(fee, CurrencyCode.UZS), rule);
                });
    }

    private long computeFee(FeeRule rule, long amount) {
        return switch (rule.feeType()) {
            case FIXED -> rule.fixedAmount() != null ? rule.fixedAmount() : 0L;
            case PERCENTAGE -> {
                long raw = amount * rule.percentageBps() / 10_000;
                long floored = rule.minFeeAmount() != null ? Math.max(raw, rule.minFeeAmount()) : raw;
                yield rule.maxFeeAmount() != null ? Math.min(floored, rule.maxFeeAmount()) : floored;
            }
            case TIERED -> 0L; // tiered: delegated to FeeRuleTiers (extend when needed)
        };
    }
}
