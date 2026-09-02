package uz.pulsepay.domain.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.ledger.EntryDirection;
import uz.pulsepay.domain.ledger.LedgerEntry;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "ledger_transaction_id", nullable = false)
    private UUID ledgerTransactionId;

    @Column(name = "ledger_account_id", nullable = false)
    private UUID ledgerAccountId;

    @Convert(converter = EntryDirectionConverter.class)
    @Column(name = "direction", nullable = false, length = 6)
    private EntryDirection direction;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {}

    public LedgerEntryEntity(UUID id, UUID ledgerTransactionId, UUID ledgerAccountId,
                              EntryDirection direction, long amount, String currencyCode,
                              String status, Instant createdAt) {
        this.id = id;
        this.ledgerTransactionId = ledgerTransactionId;
        this.ledgerAccountId = ledgerAccountId;
        this.direction = direction;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static LedgerEntryEntity fromDomain(LedgerEntry e) {
        return new LedgerEntryEntity(e.id(), e.ledgerTransactionId(), e.ledgerAccountId(),
                e.direction(), e.amount(), e.currencyCode(), e.status(), e.createdAt());
    }
}
