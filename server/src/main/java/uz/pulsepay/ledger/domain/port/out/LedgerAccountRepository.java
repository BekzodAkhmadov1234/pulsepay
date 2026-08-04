package uz.pulsepay.ledger.domain.port.out;

import uz.pulsepay.ledger.domain.model.LedgerAccount;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository {
    Optional<LedgerAccount> findByCode(String code);

    /**
     * Atomic SQL increment — avoids ORM-level race (Risk #2).
     */
    void incrementPostedBalance(UUID accountId, long delta);
}
