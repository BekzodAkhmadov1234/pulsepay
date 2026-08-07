package uz.pulsepay.fee.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeRuleTier;
import uz.pulsepay.fee.domain.port.in.CalculateFeePort;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;
import uz.pulsepay.fee.domain.port.out.FeeRuleTierRepository;
import uz.pulsepay.shared.domain.CurrencyCode;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FeeCalculationService implements CalculateFeePort {

    private final FeeRuleRepository feeRuleRepository;
    private final FeeRuleTierRepository feeRuleTierRepository;

    public FeeCalculationService(FeeRuleRepository feeRuleRepository,
                                  FeeRuleTierRepository feeRuleTierRepository) {
        this.feeRuleRepository = feeRuleRepository;
        this.feeRuleTierRepository = feeRuleTierRepository;
    }

    @Override
    public Optional<FeeResult> calculate(Money amount, int transferTypeId,
                                         String sourceNetwork, String destNetwork,
                                         String currencyCode, Instant occurringAt) {
        List<FeeRule> rules = feeRuleRepository.findApplicableRules(
                transferTypeId, sourceNetwork, destNetwork,
                amount.amount(), currencyCode, occurringAt);

        Optional<FeeRule> winner = rules.stream()
                .sorted(Comparator.comparingInt(FeeRule::priority)
                        .thenComparingInt(r -> -specificityScore(r))
                        .thenComparing(FeeRule::createdAt, Comparator.reverseOrder()))
                .findFirst();

        if (winner.isEmpty()) {
            log.warn("No fee rule matched: transferTypeId={}, sourceNetwork={}, destNetwork={}, " +
                     "amount={}, currencyCode={}", transferTypeId, sourceNetwork, destNetwork,
                     amount.amount(), currencyCode);
            return Optional.empty();
        }

        FeeRule rule = winner.get();
        long fee = computeFee(rule, amount.amount());
        return Optional.of(new FeeResult(Money.ofTiyin(fee, CurrencyCode.UZS), rule));
    }

    private int specificityScore(FeeRule rule) {
        return (rule.transferTypeId() != null ? 4 : 0)
             + (rule.sourceNetwork()  != null ? 2 : 0)
             + (rule.destinationNetwork() != null ? 1 : 0);
    }

    private long computeFee(FeeRule rule, long amount) {
        return switch (rule.feeType()) {
            case FIXED -> rule.fixedAmount() != null ? rule.fixedAmount() : 0L;
            case PERCENTAGE -> computePercentage(rule, amount);
            case TIERED -> computeTiered(rule, amount);
        };
    }

    private long computePercentage(FeeRule rule, long amount) {
        long raw = (amount * rule.percentageBps() + 5_000L) / 10_000L;
        long floored = rule.minFeeAmount() != null ? Math.max(raw, rule.minFeeAmount()) : raw;
        return rule.maxFeeAmount() != null ? Math.min(floored, rule.maxFeeAmount()) : floored;
    }

    private long computeTiered(FeeRule rule, long amount) {
        List<FeeRuleTier> tiers = feeRuleTierRepository.findByFeeRuleId(rule.id());
        FeeRuleTier tier = tiers.stream()
                .filter(t -> t.tierMinAmount() <= amount
                          && (t.tierMaxAmount() == null || t.tierMaxAmount() >= amount))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No tier matches amount {} for fee rule {}", amount, rule.id());
                    return new DomainException(
                            "No tier matches amount %d for fee rule %s".formatted(amount, rule.id()));
                });

        if (tier.fixedAmount() != null) {
            return tier.fixedAmount();
        }

        // percentage-based tier — apply parent rule's floor/cap
        long raw = (amount * tier.percentageBps() + 5_000L) / 10_000L;
        long floored = rule.minFeeAmount() != null ? Math.max(raw, rule.minFeeAmount()) : raw;
        return rule.maxFeeAmount() != null ? Math.min(floored, rule.maxFeeAmount()) : floored;
    }
}
