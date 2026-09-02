package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.limit.LimitUsageCounterEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LimitUsageCounterRepository extends JpaRepository<LimitUsageCounterEntity, UUID> {

    @Query("""
            SELECT c FROM LimitUsageCounterEntity c
            WHERE c.userId = :userId
            AND c.limitRuleId = :limitRuleId
            AND c.periodStart <= :now
            AND c.periodEnd > :now
            """)
    Optional<LimitUsageCounterEntity> findCurrentPeriod(@Param("userId") UUID userId,
                                                         @Param("limitRuleId") UUID limitRuleId,
                                                         @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE LimitUsageCounterEntity c
            SET c.usedAmount = c.usedAmount + :amountDelta,
                c.usedCount = c.usedCount + 1,
                c.updatedAt = :now
            WHERE c.userId = :userId
            AND c.limitRuleId = :limitRuleId
            AND c.periodStart <= :now
            AND c.periodEnd > :now
            """)
    void incrementUsage(@Param("userId") UUID userId, @Param("limitRuleId") UUID limitRuleId,
                        @Param("amountDelta") long amountDelta, @Param("now") Instant now);
}
