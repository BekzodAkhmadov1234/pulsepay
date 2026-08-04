package uz.pulsepay.ledger.domain.port.out;

import uz.pulsepay.ledger.domain.model.LedgerTransaction;

public interface LedgerTransactionRepository {
    LedgerTransaction save(LedgerTransaction tx);
}
