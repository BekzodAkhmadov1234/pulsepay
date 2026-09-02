package uz.pulsepay.domain.party;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.party.BankAccountDetails;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_account_details")
public class BankAccountDetailsEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "instrument_id", nullable = false, unique = true)
    private UUID instrumentId;

    @Column(name = "bank_id", nullable = false)
    private UUID bankId;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "account_number", length = 25)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BankAccountDetailsEntity() {}

    public BankAccountDetailsEntity(UUID id, UUID instrumentId, UUID bankId,
                                    String iban, String accountNumber,
                                    String accountHolderName, Instant createdAt) {
        this.id = id;
        this.instrumentId = instrumentId;
        this.bankId = bankId;
        this.iban = iban;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.createdAt = createdAt;
    }

    public BankAccountDetails toDomain() {
        return new BankAccountDetails(id, instrumentId, bankId, iban,
                accountNumber, accountHolderName, createdAt);
    }

    public static BankAccountDetailsEntity fromDomain(BankAccountDetails d) {
        return new BankAccountDetailsEntity(d.id(), d.instrumentId(), d.bankId(),
                d.iban(), d.accountNumber(), d.accountHolderName(), d.createdAt());
    }
}
