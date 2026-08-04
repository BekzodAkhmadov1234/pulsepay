package uz.pulsepay.card.adapter.in;

import org.springframework.stereotype.Component;
import uz.pulsepay.card.domain.service.CardSecurityService;
import uz.pulsepay.shared.domain.port.CardDeactivationPort;

import java.util.UUID;

/**
 * Implements the shared CardDeactivationPort so the identity module can trigger
 * card deactivation on security events without importing the card module directly.
 */
@Component
public class CardDeactivationAdapter implements CardDeactivationPort {

    private final CardSecurityService cardSecurityService;

    public CardDeactivationAdapter(CardSecurityService cardSecurityService) {
        this.cardSecurityService = cardSecurityService;
    }

    @Override
    public int deactivateCardsOnSecurityEvent(UUID userId, String reason) {
        return cardSecurityService.deactivateCardsOnSecurityEvent(userId, reason);
    }
}
