package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.repository.SettlementBatchRepository;
import uz.pulsepay.domain.settlement.SettlementBatchEntity;
import uz.pulsepay.domain.settlement.BatchType;
import uz.pulsepay.domain.settlement.SettlementBatch;
import uz.pulsepay.domain.settlement.SettlementStatus;
import uz.pulsepay.domain.shared.ConflictException;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SettlementService {

    private final SettlementBatchRepository batchRepository;

    public SettlementService(SettlementBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Transactional
    public SettlementBatch generateDailyBatch(UUID merchantAccountId, LocalDate operationalDate) {
        // Guard: no duplicate open batch for same account+date
        batchRepository.findByMerchantAccountIdAndOperationalDateAndStatus(
                merchantAccountId, operationalDate, SettlementStatus.OPEN)
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Open batch already exists for account %s on %s"
                                    .formatted(merchantAccountId, operationalDate));
                });

        Long rawTotal = batchRepository.sumCompletedAmountNotInBatch(merchantAccountId, operationalDate);
        long totalAmount = rawTotal != null ? rawTotal : 0L;

        SettlementBatch batch = new SettlementBatch(
                UUID.randomUUID(), BatchType.MERCHANT_SETTLEMENT,
                merchantAccountId, null, operationalDate,
                totalAmount, SettlementStatus.OPEN, Instant.now(), null);

        SettlementBatch saved = batchRepository.save(SettlementBatchEntity.fromDomain(batch)).toDomain();
        log.info("Settlement batch generated: id={}, account={}, date={}, total={}",
                saved.id(), merchantAccountId, operationalDate, totalAmount);
        return saved;
    }

    @Transactional
    public SettlementBatch submitBatch(UUID batchId) {
        SettlementBatch batch = batchRepository.findById(batchId)
                .map(SettlementBatchEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Settlement batch not found: " + batchId));

        if (batch.status() != SettlementStatus.OPEN) {
            throw new DomainException("Batch is not in OPEN status: " + batch.status());
        }

        SettlementBatch updated = new SettlementBatch(
                batch.id(), batch.batchType(), batch.merchantAccountId(),
                batch.paymentNetworkId(), batch.operationalDate(),
                batch.totalAmount(), SettlementStatus.SUBMITTED,
                batch.generatedAt(), null);

        SettlementBatch saved = batchRepository.save(SettlementBatchEntity.fromDomain(updated)).toDomain();
        log.info("Settlement batch submitted: id={}", batchId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SettlementBatch> listBatches(UUID merchantAccountId) {
        return batchRepository.findByMerchantAccountIdOrderByGeneratedAtDesc(merchantAccountId)
                .stream()
                .map(SettlementBatchEntity::toDomain)
                .collect(Collectors.toList());
    }
}
