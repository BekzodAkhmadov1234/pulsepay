package uz.pulsepay.ledger.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.ledger.adapter.out.jpa.entity.LedgerAccountEntity;

import java.util.Optional;
import java.util.UUID;

interface LedgerAccountJpaRepository extends JpaRepository<LedgerAccountEntity, UUID> {
    Optional<LedgerAccountEntity> findByCode(String code);

    /**
     * Atomic SQL-level increment — no ORM-level read-modify-write race (Risk #2).
     */
    @Modifying
    @Query("UPDATE LedgerAccountEntity a SET a.postedBalance = a.postedBalance + :delta WHERE a.id = :id")
    void incrementPostedBalance(@Param("id") UUID id, @Param("delta") long delta);
}
