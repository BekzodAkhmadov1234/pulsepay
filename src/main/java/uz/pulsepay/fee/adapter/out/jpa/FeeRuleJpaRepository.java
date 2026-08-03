package uz.pulsepay.fee.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.fee.adapter.out.jpa.entity.FeeRuleEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface FeeRuleJpaRepository extends JpaRepository<FeeRuleEntity, UUID> {

    @Query("""
            SELECT f FROM FeeRuleEntity f
            WHERE f.isActive = true
            AND (f.transferTypeId IS NULL OR f.transferTypeId = :transferTypeId)
            AND (f.sourceNetwork IS NULL OR f.sourceNetwork = :sourceNetwork)
            AND (f.destinationNetwork IS NULL OR f.destinationNetwork = :destNetwork)
            AND f.minAmount <= :amount
            AND (f.maxAmount IS NULL OR f.maxAmount >= :amount)
            AND f.effectiveFrom <= :now
            AND (f.effectiveTo IS NULL OR f.effectiveTo > :now)
            ORDER BY f.priority ASC
            """)
    List<FeeRuleEntity> findApplicableRules(@Param("transferTypeId") int transferTypeId,
                                             @Param("sourceNetwork") String sourceNetwork,
                                             @Param("destNetwork") String destNetwork,
                                             @Param("amount") long amount,
                                             @Param("now") Instant now);
}
