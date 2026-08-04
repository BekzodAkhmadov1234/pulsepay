package uz.pulsepay.party.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Party(UUID id, PartyType partyType, Instant createdAt) {}
