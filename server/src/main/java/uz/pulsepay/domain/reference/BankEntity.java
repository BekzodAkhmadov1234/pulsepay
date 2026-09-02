package uz.pulsepay.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.reference.Bank;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "banks")
public class BankEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "mfo_code", nullable = false, unique = true, length = 5)
    private String mfoCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tin", length = 9)
    private String tin;

    @Column(name = "role_issuer", nullable = false)
    private boolean roleIssuer;

    @Column(name = "role_acquirer", nullable = false)
    private boolean roleAcquirer;

    @Column(name = "role_settlement", nullable = false)
    private boolean roleSettlement;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BankEntity() {}

    public Bank toDomain() {
        return new Bank(id, mfoCode, name, tin, roleIssuer, roleAcquirer, roleSettlement, isActive, createdAt);
    }
}
