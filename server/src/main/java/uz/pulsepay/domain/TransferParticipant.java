package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.domain.converter.InstrumentTypeConverter;
import uz.pulsepay.domain.converter.ParticipantRoleConverter;
import uz.pulsepay.domain.converter.PartyTypeConverter;
import uz.pulsepay.domain.enums.InstrumentType;
import uz.pulsepay.domain.enums.ParticipantRole;
import uz.pulsepay.domain.enums.PartyType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_participants")
public class TransferParticipant {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Convert(converter = ParticipantRoleConverter.class)
    @Column(name = "role", nullable = false, length = 10)
    private ParticipantRole role;

    @Column(name = "party_id", nullable = false)
    private UUID partyId;

    @Convert(converter = PartyTypeConverter.class)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Convert(converter = InstrumentTypeConverter.class)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TransferParticipant() {}

    public TransferParticipant(UUID id, UUID transferId, ParticipantRole role,
                                UUID partyId, PartyType partyType,
                                UUID instrumentId, InstrumentType instrumentType, Instant createdAt) {
        this.id = id;
        this.transferId = transferId;
        this.role = role;
        this.partyId = partyId;
        this.partyType = partyType;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public ParticipantRole getRole() { return role; }
    public UUID getPartyId() { return partyId; }
    public PartyType getPartyType() { return partyType; }
    public UUID getInstrumentId() { return instrumentId; }
    public InstrumentType getInstrumentType() { return instrumentType; }
    public Instant getCreatedAt() { return createdAt; }
}
