package uz.pulsepay.fee.domain.port.in;

import uz.pulsepay.fee.domain.model.FeePayer;
import uz.pulsepay.fee.domain.model.FeeRecipient;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.model.FeeRuleTier;
import uz.pulsepay.fee.domain.model.FeeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ManageFeeRulePort {

    record CreateFeeRuleCommand(
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
            Instant effectiveFrom,
            Instant effectiveTo,
            Integer transferTypeId,
            FeePayer feePayer,
            FeeRecipient feeRecipient,
            List<AddTierCommand> tiers
    ) {}

    record AddTierCommand(
            long tierMinAmount,
            Long tierMaxAmount,
            Long fixedAmount,
            Integer percentageBps
    ) {}

    FeeRule createRule(CreateFeeRuleCommand command);

    List<FeeRule> listAll();

    FeeRule getRule(UUID id);

    /** Soft-deactivate: sets is_active=false and effective_to=now. */
    FeeRule deactivate(UUID id);

    /** Re-activate: sets is_active=true and effective_to=null. Runs overlap check. */
    FeeRule activate(UUID id);

    /**
     * Atomically deactivates the old rule and creates a replacement.
     * The replacement inherits all parameters from the command.
     */
    FeeRule supersede(UUID id, CreateFeeRuleCommand replacement);

    FeeRuleTier addTier(UUID ruleId, AddTierCommand command);

    void removeTier(UUID ruleId, UUID tierId);
}
