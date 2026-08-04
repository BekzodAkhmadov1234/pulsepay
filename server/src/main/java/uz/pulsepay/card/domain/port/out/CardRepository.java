package uz.pulsepay.card.domain.port.out;

import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.model.CardStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository {
    Card save(Card card);
    Optional<Card> findById(UUID id);

    /** Excludes soft-deleted (instruments.removed_at IS NOT NULL) cards */
    List<Card> findByOwnerUserId(UUID userId);

    Optional<Card> findByIdAndOwnerUserId(UUID cardId, UUID userId);

    /**
     * Atomically updates the status of a single card, enforcing the state machine transition.
     * Returns the updated card.
     */
    Card updateStatus(UUID cardId, CardStatus newStatus);

    /**
     * Deactivates all VERIFIED cards for the given user (moves them to INACTIVE).
     * Called on new-device login or password/phone change — MANDATORY per Phase 2.
     *
     * @return the number of cards deactivated
     */
    int deactivateAllVerifiedByOwner(UUID userId);
}
