package uz.pulsepay.card.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.card.adapter.out.jpa.entity.CardEntity;
import uz.pulsepay.card.domain.model.CardStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {

    /**
     * Joins instruments to filter by owner and exclude soft-deleted rows (removed_at IS NULL).
     */
    @Query(nativeQuery = true, value = """
            SELECT c.* FROM cards c
            JOIN instruments i ON c.id = i.id
            WHERE i.owner_party_id = :userId
            AND i.removed_at IS NULL
            """)
    List<CardEntity> findByOwnerUserId(@Param("userId") UUID userId);

    @Query(nativeQuery = true, value = """
            SELECT c.* FROM cards c
            JOIN instruments i ON c.id = i.id
            WHERE c.id = :cardId
            AND i.owner_party_id = :userId
            AND i.removed_at IS NULL
            """)
    Optional<CardEntity> findByIdAndOwnerUserId(@Param("cardId") UUID cardId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE CardEntity c SET c.status = :status WHERE c.id = :cardId")
    void updateStatus(@Param("cardId") UUID cardId, @Param("status") CardStatus status);

    /**
     * Deactivates all VERIFIED cards for the given owner — called on security events (Phase 2 MANDATORY).
     * Uses a JOIN to instruments to resolve the owner.
     * Uppercase status values match @Enumerated(EnumType.STRING) storage.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE cards SET status = 'INACTIVE'
            WHERE id IN (
                SELECT c.id FROM cards c
                JOIN instruments i ON c.id = i.id
                WHERE i.owner_party_id = :userId
                AND i.removed_at IS NULL
                AND c.status = 'VERIFIED'
            )
            """)
    int deactivateAllVerifiedByOwner(@Param("userId") UUID userId);
}
