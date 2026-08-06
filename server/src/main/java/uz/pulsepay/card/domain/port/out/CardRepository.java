package uz.pulsepay.card.domain.port.out;

import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.model.CardStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository {
    /** Creates the instruments row (owner, soft-delete) then inserts the card row. */
    Card save(Card card, UUID ownerPartyId);
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

    /** Soft-deletes a card: sets instruments.removed_at = NOW() and instruments.status = 'removed'. */
    void softDelete(UUID cardId);

    /** Finds a non-deleted card whose masked_pan starts with first6 and ends with last4. */
    Optional<Card> findByMaskedPanPattern(String first6, String last4);

    /** Returns the owner party id (instruments.owner_party_id) for a non-deleted card. */
    Optional<UUID> findOwnerIdByCardId(UUID cardId);

    /**
     * Atomically deducts amountTiyin from the card's balance.
     * Throws InsufficientFundsException if balance &lt; amountTiyin.
     */
    void debitBalance(UUID cardId, long amountTiyin);

    /** Atomically adds amountTiyin to the card's balance. */
    void creditBalance(UUID cardId, long amountTiyin);

    /**
     * Atomically clears is_default for all of the user's cards, then marks this card as default.
     * Throws NotFoundException if the card doesn't exist or doesn't belong to the user.
     */
    Card setDefault(UUID cardId, UUID userId);
}
