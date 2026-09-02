package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.PartyTypeConverter;
import uz.pulsepay.domain.enums.PartyType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parties")
public class Party {

    @Id
    @Column(name = "id")
    private UUID id;

    @Convert(converter = PartyTypeConverter.class)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Party() {}

    public Party(UUID id, PartyType partyType, Instant createdAt) {
        this.id = id;
        this.partyType = partyType;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public PartyType getPartyType() { return partyType; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPartyType(PartyType partyType) { this.partyType = partyType; }
}
