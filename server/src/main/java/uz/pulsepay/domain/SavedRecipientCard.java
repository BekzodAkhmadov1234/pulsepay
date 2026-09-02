package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_recipient_cards")
public class SavedRecipientCard {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "masked_pan", nullable = false, length = 19)
    private String maskedPan;

    @Column(name = "card_token", nullable = false, length = 128)
    private String cardToken;

    @Column(name = "card_network", nullable = false, length = 10)
    private String cardNetwork;

    @Column(name = "label")
    private String label;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SavedRecipientCard() {}

    public SavedRecipientCard(UUID id, UUID ownerUserId, String maskedPan, String cardToken,
                               String cardNetwork, String label, Instant lastUsedAt, Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.maskedPan = maskedPan;
        this.cardToken = cardToken;
        this.cardNetwork = cardNetwork;
        this.label = label;
        this.lastUsedAt = lastUsedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getMaskedPan() { return maskedPan; }
    public String getCardToken() { return cardToken; }
    public String getCardNetwork() { return cardNetwork; }
    public String getLabel() { return label; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setLabel(String label) { this.label = label; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
