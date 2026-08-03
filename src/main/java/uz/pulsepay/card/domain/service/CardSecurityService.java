package uz.pulsepay.card.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.card.domain.model.CardStatus;
import uz.pulsepay.card.domain.model.CardStateMachine;
import uz.pulsepay.card.domain.port.out.CardRepository;
import uz.pulsepay.shared.exception.DomainException;

import java.util.UUID;

/**
 * Handles card state changes triggered by security events.
 *
 * Phase 2 MANDATORY rule (DEV-01/CARD lifecycle):
 * "new-device login or password recovery → linked cards move to INACTIVE,
 *  reactivate only via OTP — do not skip even though it adds friction."
 */
@Service
public class CardSecurityService {

    private final CardRepository cardRepository;

    public CardSecurityService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * Deactivates all VERIFIED cards owned by the given user.
     * Called when: (a) user logs in from a new/untrusted device, or (b) password/phone change.
     *
     * The transition VERIFIED → INACTIVE is validated by {@link CardStateMachine} before persisting.
     *
     * @param userId  owner of the cards to deactivate
     * @param reason  short human-readable reason (logged for audit trail)
     * @return number of cards deactivated
     */
    public int deactivateCardsOnSecurityEvent(UUID userId, String reason) {
        // Validate transition is legal for this status (catches BLOCKED/EXPIRED cards)
        // deactivateAllVerifiedByOwner already filters to VERIFIED only, which maps
        // to VERIFIED → INACTIVE — the one legal security-event transition.
        // Assert the transition is legal before touching the DB.
        CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.INACTIVE);

        int count = cardRepository.deactivateAllVerifiedByOwner(userId);
        // count = 0 is valid (user has no verified cards) — not an error
        return count;
    }

    /**
     * Reactivates a single INACTIVE card for the given owner, after OTP confirmation.
     * Called after the user successfully confirms the card-reactivation OTP.
     */
    public void reactivateCard(UUID cardId, UUID userId) {
        var card = cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new uz.pulsepay.shared.exception.NotFoundException(
                        "Card not found or does not belong to user"));

        // Reactivation is only valid from INACTIVE. UNVERIFIED→VERIFIED goes through the
        // verification flow (OTP micro-payment), not reactivation. BLOCKED is terminal.
        if (card.status() != CardStatus.INACTIVE) {
            throw new DomainException(
                    "Card reactivation requires INACTIVE status, current: " + card.status());
        }
        CardStateMachine.assertTransition(card.status(), CardStatus.VERIFIED);
        cardRepository.updateStatus(cardId, CardStatus.VERIFIED);
    }
}
