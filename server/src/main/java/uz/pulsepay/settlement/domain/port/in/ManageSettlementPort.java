package uz.pulsepay.settlement.domain.port.in;

import uz.pulsepay.settlement.domain.model.SettlementBatch;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageSettlementPort {
    SettlementBatch generateDailyBatch(UUID merchantAccountId, LocalDate operationalDate);
    SettlementBatch submitBatch(UUID batchId);
    List<SettlementBatch> listBatches(UUID merchantAccountId);
}
