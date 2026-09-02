package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount {

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

    protected LedgerAccount() {}

    public UUID getId() { return id; }
    public int getAccountTypeId() { return accountTypeId; }
    public String getNormalBalance() { return normalBalance; }
    public String getCode() { return code; }
    public String getCurrencyCode() { return currencyCode; }
    public long getPostedBalance() { return postedBalance; }
    public long getLockVersion() { return lockVersion; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPostedBalance(long postedBalance) { this.postedBalance = postedBalance; }
    public void setStatus(String status) { this.status = status; }
}
