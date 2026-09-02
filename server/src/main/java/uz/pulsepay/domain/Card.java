package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.enums.CardStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps the cards table only. instruments row (owner / soft-delete) is managed via InstrumentRepository.
 * cards.id == instruments.id (class-table inheritance).
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "card_token", nullable = false, length = 128)
    private String cardToken;

    @Column(name = "masked_pan", nullable = false, length = 19)
    private String maskedPan;

    @Column(name = "card_network", length = 10)
    private String cardNetwork;

    @Column(name = "payment_network_id")
    private Integer paymentNetworkId;

    @Column(name = "issuer_bank_id")
    private UUID issuerBankId;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "exp_month", nullable = false)
    private short expMonth;

    @Column(name = "exp_year", nullable = false)
    private short expYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private CardStatus status;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_special_card_account", nullable = false)
    private boolean isSpecialCardAccount;

    @Column(name = "sca_purpose", length = 30)
    private String scaPurpose;

    @Column(name = "balance_tiyin", nullable = false)
    private long balanceTiyin;

    protected Card() {}

    public Card(UUID id, String cardToken, String maskedPan, String cardNetwork,
                Integer paymentNetworkId, UUID issuerBankId, String cardHolderName,
                short expMonth, short expYear, CardStatus status, Instant verifiedAt,
                boolean isDefault, boolean isSpecialCardAccount, String scaPurpose,
                long balanceTiyin) {
        this.id = id;
        this.cardToken = cardToken;
        this.maskedPan = maskedPan;
        this.cardNetwork = cardNetwork;
        this.paymentNetworkId = paymentNetworkId;
        this.issuerBankId = issuerBankId;
        this.cardHolderName = cardHolderName;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.isDefault = isDefault;
        this.isSpecialCardAccount = isSpecialCardAccount;
        this.scaPurpose = scaPurpose;
        this.balanceTiyin = balanceTiyin;
    }

    public UUID getId() { return id; }
    public String getCardToken() { return cardToken; }
    public String getMaskedPan() { return maskedPan; }
    public String getCardNetwork() { return cardNetwork; }
    public Integer getPaymentNetworkId() { return paymentNetworkId; }
    public UUID getIssuerBankId() { return issuerBankId; }
    public String getCardHolderName() { return cardHolderName; }
    public short getExpMonth() { return expMonth; }
    public short getExpYear() { return expYear; }
    public CardStatus getStatus() { return status; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public boolean isDefault() { return isDefault; }
    public boolean isSpecialCardAccount() { return isSpecialCardAccount; }
    public String getScaPurpose() { return scaPurpose; }
    public long getBalanceTiyin() { return balanceTiyin; }

    public void setStatus(CardStatus status) { this.status = status; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public void setBalanceTiyin(long balanceTiyin) { this.balanceTiyin = balanceTiyin; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public void setIssuerBankId(UUID issuerBankId) { this.issuerBankId = issuerBankId; }
    public void setPaymentNetworkId(Integer paymentNetworkId) { this.paymentNetworkId = paymentNetworkId; }
}
