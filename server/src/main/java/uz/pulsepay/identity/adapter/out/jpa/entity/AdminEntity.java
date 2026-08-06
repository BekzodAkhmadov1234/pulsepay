package uz.pulsepay.identity.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.identity.domain.model.Admin;
import uz.pulsepay.identity.domain.model.AdminRole;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admins")
public class AdminEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Convert(converter = AdminRoleConverter.class)
    @Column(name = "role", nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected AdminEntity() {}

    AdminEntity(UUID id, String fullName, String email, String passwordHash,
                AdminRole role, String status, Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Admin toDomain() {
        return new Admin(id, fullName, email, passwordHash, role, status, createdAt, lastLoginAt);
    }

    public static AdminEntity fromDomain(Admin a) {
        return new AdminEntity(a.id(), a.fullName(), a.email(), a.passwordHash(),
                a.role(), a.status(), a.createdAt(), a.lastLoginAt());
    }
}
