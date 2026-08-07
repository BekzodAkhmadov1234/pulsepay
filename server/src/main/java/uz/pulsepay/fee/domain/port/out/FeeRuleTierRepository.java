package uz.pulsepay.fee.domain.port.out;

import uz.pulsepay.fee.domain.model.FeeRuleTier;

import java.util.List;
import java.util.UUID;

public interface FeeRuleTierRepository {
    List<FeeRuleTier> findByFeeRuleId(UUID feeRuleId);
    FeeRuleTier save(FeeRuleTier tier);
    void deleteById(UUID tierId);
}
