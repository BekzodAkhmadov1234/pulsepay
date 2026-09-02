package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.fee.FeeRuleEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FeeRuleRepository extends JpaRepository<FeeRuleEntity, UUID> {

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
            AND f.currencyCode = :currencyCode
            ORDER BY f.priority ASC
            """)
    List<FeeRuleEntity> findApplicableRules(@Param("transferTypeId") int transferTypeId,
                                             @Param("sourceNetwork") String sourceNetwork,
                                             @Param("destNetwork") String destNetwork,
                                             @Param("amount") long amount,
                                             @Param("currencyCode") String currencyCode,
                                             @Param("now") Instant now);

    @Query("""
            SELECT f FROM FeeRuleEntity f
            WHERE f.isActive = true
            AND (:transferTypeId IS NULL AND f.transferTypeId IS NULL
                 OR f.transferTypeId = :transferTypeId)
            AND (:sourceNetwork IS NULL AND f.sourceNetwork IS NULL
                 OR f.sourceNetwork = :sourceNetwork)
            AND (:destinationNetwork IS NULL AND f.destinationNetwork IS NULL
                 OR f.destinationNetwork = :destinationNetwork)
            AND f.currencyCode = :currencyCode
            AND f.priority = :priority
            """)
    List<FeeRuleEntity> findActiveByScope(@Param("transferTypeId") Integer transferTypeId,
                                           @Param("sourceNetwork") String sourceNetwork,
                                           @Param("destinationNetwork") String destinationNetwork,
                                           @Param("currencyCode") String currencyCode,
                                           @Param("priority") int priority);

    @Query(value = "SELECT COUNT(*) FROM transfers WHERE applied_fee_rule_id = :ruleId", nativeQuery = true)
    long countTransferReferences(@Param("ruleId") UUID ruleId);
}
