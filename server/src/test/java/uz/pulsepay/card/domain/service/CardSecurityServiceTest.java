package uz.pulsepay.card.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.card.Card;
import uz.pulsepay.domain.card.CardEntity;
import uz.pulsepay.domain.card.CardStatus;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.repository.CardRepository;
import uz.pulsepay.service.CardService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 2 MANDATORY tests: card security event deactivation.
 *
 * MANDATORY rule: "new-device login or password recovery → linked cards move to INACTIVE,
 * reactivate only via OTP — do not skip even though it adds friction."
 */
class CardSecurityServiceTest {

    private CardRepository cardRepository;
    private CardService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CARD_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        service = new CardService(cardRepository);
    }

    @Test
    void deactivateCardsOnSecurityEvent_calls_repository_and_returns_count() {
        when(cardRepository.deactivateAllVerifiedByOwner(USER_ID)).thenReturn(3);

        int count = service.deactivateCardsOnSecurityEvent(USER_ID, "new-device");

        assertThat(count).isEqualTo(3);
        verify(cardRepository).deactivateAllVerifiedByOwner(USER_ID);
    }

    @Test
    void deactivateCardsOnSecurityEvent_zero_cards_is_not_an_error() {
        when(cardRepository.deactivateAllVerifiedByOwner(USER_ID)).thenReturn(0);

        assertThatNoException().isThrownBy(() ->
                service.deactivateCardsOnSecurityEvent(USER_ID, "new-device"));
    }

    @Test
    void reactivateCard_transitions_inactive_to_verified() {
        Card inactiveCard = card(CARD_ID, CardStatus.INACTIVE);
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(inactiveCard)));

        service.reactivateCard(CARD_ID, USER_ID);

        verify(cardRepository).updateStatus(CARD_ID, CardStatus.VERIFIED);
    }

    @Test
    void reactivateCard_throws_when_card_not_found() {
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reactivateCard(CARD_ID, USER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reactivateCard_throws_when_card_is_blocked_illegal_transition() {
        Card blockedCard = card(CARD_ID, CardStatus.BLOCKED);
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(blockedCard)));

        // BLOCKED card fails the INACTIVE status check before state machine
        assertThatThrownBy(() -> service.reactivateCard(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("BLOCKED");

        verify(cardRepository, never()).updateStatus(any(), any());
    }

    @Test
    void reactivateCard_throws_when_card_is_unverified_illegal_transition() {
        Card unverifiedCard = card(CARD_ID, CardStatus.UNVERIFIED);
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(unverifiedCard)));

        // UNVERIFIED → VERIFIED goes through verification flow, not reactivation
        assertThatThrownBy(() -> service.reactivateCard(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static Card card(UUID id, CardStatus status) {
        return new Card(id, "token", "**** 1234", "uzcard", null, null,
                "Test User", (short) 12, (short) 27,
                status, null, false, false, null, 0L);
    }
}
