package uz.pulsepay.merchant.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.merchant.domain.model.KybStatus;
import uz.pulsepay.merchant.domain.model.Merchant;
import uz.pulsepay.merchant.domain.model.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class MerchantEntity {

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

    protected MerchantEntity() {}

    public MerchantEntity(UUID id, String legalTradeName, Integer categoryId, UUID acquiringBankId,
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

    public Merchant toDomain() {
        return new Merchant(id, legalTradeName, categoryId, acquiringBankId,
                kybStatus, status, uzqrEnabled, email, passwordHash, createdAt);
    }

    public static MerchantEntity fromDomain(Merchant m) {
        return new MerchantEntity(m.id(), m.legalTradeName(), m.categoryId(), m.acquiringBankId(),
                m.kybStatus(), m.status(), m.uzqrEnabled(), m.email(), m.passwordHash(), m.createdAt());
    }
}
