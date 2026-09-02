package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.card.CardEntity;
import uz.pulsepay.domain.card.Card;
import uz.pulsepay.domain.card.CardStateMachine;
import uz.pulsepay.domain.card.CardStatus;
import uz.pulsepay.repository.CardRepository;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CardService {

    /** Default stub balance: 50 000 000 UZS in tiyin. Well above the 30M UZS per-tx limit. */
    private static final long DEFAULT_BALANCE_TIYIN = 5_000_000_000L;

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    // ── AddCard ──────────────────────────────────────────────────────────────

    @Transactional
    public Card addCard(UUID userId, String cardToken, String maskedPan,
                        String cardNetwork, String cardHolderName,
                        short expMonth, short expYear) {
        log.info("Add card attempt: userId={}, network={}", userId, cardNetwork);

        String resolvedNetwork = resolveNetwork(maskedPan, cardNetwork);
        boolean isFirst = cardRepository.findByOwnerUserId(userId).isEmpty();
        Instant now = Instant.now();

        Card card = new Card(
                UUID.randomUUID(), cardToken, maskedPan, resolvedNetwork,
                null, null, cardHolderName, expMonth, expYear,
                CardStatus.VERIFIED, now, isFirst, false, null, DEFAULT_BALANCE_TIYIN);

        // Insert instrument row first (cards.id == instruments.id FK)
        cardRepository.insertInstrument(card.id(), userId);
        Card saved = cardRepository.save(CardEntity.fromDomain(card)).toDomain();

        log.info("Card added: cardId={}, userId={}, network={}, isDefault={}",
                saved.id(), userId, resolvedNetwork, isFirst);
        return saved;
    }

    // ── ListCards ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Card> listCards(UUID userId) {
        return cardRepository.findByOwnerUserId(userId)
                .stream().map(CardEntity::toDomain).toList();
    }

    // ── RemoveCard ────────────────────────────────────────────────────────────

    @Transactional
    public void removeCard(UUID cardId, UUID requestingUserId) {
        cardRepository.findByIdAndOwnerUserId(cardId, requestingUserId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        cardRepository.softDelete(cardId);
        log.info("Card removed: cardId={}, userId={}", cardId, requestingUserId);
    }

    // ── SetDefault ────────────────────────────────────────────────────────────

    @Transactional
    public Card setDefault(UUID cardId, UUID userId) {
        cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found or does not belong to user: " + cardId));
        cardRepository.clearDefaultForUser(userId);
        cardRepository.markDefault(cardId);
        Card card = cardRepository.findById(cardId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        log.info("Default card changed: cardId={}, userId={}", cardId, userId);
        return card;
    }

    // ── Security: deactivate / reactivate ─────────────────────────────────────

    /**
     * Deactivates all VERIFIED cards for the user (on security event).
     * Phase 2 MANDATORY: new-device login or password/phone change.
     */
    @Transactional
    public int deactivateCardsOnSecurityEvent(UUID userId, String reason) {
        // Validate the transition is legal before touching DB
        CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.INACTIVE);
        int count = cardRepository.deactivateAllVerifiedByOwner(userId);
        log.info("Deactivated {} card(s) for userId={}, reason={}", count, userId, reason);
        return count;
    }

    /**
     * Reactivates a single INACTIVE card for the given owner (after OTP confirmation).
     */
    @Transactional
    public void reactivateCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException(
                        "Card not found or does not belong to user"));

        if (card.status() != CardStatus.INACTIVE) {
            throw new DomainException(
                    "Card reactivation requires INACTIVE status, current: " + card.status());
        }
        CardStateMachine.assertTransition(card.status(), CardStatus.VERIFIED);
        cardRepository.updateStatus(cardId, CardStatus.VERIFIED);
        log.info("Card reactivated: cardId={}, userId={}", cardId, userId);
    }

    // ── Recipients ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Card> findByMaskedPanPattern(String first6, String last4) {
        return cardRepository.findByMaskedPanPattern(first6, last4)
                .stream().map(CardEntity::toDomain).toList();
    }

    public UUID findOwnerIdByCardId(UUID cardId) {
        return cardRepository.findOwnerIdByCardId(cardId)
                .orElseThrow(() -> new NotFoundException("Card owner not found"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolveNetwork(String maskedPan, String cardNetwork) {
        if (cardNetwork != null && !cardNetwork.isBlank()) {
            return cardNetwork.toLowerCase();
        }
        if (maskedPan != null) {
            if (maskedPan.startsWith("8600")
                    || maskedPan.startsWith("5614")
                    || maskedPan.startsWith("6262")) return "uzcard";
            if (maskedPan.startsWith("9860")) return "humo";
        }
        return null;
    }
}
