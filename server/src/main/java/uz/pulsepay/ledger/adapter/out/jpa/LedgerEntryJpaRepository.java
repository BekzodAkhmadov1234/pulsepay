package uz.pulsepay.ledger.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.ledger.adapter.out.jpa.entity.LedgerEntryEntity;

import java.util.UUID;

interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {
}
