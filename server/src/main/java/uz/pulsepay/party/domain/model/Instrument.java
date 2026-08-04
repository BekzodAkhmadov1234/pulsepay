package uz.pulsepay.party.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Instrument(
        UUID id,
        UUID ownerPartyId,
        InstrumentType instrumentType,
        InstrumentStatus status,
        Instant createdAt,
        Instant removedAt
) {
    public boolean isUsable() {
        return status == InstrumentStatus.ACTIVE && removedAt == null;
    }
}
