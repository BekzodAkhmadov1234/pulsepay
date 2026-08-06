package uz.pulsepay.transfer.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.transfer.adapter.out.jpa.entity.TransferEntity;
import uz.pulsepay.transfer.domain.model.ParticipantRole;
import uz.pulsepay.transfer.domain.model.TransferStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {
    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TransferEntity t SET t.status = :status, t.completedAt = :completedAt WHERE t.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") TransferStatus status,
                      @Param("completedAt") Instant completedAt);

    @Query("""
            SELECT t FROM TransferEntity t
            WHERE t.id IN (
                SELECT p.transferId FROM TransferParticipantEntity p
                WHERE p.partyId = :senderId AND p.role = :role
            )
            ORDER BY t.initiatedAt DESC
            """)
    List<TransferEntity> findBySenderIdAndRole(@Param("senderId") UUID senderId,
                                                @Param("role") ParticipantRole role);

    @Query(nativeQuery = true, value = """
            SELECT t.id, t.amount, t.fee_amount, t.currency_code, t.status, t.channel,
                   t.idempotency_key,
                   TO_CHAR(t.initiated_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS initiated_at,
                   TO_CHAR(t.completed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS completed_at,
                   su.full_name  AS sender_name,
                   sc.masked_pan AS sender_masked_pan,
                   ru.full_name  AS recipient_name,
                   rc.masked_pan AS recipient_masked_pan,
                   TO_CHAR(txn.processed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS processed_at,
                   'debit' AS direction
            FROM transfers t
            LEFT JOIN transfer_participants sp ON sp.transfer_id = t.id AND sp.role = 'sender'
            LEFT JOIN users su ON su.id = sp.party_id
            LEFT JOIN cards sc ON sc.id = sp.instrument_id
            LEFT JOIN transfer_participants rp ON rp.transfer_id = t.id AND rp.role = 'recipient'
            LEFT JOIN users ru ON ru.id = rp.party_id
            LEFT JOIN cards rc ON rc.id = rp.instrument_id
            LEFT JOIN transactions txn ON txn.transfer_id = t.id AND txn.transaction_type_id = 1
            WHERE sp.party_id = :userId
            UNION ALL
            SELECT t.id, t.amount, t.fee_amount, t.currency_code, t.status, t.channel,
                   t.idempotency_key,
                   TO_CHAR(t.initiated_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                   TO_CHAR(t.completed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                   su.full_name,
                   sc.masked_pan,
                   ru.full_name,
                   rc.masked_pan,
                   TO_CHAR(txn.processed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                   'credit' AS direction
            FROM transfers t
            LEFT JOIN transfer_participants sp ON sp.transfer_id = t.id AND sp.role = 'sender'
            LEFT JOIN users su ON su.id = sp.party_id
            LEFT JOIN cards sc ON sc.id = sp.instrument_id
            LEFT JOIN transfer_participants rp ON rp.transfer_id = t.id AND rp.role = 'recipient'
            LEFT JOIN users ru ON ru.id = rp.party_id
            LEFT JOIN cards rc ON rc.id = rp.instrument_id
            LEFT JOIN transactions txn ON txn.transfer_id = t.id AND txn.transaction_type_id = 1
            WHERE rp.party_id = :userId
            ORDER BY initiated_at DESC
            """)
    List<Object[]> findSummariesByParticipantId(@Param("userId") UUID userId);
}
