package uz.pulsepay.limit.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.limit.adapter.out.jpa.entity.LimitRuleEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface LimitRuleJpaRepository extends JpaRepository<LimitRuleEntity, UUID> {

    @Query("""
            SELECT r FROM LimitRuleEntity r
            WHERE r.isActive = true
            AND (r.kycTier = :kycTier OR r.kycTier = 'all')
            AND (r.transferTypeId IS NULL OR r.transferTypeId = :transferTypeId)
            AND r.effectiveFrom <= :now
            AND (r.effectiveTo IS NULL OR r.effectiveTo > :now)
            """)
    List<LimitRuleEntity> findActiveRules(@Param("kycTier") String kycTier,
                                           @Param("transferTypeId") int transferTypeId,
                                           @Param("now") Instant now);
}
