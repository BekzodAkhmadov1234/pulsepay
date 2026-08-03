package uz.pulsepay.ledger.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.ledger.domain.model.LedgerTransaction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransactionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "entry_type_id", nullable = false)
    private int entryTypeId;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "reverses_txn_id")
    private UUID reversesTxnId;

    protected LedgerTransactionEntity() {}

    public LedgerTransactionEntity(UUID id, UUID transferId, String status, String externalId,
                                    int entryTypeId, Instant effectiveAt, Instant postedAt,
                                    UUID reversesTxnId) {
        this.id = id;
        this.transferId = transferId;
        this.status = status;
        this.externalId = externalId;
        this.entryTypeId = entryTypeId;
        this.effectiveAt = effectiveAt;
        this.postedAt = postedAt;
        this.reversesTxnId = reversesTxnId;
    }

    public LedgerTransaction toDomain() {
        return new LedgerTransaction(id, transferId, status, externalId, entryTypeId,
                effectiveAt, postedAt, reversesTxnId);
    }

    public static LedgerTransactionEntity fromDomain(LedgerTransaction t) {
        return new LedgerTransactionEntity(t.id(), t.transferId(), t.status(), t.externalId(),
                t.entryTypeId(), t.effectiveAt(), t.postedAt(), t.reversesTxnId());
    }
}
