package uz.pulsepay.fee.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.fee.adapter.out.jpa.entity.FeeRuleTierEntity;

import java.util.List;
import java.util.UUID;

interface FeeRuleTierJpaRepository extends JpaRepository<FeeRuleTierEntity, UUID> {
    List<FeeRuleTierEntity> findByFeeRuleId(UUID feeRuleId);
}
