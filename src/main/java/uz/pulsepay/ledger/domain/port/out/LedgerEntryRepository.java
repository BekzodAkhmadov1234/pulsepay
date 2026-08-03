package uz.pulsepay.ledger.domain.port.out;

import uz.pulsepay.ledger.domain.model.LedgerEntry;

import java.util.List;

public interface LedgerEntryRepository {
    void saveAll(List<LedgerEntry> entries);
}
