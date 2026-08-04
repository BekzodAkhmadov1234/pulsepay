package uz.pulsepay.transfer.domain.model;

import uz.pulsepay.party.domain.model.InstrumentType;
import uz.pulsepay.party.domain.model.PartyType;

import java.time.Instant;
import java.util.UUID;

public record TransferParticipant(
        UUID id,
        UUID transferId,
        ParticipantRole role,
        UUID partyId,
        PartyType partyType,
        UUID instrumentId,
        InstrumentType instrumentType,
        Instant createdAt
) {}
