package uz.pulsepay.domain.merchant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.merchant.MerchantAccount;
import uz.pulsepay.domain.merchant.MerchantAccountStatus;
import uz.pulsepay.domain.merchant.SettlementSchedule;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_accounts")
public class MerchantAccountEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "min_payout_threshold", nullable = false)
    private long minPayoutThreshold;

    @Convert(converter = SettlementScheduleConverter.class)
    @Column(name = "settlement_schedule", nullable = false, length = 15)
    private SettlementSchedule settlementSchedule;

    @Convert(converter = MerchantAccountStatusConverter.class)
    @Column(name = "status", nullable = false, length = 12)
    private MerchantAccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MerchantAccountEntity() {}

    public MerchantAccountEntity(UUID id, UUID merchantId, String currencyCode,
                                  long minPayoutThreshold, SettlementSchedule settlementSchedule,
                                  MerchantAccountStatus status, Instant createdAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.currencyCode = currencyCode;
        this.minPayoutThreshold = minPayoutThreshold;
        this.settlementSchedule = settlementSchedule;
        this.status = status;
        this.createdAt = createdAt;
    }

    public MerchantAccount toDomain() {
        return new MerchantAccount(id, merchantId, currencyCode,
                minPayoutThreshold, settlementSchedule, status, createdAt);
    }

    public static MerchantAccountEntity fromDomain(MerchantAccount a) {
        return new MerchantAccountEntity(a.id(), a.merchantId(), a.currencyCode(),
                a.minPayoutThreshold(), a.settlementSchedule(), a.status(), a.createdAt());
    }
}
