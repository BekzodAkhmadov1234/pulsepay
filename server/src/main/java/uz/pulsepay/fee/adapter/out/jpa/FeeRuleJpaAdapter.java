package uz.pulsepay.fee.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.fee.adapter.out.jpa.entity.FeeRuleEntity;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;

import java.time.Instant;
import java.util.List;

@Repository
class FeeRuleJpaAdapter implements FeeRuleRepository {

    private final FeeRuleJpaRepository jpa;

    FeeRuleJpaAdapter(FeeRuleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<FeeRule> findApplicableRules(int transferTypeId, String sourceNetwork,
                                              String destNetwork, long amount) {
        return jpa.findApplicableRules(transferTypeId, sourceNetwork, destNetwork, amount, Instant.now())
                .stream().map(FeeRuleEntity::toDomain).toList();
    }
}
