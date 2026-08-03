package uz.pulsepay.limit.domain.port.out;

import uz.pulsepay.limit.domain.model.LimitRule;

import java.util.List;

public interface LimitRuleRepository {
    List<LimitRule> findActiveRules(String kycTier, int transferTypeId);
}
