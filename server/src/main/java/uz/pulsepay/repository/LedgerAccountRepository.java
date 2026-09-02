package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.ledger.LedgerAccountEntity;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, UUID> {

    Optional<LedgerAccountEntity> findByCode(String code);

    @Modifying
    @Query("UPDATE LedgerAccountEntity a SET a.postedBalance = a.postedBalance + :delta WHERE a.id = :id")
    void incrementPostedBalance(@Param("id") UUID id, @Param("delta") long delta);
}
