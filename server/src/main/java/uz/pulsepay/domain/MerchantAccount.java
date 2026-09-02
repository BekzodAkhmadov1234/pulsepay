package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.MerchantAccountStatusConverter;
import uz.pulsepay.domain.converter.SettlementScheduleConverter;
import uz.pulsepay.domain.enums.MerchantAccountStatus;
import uz.pulsepay.domain.enums.SettlementSchedule;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_accounts")
public class MerchantAccount {

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

    protected MerchantAccount() {}

    public MerchantAccount(UUID id, UUID merchantId, String currencyCode,
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

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public String getCurrencyCode() { return currencyCode; }
    public long getMinPayoutThreshold() { return minPayoutThreshold; }
    public SettlementSchedule getSettlementSchedule() { return settlementSchedule; }
    public MerchantAccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(MerchantAccountStatus status) { this.status = status; }
    public void setMinPayoutThreshold(long minPayoutThreshold) { this.minPayoutThreshold = minPayoutThreshold; }
    public void setSettlementSchedule(SettlementSchedule settlementSchedule) { this.settlementSchedule = settlementSchedule; }
}
