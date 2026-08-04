package uz.pulsepay.fee.domain.port.out;

import uz.pulsepay.fee.domain.model.FeeRule;

import java.util.List;

public interface FeeRuleRepository {
    List<FeeRule> findApplicableRules(int transferTypeId, String sourceNetwork,
                                      String destNetwork, long amount);
}
