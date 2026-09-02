package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.ledger.LedgerTransactionEntity;

import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransactionEntity, UUID> {
}
