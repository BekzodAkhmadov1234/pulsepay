package uz.pulsepay.settlement.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.settlement.adapter.out.jpa.entity.SettlementBatchEntity;
import uz.pulsepay.settlement.domain.model.SettlementStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatchEntity, UUID> {

    List<SettlementBatchEntity> findByMerchantAccountIdOrderByGeneratedAtDesc(UUID merchantAccountId);

    Optional<SettlementBatchEntity> findByMerchantAccountIdAndOperationalDateAndStatus(
            UUID merchantAccountId, LocalDate operationalDate, SettlementStatus status);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(t.amount + t.fee_amount), 0)
            FROM transfers t
            JOIN transfer_participants tp ON tp.transfer_id = t.id AND tp.role = 'recipient'
            JOIN merchant_accounts ma ON ma.id = tp.instrument_id
            WHERE ma.id = :accountId
              AND t.status = 'completed'
              AND DATE(t.completed_at) <= :date
              AND NOT EXISTS (
                  SELECT 1 FROM transactions txn WHERE txn.transfer_id = t.id
                    AND txn.settlement_batch_id IS NOT NULL
              )
            """)
    Long sumCompletedAmountNotInBatch(@Param("accountId") UUID accountId,
                                       @Param("date") LocalDate date);
}
