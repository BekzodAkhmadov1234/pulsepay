package uz.pulsepay.domain.transfer;

import uz.pulsepay.domain.party.InstrumentType;
import uz.pulsepay.domain.party.PartyType;

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
