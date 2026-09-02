package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.fee.FeeRuleTierEntity;

import java.util.List;
import java.util.UUID;

public interface FeeRuleTierRepository extends JpaRepository<FeeRuleTierEntity, UUID> {

    List<FeeRuleTierEntity> findByFeeRuleId(UUID feeRuleId);
}
