package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.card.CardEntity;
import uz.pulsepay.domain.card.CardStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {

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

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO instruments (id, owner_party_id, instrument_type, status, created_at)
            VALUES (:id, :ownerPartyId, 'card', 'active', NOW())
            ON CONFLICT DO NOTHING
            """)
    void insertInstrument(@Param("id") UUID id, @Param("ownerPartyId") UUID ownerPartyId);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE instruments SET removed_at = NOW(), status = 'removed'
            WHERE id = :cardId
            """)
    void softDelete(@Param("cardId") UUID cardId);

    @Query(nativeQuery = true, value = """
            SELECT c.* FROM cards c
            JOIN instruments i ON c.id = i.id
            WHERE c.masked_pan LIKE CONCAT(:first6, '%', :last4)
            AND i.removed_at IS NULL
            LIMIT 1
            """)
    Optional<CardEntity> findByMaskedPanPattern(@Param("first6") String first6,
                                                 @Param("last4") String last4);

    @Query(nativeQuery = true, value = """
            SELECT i.owner_party_id FROM instruments i
            WHERE i.id = :cardId AND i.removed_at IS NULL
            """)
    Optional<UUID> findOwnerIdByCardId(@Param("cardId") UUID cardId);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE cards SET balance_tiyin = balance_tiyin - :amount
            WHERE id = :cardId AND balance_tiyin >= :amount
            """)
    int debitBalance(@Param("cardId") UUID cardId, @Param("amount") long amount);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE cards SET balance_tiyin = balance_tiyin + :amount
            WHERE id = :cardId
            """)
    void creditBalance(@Param("cardId") UUID cardId, @Param("amount") long amount);

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

    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE cards SET is_default = true WHERE id = :cardId")
    void markDefault(@Param("cardId") UUID cardId);
}
