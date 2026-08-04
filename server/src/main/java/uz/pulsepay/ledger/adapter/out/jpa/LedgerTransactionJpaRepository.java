package uz.pulsepay.ledger.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.ledger.adapter.out.jpa.entity.LedgerTransactionEntity;

import java.util.UUID;

interface LedgerTransactionJpaRepository extends JpaRepository<LedgerTransactionEntity, UUID> {
}
