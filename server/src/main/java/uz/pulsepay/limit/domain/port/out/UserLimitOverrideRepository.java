package uz.pulsepay.limit.domain.port.out;

import uz.pulsepay.limit.domain.model.UserLimitOverride;

import java.util.Optional;
import java.util.UUID;

public interface UserLimitOverrideRepository {
    Optional<UserLimitOverride> findActiveOverride(UUID userId, UUID limitRuleId);
}
