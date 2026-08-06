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

    /**
     * Inserts the instruments row before the cards row (class-table inheritance).
     * ON CONFLICT DO NOTHING makes it safe to call multiple times.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO instruments (id, owner_party_id, instrument_type, status, created_at)
            VALUES (:id, :ownerPartyId, 'card', 'active', NOW())
            ON CONFLICT DO NOTHING
            """)
    void insertInstrument(@Param("id") UUID id, @Param("ownerPartyId") UUID ownerPartyId);

    /** Soft-deletes the instruments row: sets removed_at and status = 'removed'. */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE instruments SET removed_at = NOW(), status = 'removed'
            WHERE id = :cardId
            """)
    void softDelete(@Param("cardId") UUID cardId);

    /** Finds a non-deleted card whose masked_pan starts with first6 and ends with last4. */
    @Query(nativeQuery = true, value = """
            SELECT c.* FROM cards c
            JOIN instruments i ON c.id = i.id
            WHERE c.masked_pan LIKE CONCAT(:first6, '%', :last4)
            AND i.removed_at IS NULL
            LIMIT 1
            """)
    Optional<CardEntity> findByMaskedPanPattern(@Param("first6") String first6,
                                                @Param("last4") String last4);

    /** Returns the owner party id for a non-deleted card. */
    @Query(nativeQuery = true, value = """
            SELECT i.owner_party_id FROM instruments i
            WHERE i.id = :cardId AND i.removed_at IS NULL
            """)
    Optional<UUID> findOwnerIdByCardId(@Param("cardId") UUID cardId);

    /**
     * Atomic balance debit: deducts amount only if balance is sufficient.
     * Returns rows updated (0 = insufficient funds).
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE cards SET balance_tiyin = balance_tiyin - :amount
            WHERE id = :cardId AND balance_tiyin >= :amount
            """)
    int debitBalance(@Param("cardId") UUID cardId, @Param("amount") long amount);

    /** Atomic balance credit: unconditionally adds amount to the card's balance. */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE cards SET balance_tiyin = balance_tiyin + :amount
            WHERE id = :cardId
            """)
    void creditBalance(@Param("cardId") UUID cardId, @Param("amount") long amount);

    /** Clears is_default for all non-deleted cards owned by the user. */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE cards SET is_default = false
            WHERE id IN (
                SELECT c.id FROM cards c
                JOIN instruments i ON c.id = i.id
                WHERE i.owner_party_id = :userId
                AND i.removed_at IS NULL
            )
            """)
    void clearDefaultForUser(@Param("userId") UUID userId);

    /** Sets is_default = true for a specific card. Clears L1 cache so subsequent findById returns fresh data. */
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE cards SET is_default = true WHERE id = :cardId")
    void markDefault(@Param("cardId") UUID cardId);
}
