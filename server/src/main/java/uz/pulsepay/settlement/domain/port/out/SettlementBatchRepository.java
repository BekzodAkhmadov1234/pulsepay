package uz.pulsepay.settlement.domain.port.out;

import uz.pulsepay.settlement.domain.model.SettlementBatch;
import uz.pulsepay.settlement.domain.model.SettlementStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementBatchRepository {
    SettlementBatch save(SettlementBatch batch);
    Optional<SettlementBatch> findById(UUID id);
    List<SettlementBatch> findByMerchantAccountId(UUID merchantAccountId);
    Optional<SettlementBatch> findByMerchantAccountIdAndOperationalDateAndStatus(
            UUID merchantAccountId, LocalDate date, SettlementStatus status);
    long sumCompletedAmountNotInBatch(UUID merchantAccountId, LocalDate date);
}
