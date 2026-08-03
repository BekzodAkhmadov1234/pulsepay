package uz.pulsepay.ledger.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.ledger.adapter.out.jpa.entity.LedgerTransactionEntity;
import uz.pulsepay.ledger.domain.model.LedgerTransaction;
import uz.pulsepay.ledger.domain.port.out.LedgerTransactionRepository;

@Repository
class LedgerTransactionJpaAdapter implements LedgerTransactionRepository {

    private final LedgerTransactionJpaRepository jpa;

    LedgerTransactionJpaAdapter(LedgerTransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public LedgerTransaction save(LedgerTransaction tx) {
        return jpa.save(LedgerTransactionEntity.fromDomain(tx)).toDomain();
    }
}
