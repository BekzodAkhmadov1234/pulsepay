package uz.pulsepay.fee.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.fee.adapter.out.jpa.entity.FeeRuleEntity;
import uz.pulsepay.fee.domain.model.FeeRule;
import uz.pulsepay.fee.domain.port.out.FeeRuleRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class FeeRuleJpaAdapter implements FeeRuleRepository {

    private final FeeRuleJpaRepository jpa;

    FeeRuleJpaAdapter(FeeRuleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<FeeRule> findApplicableRules(int transferTypeId, String sourceNetwork,
                                              String destNetwork, long amount,
                                              String currencyCode, Instant occurringAt) {
        return jpa.findApplicableRules(transferTypeId, sourceNetwork, destNetwork,
                        amount, currencyCode, occurringAt)
                .stream().map(FeeRuleEntity::toDomain).toList();
    }

    @Override
    public Optional<FeeRule> findById(UUID id) {
        return jpa.findById(id).map(FeeRuleEntity::toDomain);
    }

    @Override
    public List<FeeRule> findAll() {
        return jpa.findAll().stream().map(FeeRuleEntity::toDomain).toList();
    }

    @Override
    public List<FeeRule> findActiveByScope(Integer transferTypeId, String sourceNetwork,
                                            String destinationNetwork, String currencyCode, int priority) {
        return jpa.findActiveByScope(transferTypeId, sourceNetwork, destinationNetwork, currencyCode, priority)
                .stream().map(FeeRuleEntity::toDomain).toList();
    }

    @Override
    public long countTransferReferences(UUID ruleId) {
        return jpa.countTransferReferences(ruleId);
    }

    @Override
    public FeeRule save(FeeRule rule) {
        return jpa.save(FeeRuleEntity.fromDomain(rule)).toDomain();
    }
}
