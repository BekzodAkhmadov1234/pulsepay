package uz.pulsepay.fee.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.fee.adapter.out.jpa.entity.FeeRuleTierEntity;
import uz.pulsepay.fee.domain.model.FeeRuleTier;
import uz.pulsepay.fee.domain.port.out.FeeRuleTierRepository;

import java.util.List;
import java.util.UUID;

@Repository
class FeeRuleTierJpaAdapter implements FeeRuleTierRepository {

    private final FeeRuleTierJpaRepository jpa;

    FeeRuleTierJpaAdapter(FeeRuleTierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<FeeRuleTier> findByFeeRuleId(UUID feeRuleId) {
        return jpa.findByFeeRuleId(feeRuleId).stream()
                .map(FeeRuleTierEntity::toDomain)
                .toList();
    }

    @Override
    public FeeRuleTier save(FeeRuleTier tier) {
        return jpa.save(FeeRuleTierEntity.fromDomain(tier)).toDomain();
    }

    @Override
    public void deleteById(UUID tierId) {
        jpa.deleteById(tierId);
    }
}
