package uz.pulsepay.card.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.model.CardStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps the cards table only. instruments row (owner / soft-delete) is managed via InstrumentRepository.
 * cards.id == instruments.id (class-table inheritance).
 */
@Entity
@Table(name = "cards")
public class CardEntity {

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

    protected CardEntity() {}

    public CardEntity(UUID id, String cardToken, String maskedPan, String cardNetwork,
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

    public Card toDomain() {
        return new Card(id, cardToken, maskedPan, cardNetwork, paymentNetworkId, issuerBankId,
                cardHolderName, expMonth, expYear, status, verifiedAt, isDefault,
                isSpecialCardAccount, scaPurpose, balanceTiyin);
    }

    public static CardEntity fromDomain(Card c) {
        return new CardEntity(c.id(), c.cardToken(), c.maskedPan(), c.cardNetwork(),
                c.paymentNetworkId(), c.issuerBankId(), c.cardHolderName(),
                c.expMonth(), c.expYear(), c.status(), c.verifiedAt(),
                c.isDefault(), c.isSpecialCardAccount(), c.scaPurpose(), c.balanceTiyin());
    }
}
