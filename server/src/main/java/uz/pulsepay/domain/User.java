package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

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

    protected User() {}

    public User(UUID id, String phoneE164, String fullName, String status,
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
    public String getPhoneE164() { return phoneE164; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
    public String getKycLevel() { return kycLevel; }
    public Instant getBiometricVerifiedAt() { return biometricVerifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public int getVersion() { return version; }

    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setStatus(String status) { this.status = status; }
    public void setKycLevel(String kycLevel) { this.kycLevel = kycLevel; }
    public void setBiometricVerifiedAt(Instant biometricVerifiedAt) { this.biometricVerifiedAt = biometricVerifiedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public void setVersion(int version) { this.version = version; }
}
