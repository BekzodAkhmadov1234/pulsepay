package uz.pulsepay.card.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SavedRecipientCard(
        UUID id,
        UUID ownerUserId,
        String maskedPan,
        String cardToken,
        String cardNetwork,
        String label,
        Instant lastUsedAt,
        Instant createdAt
) {}
