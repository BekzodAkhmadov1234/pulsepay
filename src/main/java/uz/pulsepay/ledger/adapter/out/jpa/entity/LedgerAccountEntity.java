package uz.pulsepay.ledger.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import uz.pulsepay.ledger.domain.model.LedgerAccount;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccountEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "account_type_id", nullable = false)
    private int accountTypeId;

    @Column(name = "normal_balance", nullable = false, length = 6)
    private String normalBalance;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "posted_balance", nullable = false)
    private long postedBalance;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerAccountEntity() {}

    public LedgerAccount toDomain() {
        return new LedgerAccount(id, accountTypeId, normalBalance, code, currencyCode,
                postedBalance, lockVersion, status, createdAt);
    }
}
