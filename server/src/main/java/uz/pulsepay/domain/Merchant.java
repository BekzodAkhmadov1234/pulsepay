package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.KybStatusConverter;
import uz.pulsepay.domain.converter.MerchantStatusConverter;
import uz.pulsepay.domain.enums.KybStatus;
import uz.pulsepay.domain.enums.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "legal_trade_name", nullable = false)
    private String legalTradeName;

    @Column(name = "merchant_category_id")
    private Integer categoryId;

    @Column(name = "acquiring_bank_id")
    private UUID acquiringBankId;

    @Convert(converter = KybStatusConverter.class)
    @Column(name = "kyb_status", nullable = false, length = 15)
    private KybStatus kybStatus;

    @Convert(converter = MerchantStatusConverter.class)
    @Column(name = "status", nullable = false, length = 15)
    private MerchantStatus status;

    @Column(name = "uzqr_enabled", nullable = false)
    private boolean uzqrEnabled;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Merchant() {}

    public Merchant(UUID id, String legalTradeName, Integer categoryId, UUID acquiringBankId,
                    KybStatus kybStatus, MerchantStatus status, boolean uzqrEnabled,
                    String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.legalTradeName = legalTradeName;
        this.categoryId = categoryId;
        this.acquiringBankId = acquiringBankId;
        this.kybStatus = kybStatus;
        this.status = status;
        this.uzqrEnabled = uzqrEnabled;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getLegalTradeName() { return legalTradeName; }
    public Integer getCategoryId() { return categoryId; }
    public UUID getAcquiringBankId() { return acquiringBankId; }
    public KybStatus getKybStatus() { return kybStatus; }
    public MerchantStatus getStatus() { return status; }
    public boolean isUzqrEnabled() { return uzqrEnabled; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }

    public void setLegalTradeName(String legalTradeName) { this.legalTradeName = legalTradeName; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public void setAcquiringBankId(UUID acquiringBankId) { this.acquiringBankId = acquiringBankId; }
    public void setKybStatus(KybStatus kybStatus) { this.kybStatus = kybStatus; }
    public void setStatus(MerchantStatus status) { this.status = status; }
    public void setUzqrEnabled(boolean uzqrEnabled) { this.uzqrEnabled = uzqrEnabled; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
