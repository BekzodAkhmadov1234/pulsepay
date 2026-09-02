package uz.pulsepay.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import uz.pulsepay.domain.identity.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "phone_e164", nullable = false, unique = true, length = 20)
    private String phoneE164;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "kyc_level", nullable = false, length = 10)
    private String kycLevel;

    @Column(name = "biometric_verified_at")
    private Instant biometricVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected UserEntity() {}

    public UserEntity(UUID id, String phoneE164, String fullName, String status,
                      String kycLevel, Instant biometricVerifiedAt,
                      Instant createdAt, Instant updatedAt, Instant closedAt) {
        this.id = id;
        this.phoneE164 = phoneE164;
        this.fullName = fullName;
        this.status = status;
        this.kycLevel = kycLevel;
        this.biometricVerifiedAt = biometricVerifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
    }

    public UUID getId() { return id; }

    public User toDomain() {
        return new User(id, phoneE164, fullName, status, kycLevel,
                biometricVerifiedAt, createdAt, updatedAt, closedAt, version);
    }

    public static UserEntity fromDomain(User user) {
        UserEntity e = new UserEntity(user.id(), user.phoneE164(), user.fullName(),
                user.status(), user.kycLevel(), user.biometricVerifiedAt(),
                user.createdAt(), user.updatedAt(), user.closedAt());
        e.version = user.version();
        return e;
    }
}
