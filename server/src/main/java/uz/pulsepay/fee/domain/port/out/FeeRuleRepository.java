package uz.pulsepay.fee.domain.port.out;

import uz.pulsepay.fee.domain.model.FeeRule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeRuleRepository {
    List<FeeRule> findApplicableRules(int transferTypeId, String sourceNetwork,
                                      String destNetwork, long amount,
                                      String currencyCode, Instant occurringAt);

    Optional<FeeRule> findById(UUID id);

    List<FeeRule> findAll();

    /** All active rules matching the given scope exactly (for overlap validation). */
    List<FeeRule> findActiveByScope(Integer transferTypeId, String sourceNetwork,
                                    String destinationNetwork, String currencyCode, int priority);

    /** Count of transfers referencing this rule (for immutability guard). */
    long countTransferReferences(UUID ruleId);

    FeeRule save(FeeRule rule);
}
