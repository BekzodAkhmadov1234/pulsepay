package uz.pulsepay.settlement.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.settlement.adapter.out.jpa.entity.SettlementBatchEntity;
import uz.pulsepay.settlement.domain.model.SettlementBatch;
import uz.pulsepay.settlement.domain.model.SettlementStatus;
import uz.pulsepay.settlement.domain.port.out.SettlementBatchRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SettlementBatchJpaAdapter implements SettlementBatchRepository {

    private final SettlementBatchJpaRepository jpa;

    SettlementBatchJpaAdapter(SettlementBatchJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SettlementBatch save(SettlementBatch batch) {
        return jpa.save(SettlementBatchEntity.fromDomain(batch)).toDomain();
    }

    @Override
    public Optional<SettlementBatch> findById(UUID id) {
        return jpa.findById(id).map(SettlementBatchEntity::toDomain);
    }

    @Override
    public List<SettlementBatch> findByMerchantAccountId(UUID merchantAccountId) {
        return jpa.findByMerchantAccountIdOrderByGeneratedAtDesc(merchantAccountId)
                .stream().map(SettlementBatchEntity::toDomain).toList();
    }

    @Override
    public Optional<SettlementBatch> findByMerchantAccountIdAndOperationalDateAndStatus(
            UUID merchantAccountId, LocalDate date, SettlementStatus status) {
        return jpa.findByMerchantAccountIdAndOperationalDateAndStatus(merchantAccountId, date, status)
                .map(SettlementBatchEntity::toDomain);
    }

    @Override
    public long sumCompletedAmountNotInBatch(UUID merchantAccountId, LocalDate date) {
        Long result = jpa.sumCompletedAmountNotInBatch(merchantAccountId, date);
        return result != null ? result : 0L;
    }
}
