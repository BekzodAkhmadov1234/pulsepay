package uz.pulsepay.ledger.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.ledger.adapter.out.jpa.entity.LedgerEntryEntity;
import uz.pulsepay.ledger.domain.model.LedgerEntry;
import uz.pulsepay.ledger.domain.port.out.LedgerEntryRepository;

import java.util.List;

@Repository
class LedgerEntryJpaAdapter implements LedgerEntryRepository {

    private final LedgerEntryJpaRepository jpa;

    LedgerEntryJpaAdapter(LedgerEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void saveAll(List<LedgerEntry> entries) {
        jpa.saveAll(entries.stream().map(LedgerEntryEntity::fromDomain).toList());
    }
}
