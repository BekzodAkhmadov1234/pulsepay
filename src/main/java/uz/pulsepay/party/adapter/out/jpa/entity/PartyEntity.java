package uz.pulsepay.party.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.party.domain.model.Party;
import uz.pulsepay.party.domain.model.PartyType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parties")
public class PartyEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PartyEntity() {}

    public PartyEntity(UUID id, PartyType partyType, Instant createdAt) {
        this.id = id;
        this.partyType = partyType;
        this.createdAt = createdAt;
    }

    public Party toDomain() {
        return new Party(id, partyType, createdAt);
    }

    public static PartyEntity fromDomain(Party p) {
        return new PartyEntity(p.id(), p.partyType(), p.createdAt());
    }
}
