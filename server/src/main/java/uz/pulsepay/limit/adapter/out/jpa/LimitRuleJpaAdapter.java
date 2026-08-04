package uz.pulsepay.limit.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.limit.adapter.out.jpa.entity.LimitRuleEntity;
import uz.pulsepay.limit.domain.model.LimitRule;
import uz.pulsepay.limit.domain.port.out.LimitRuleRepository;

import java.time.Instant;
import java.util.List;

@Repository
class LimitRuleJpaAdapter implements LimitRuleRepository {

    private final LimitRuleJpaRepository jpa;

    LimitRuleJpaAdapter(LimitRuleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<LimitRule> findActiveRules(String kycTier, int transferTypeId) {
        return jpa.findActiveRules(kycTier, transferTypeId, Instant.now())
                .stream().map(LimitRuleEntity::toDomain).toList();
    }
}
