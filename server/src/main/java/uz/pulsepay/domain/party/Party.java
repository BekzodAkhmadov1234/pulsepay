package uz.pulsepay.domain.party;

import java.time.Instant;
import java.util.UUID;

public record Party(UUID id, PartyType partyType, Instant createdAt) {}
