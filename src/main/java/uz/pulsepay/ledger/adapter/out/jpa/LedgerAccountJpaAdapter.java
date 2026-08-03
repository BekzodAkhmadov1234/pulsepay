package uz.pulsepay.ledger.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.ledger.adapter.out.jpa.entity.LedgerAccountEntity;
import uz.pulsepay.ledger.domain.model.LedgerAccount;
import uz.pulsepay.ledger.domain.port.out.LedgerAccountRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class LedgerAccountJpaAdapter implements LedgerAccountRepository {

    private final LedgerAccountJpaRepository jpa;

    LedgerAccountJpaAdapter(LedgerAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<LedgerAccount> findByCode(String code) {
        return jpa.findByCode(code).map(LedgerAccountEntity::toDomain);
    }

    @Override
    public void incrementPostedBalance(UUID accountId, long delta) {
        jpa.incrementPostedBalance(accountId, delta);
    }
}
