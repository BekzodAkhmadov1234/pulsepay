package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "banks")
public class Bank {

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

    protected Bank() {}

    public UUID getId() { return id; }
    public String getMfoCode() { return mfoCode; }
    public String getName() { return name; }
    public String getTin() { return tin; }
    public boolean isRoleIssuer() { return roleIssuer; }
    public boolean isRoleAcquirer() { return roleAcquirer; }
    public boolean isRoleSettlement() { return roleSettlement; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
}
