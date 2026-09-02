package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

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

    protected LedgerTransaction() {}

    public LedgerTransaction(UUID id, UUID transferId, String status, String externalId,
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

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public String getStatus() { return status; }
    public String getExternalId() { return externalId; }
    public int getEntryTypeId() { return entryTypeId; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getPostedAt() { return postedAt; }
    public UUID getReversesTxnId() { return reversesTxnId; }

    public void setStatus(String status) { this.status = status; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
