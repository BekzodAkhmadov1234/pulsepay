package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.InstrumentStatusConverter;
import uz.pulsepay.domain.converter.InstrumentTypeConverter;
import uz.pulsepay.domain.enums.InstrumentStatus;
import uz.pulsepay.domain.enums.InstrumentType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "owner_party_id", nullable = false)
    private UUID ownerPartyId;

    @Convert(converter = InstrumentTypeConverter.class)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType;

    @Convert(converter = InstrumentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private InstrumentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected Instrument() {}

    public Instrument(UUID id, UUID ownerPartyId, InstrumentType instrumentType,
                      InstrumentStatus status, Instant createdAt, Instant removedAt) {
        this.id = id;
        this.ownerPartyId = ownerPartyId;
        this.instrumentType = instrumentType;
        this.status = status;
        this.createdAt = createdAt;
        this.removedAt = removedAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerPartyId() { return ownerPartyId; }
    public InstrumentType getInstrumentType() { return instrumentType; }
    public InstrumentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRemovedAt() { return removedAt; }

    public void setStatus(InstrumentStatus status) { this.status = status; }
    public void setRemovedAt(Instant removedAt) { this.removedAt = removedAt; }
}
