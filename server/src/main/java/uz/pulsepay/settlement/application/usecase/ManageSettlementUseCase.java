package uz.pulsepay.settlement.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.settlement.domain.model.BatchType;
import uz.pulsepay.settlement.domain.model.SettlementBatch;
import uz.pulsepay.settlement.domain.model.SettlementStatus;
import uz.pulsepay.settlement.domain.port.in.ManageSettlementPort;
import uz.pulsepay.settlement.domain.port.out.SettlementBatchRepository;
import uz.pulsepay.shared.exception.ConflictException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ManageSettlementUseCase implements ManageSettlementPort {

    private final SettlementBatchRepository batchRepository;

    public ManageSettlementUseCase(SettlementBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Override
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

        long totalAmount = batchRepository.sumCompletedAmountNotInBatch(merchantAccountId, operationalDate);

        SettlementBatch batch = batchRepository.save(new SettlementBatch(
                UUID.randomUUID(), BatchType.MERCHANT_SETTLEMENT,
                merchantAccountId, null, operationalDate,
                totalAmount, SettlementStatus.OPEN, Instant.now(), null));

        log.info("Settlement batch generated: id={}, account={}, date={}, total={}",
                batch.id(), merchantAccountId, operationalDate, totalAmount);
        return batch;
    }

    @Override
    @Transactional
    public SettlementBatch submitBatch(UUID batchId) {
        SettlementBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Settlement batch not found: " + batchId));

        if (batch.status() != SettlementStatus.OPEN) {
            throw new uz.pulsepay.shared.exception.DomainException(
                    "Batch is not in OPEN status: " + batch.status());
        }

        SettlementBatch updated = new SettlementBatch(
                batch.id(), batch.batchType(), batch.merchantAccountId(),
                batch.paymentNetworkId(), batch.operationalDate(),
                batch.totalAmount(), SettlementStatus.SUBMITTED,
                batch.generatedAt(), null);

        SettlementBatch saved = batchRepository.save(updated);
        log.info("Settlement batch submitted: id={}", batchId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementBatch> listBatches(UUID merchantAccountId) {
        return batchRepository.findByMerchantAccountId(merchantAccountId);
    }
}
