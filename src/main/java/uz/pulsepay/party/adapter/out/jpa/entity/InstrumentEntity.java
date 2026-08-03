package uz.pulsepay.party.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.party.domain.model.Instrument;
import uz.pulsepay.party.domain.model.InstrumentStatus;
import uz.pulsepay.party.domain.model.InstrumentType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instruments")
public class InstrumentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "owner_party_id", nullable = false)
    private UUID ownerPartyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstrumentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected InstrumentEntity() {}

    public InstrumentEntity(UUID id, UUID ownerPartyId, InstrumentType instrumentType,
                            InstrumentStatus status, Instant createdAt, Instant removedAt) {
        this.id = id;
        this.ownerPartyId = ownerPartyId;
        this.instrumentType = instrumentType;
        this.status = status;
        this.createdAt = createdAt;
        this.removedAt = removedAt;
    }

    public Instrument toDomain() {
        return new Instrument(id, ownerPartyId, instrumentType, status, createdAt, removedAt);
    }

    public static InstrumentEntity fromDomain(Instrument i) {
        return new InstrumentEntity(i.id(), i.ownerPartyId(), i.instrumentType(),
                i.status(), i.createdAt(), i.removedAt());
    }
}
