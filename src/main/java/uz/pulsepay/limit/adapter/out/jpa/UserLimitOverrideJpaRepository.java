package uz.pulsepay.limit.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.limit.adapter.out.jpa.entity.UserLimitOverrideEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface UserLimitOverrideJpaRepository extends JpaRepository<UserLimitOverrideEntity, UUID> {

    @Query("""
            SELECT o FROM UserLimitOverrideEntity o
            WHERE o.userId = :userId
            AND o.limitRuleId = :limitRuleId
            AND (o.expiresAt IS NULL OR o.expiresAt > :now)
            """)
    Optional<UserLimitOverrideEntity> findActiveOverride(@Param("userId") UUID userId,
                                                          @Param("limitRuleId") UUID limitRuleId,
                                                          @Param("now") Instant now);
}
