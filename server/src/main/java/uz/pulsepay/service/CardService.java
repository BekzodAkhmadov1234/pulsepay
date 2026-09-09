package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.card.CardEntity;
import uz.pulsepay.domain.card.Card;
import uz.pulsepay.domain.card.CardStateMachine;
import uz.pulsepay.domain.card.CardStatus;
import uz.pulsepay.dto.request.SetCardLimitsRequest;
import uz.pulsepay.dto.response.CardLimitDto;
import uz.pulsepay.dto.response.CardLimitTypeDto;
import uz.pulsepay.dto.response.CardStatementEntry;
import uz.pulsepay.repository.CardRepository;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class CardService {

    /** Default stub balance: 50 000 000 UZS in tiyin. Well above the 30M UZS per-tx limit. */
    private static final long DEFAULT_BALANCE_TIYIN = 5_000_000_000L;

    /** Supported HUMO spending-limit types (excludes LIM_MCC1/2 and LIM_CRD_EXTRA_LIMIT). */
    public static final List<CardLimitTypeDto> HUMO_LIMIT_TYPES = List.of(
            new CardLimitTypeDto("LIM_1DAY_CASHLESS",  "CASHLESS",  "Kunlik naqdsiz to'lov",  false),
            new CardLimitTypeDto("LIM_1MON_CASHLESS",  "CASHLESS",  "Oylik naqdsiz to'lov",   false),
            new CardLimitTypeDto("LIM_1DAY_CASH",      "CASH",      "Kunlik naqd pul",         false),
            new CardLimitTypeDto("LIM_1MON_CASH",      "CASH",      "Oylik naqd pul",          false),
            new CardLimitTypeDto("LIM_1DAY_TRANSFER",  "TRANSFER",  "Kunlik o'tkazma",         false),
            new CardLimitTypeDto("LIM_1MON_TRANSFER",  "TRANSFER",  "Oylik o'tkazma",          false),
            new CardLimitTypeDto("LIM_PERIOD_CASHLESS","CASHLESS",  "Davr bo'yicha naqdsiz",   true),
            new CardLimitTypeDto("LIM_PERIOD_CASH",    "CASH",      "Davr bo'yicha naqd pul",  true)
    );

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

    // ── Block / Unblock (user-initiated) ──────────────────────────────────────────

    /**
     * User-initiated soft block: VERIFIED → INACTIVE.
     * The user can reverse this via {@link #unblockCard}.
     */
    @Transactional
    public Card blockCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        if (card.status() != CardStatus.VERIFIED) {
            throw new DomainException(
                    "Only VERIFIED cards can be blocked, current: " + card.status());
        }
        CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.INACTIVE);
        cardRepository.updateStatus(cardId, CardStatus.INACTIVE);
        log.info("Card blocked by user: cardId={}, userId={}", cardId, userId);
        return cardRepository.findById(cardId).map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
    }

    /**
     * User-initiated unblock: INACTIVE → VERIFIED.
     * Only applicable to cards the user blocked themselves; admin-BLOCKED cards cannot be unblocked here.
     */
    @Transactional
    public Card unblockCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        if (card.status() != CardStatus.INACTIVE) {
            throw new DomainException(
                    "Only INACTIVE cards can be unblocked, current: " + card.status());
        }
        CardStateMachine.assertTransition(CardStatus.INACTIVE, CardStatus.VERIFIED);
        cardRepository.updateStatus(cardId, CardStatus.VERIFIED);
        log.info("Card unblocked by user: cardId={}, userId={}", cardId, userId);
        return cardRepository.findById(cardId).map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
    }

    // ── Statement ─────────────────────────────────────────────────────────────

    /**
     * Returns a stub transaction statement for the card.
     * startDate / endDate are accepted but ignored in the stub phase.
     * Replace with a real HUMO/UzCard API call when network integration is wired.
     */
    @Transactional(readOnly = true)
    public List<CardStatementEntry> getStatement(UUID cardId, UUID userId,
                                                  String startDate, String endDate) {
        cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        return generateStubStatement(cardId);
    }

    private List<CardStatementEntry> generateStubStatement(UUID cardId) {
        Random rng = new Random(cardId.getMostSignificantBits());
        String[] descriptions = {
                "Supermarket", "Restoran", "Naqd pul olish", "Online xarid",
                "Kommunal to'lov", "Yoqilg'i", "Kafе", "Do'kon", "Taksi", "Aptek"
        };
        List<CardStatementEntry> entries = new ArrayList<>();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);

        for (int i = 0; i < 12; i++) {
            long daysBack = rng.nextLong(0, 30);
            long amountTiyin = (500L + rng.nextLong(0, 500_000L)) * 100L;
            boolean isCredit = (i == 2); // one top-up in the list
            String date = DateTimeFormatter.ISO_INSTANT.format(
                    now.minusSeconds(daysBack * 86_400L));
            String desc = descriptions[rng.nextInt(descriptions.length)];
            BigDecimal amountUzs = BigDecimal.valueOf(amountTiyin)
                    .divide(BigDecimal.valueOf(100));
            entries.add(new CardStatementEntry(
                    date, desc,
                    isCredit ? amountUzs : amountUzs.negate(),
                    isCredit ? "credit" : "debit"));
        }
        entries.sort(Comparator.comparing(CardStatementEntry::date).reversed());
        return entries;
    }

    // ── Limits ────────────────────────────────────────────────────────────────────

    /** Returns the static list of supported HUMO limit types. */
    public List<CardLimitTypeDto> getLimitTypes() {
        return HUMO_LIMIT_TYPES;
    }

    /**
     * Returns active limits for the card.
     * Stub: returns empty (no real HUMO API integration yet).
     */
    @Transactional(readOnly = true)
    public List<CardLimitDto> getLimits(UUID cardId, UUID userId) {
        cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        log.info("[STUB] getLimits for cardId={}", cardId);
        return List.of();
    }

    /**
     * Sets spending limits on a card.
     * Stub: echoes the requested limits back as confirmation (no real HUMO API integration yet).
     */
    @Transactional
    public List<CardLimitDto> setLimits(UUID cardId, UUID userId,
                                         List<SetCardLimitsRequest.LimitInput> limits) {
        cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        log.info("[STUB] setLimits for cardId={}, count={}", cardId, limits.size());

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return limits.stream().map(l -> {
            String from = l.dateFrom() != null ? l.dateFrom() : today.toString();
            String to   = l.dateTo()   != null ? l.dateTo()   : today.plusMonths(1).toString();
            String name = HUMO_LIMIT_TYPES.stream()
                    .filter(t -> t.code().equals(l.type()))
                    .map(CardLimitTypeDto::name)
                    .findFirst()
                    .orElse(l.type());
            BigDecimal valueUzs = BigDecimal.valueOf(l.valueTiyin())
                    .divide(BigDecimal.valueOf(100));
            return new CardLimitDto(l.type(), name, valueUzs, from, to);
        }).toList();
    }

    /**
     * Removes a single spending limit from a card.
     * Stub: validates ownership and logs (no real HUMO API integration yet).
     */
    @Transactional
    public void removeLimit(UUID cardId, UUID userId, String limitType) {
        cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        log.info("[STUB] removeLimit type={} for cardId={}", limitType, cardId);
    }

    // ── PIN change ────────────────────────────────────────────────────────────

    /**
     * Changes the PIN for a HUMO card.
     * Stub: validates ownership + network, then logs.
     * Replace with real HUMO PIN-change API call.
     */
    @Transactional(readOnly = true)
    public void setPinHumo(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        if (!"humo".equals(card.cardNetwork())) {
            throw new DomainException("PIN change via this endpoint is only for HUMO cards");
        }
        log.info("[STUB] HUMO PIN change for cardId={}", cardId);
    }

    /**
     * Changes the PIN for a UzCard card.
     * Stub: validates ownership + network, then logs.
     * Replace with real UzCard PIN-change API call.
     */
    @Transactional(readOnly = true)
    public void setPinUzcard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndOwnerUserId(cardId, userId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        if (!"uzcard".equals(card.cardNetwork())) {
            throw new DomainException("PIN change via this endpoint is only for UzCard cards");
        }
        log.info("[STUB] UzCard PIN change for cardId={}", cardId);
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
