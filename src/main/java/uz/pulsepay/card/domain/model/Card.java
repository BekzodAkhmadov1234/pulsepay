package uz.pulsepay.card.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Subtype of instruments (instrument_type='card').
 * card.id == instruments.id — class-table inheritance.
 * Ownership is via instruments.owner_party_id.
 */
public record Card(
        UUID id,
        String cardToken,
        String maskedPan,
        String cardNetwork,
        Integer paymentNetworkId,
        UUID issuerBankId,
        String cardHolderName,
        short expMonth,
        short expYear,
        CardStatus status,
        Instant verifiedAt,
        boolean isDefault,
        boolean isSpecialCardAccount,
        String scaPurpose
) {
    public boolean isUsable() {
        return status == CardStatus.VERIFIED;
    }
}
