package uz.pulsepay.limit.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.limit.adapter.out.jpa.entity.UserLimitOverrideEntity;
import uz.pulsepay.limit.domain.model.UserLimitOverride;
import uz.pulsepay.limit.domain.port.out.UserLimitOverrideRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class UserLimitOverrideJpaAdapter implements UserLimitOverrideRepository {

    private final UserLimitOverrideJpaRepository jpa;

    UserLimitOverrideJpaAdapter(UserLimitOverrideJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserLimitOverride> findActiveOverride(UUID userId, UUID limitRuleId) {
        return jpa.findActiveOverride(userId, limitRuleId, Instant.now())
                .map(UserLimitOverrideEntity::toDomain);
    }
}
