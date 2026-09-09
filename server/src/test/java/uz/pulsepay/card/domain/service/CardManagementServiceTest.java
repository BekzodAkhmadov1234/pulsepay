package uz.pulsepay.card.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.card.Card;
import uz.pulsepay.domain.card.CardEntity;
import uz.pulsepay.domain.card.CardStatus;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.dto.request.SetCardLimitsRequest;
import uz.pulsepay.dto.response.CardLimitDto;
import uz.pulsepay.dto.response.CardLimitTypeDto;
import uz.pulsepay.dto.response.CardStatementEntry;
import uz.pulsepay.repository.CardRepository;
import uz.pulsepay.service.CardService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CardManagementServiceTest {

    private CardRepository cardRepository;
    private CardService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CARD_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        service = new CardService(cardRepository);
    }

    // ── blockCard ──────────────────────────────────────────────────────────────

    @Test
    void blockCard_transitions_verified_to_inactive() {
        Card verified = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(verified)));
        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(card(CARD_ID, CardStatus.INACTIVE, "humo"))));

        Card result = service.blockCard(CARD_ID, USER_ID);

        verify(cardRepository).updateStatus(CARD_ID, CardStatus.INACTIVE);
        assertThat(result.status()).isEqualTo(CardStatus.INACTIVE);
    }

    @Test
    void blockCard_throws_when_card_not_found() {
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.blockCard(CARD_ID, USER_ID))
                .isInstanceOf(NotFoundException.class);

        verify(cardRepository, never()).updateStatus(any(), any());
    }

    @Test
    void blockCard_throws_when_already_inactive() {
        Card inactive = card(CARD_ID, CardStatus.INACTIVE, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(inactive)));

        assertThatThrownBy(() -> service.blockCard(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("INACTIVE");

        verify(cardRepository, never()).updateStatus(any(), any());
    }

    @Test
    void blockCard_throws_when_card_is_blocked_terminal_state() {
        Card blocked = card(CARD_ID, CardStatus.BLOCKED, "uzcard");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(blocked)));

        assertThatThrownBy(() -> service.blockCard(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class);

        verify(cardRepository, never()).updateStatus(any(), any());
    }

    // ── unblockCard ────────────────────────────────────────────────────────────

    @Test
    void unblockCard_transitions_inactive_to_verified() {
        Card inactive = card(CARD_ID, CardStatus.INACTIVE, "uzcard");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(inactive)));
        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(card(CARD_ID, CardStatus.VERIFIED, "uzcard"))));

        Card result = service.unblockCard(CARD_ID, USER_ID);

        verify(cardRepository).updateStatus(CARD_ID, CardStatus.VERIFIED);
        assertThat(result.status()).isEqualTo(CardStatus.VERIFIED);
    }

    @Test
    void unblockCard_throws_when_card_not_inactive() {
        Card verified = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(verified)));

        assertThatThrownBy(() -> service.unblockCard(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("VERIFIED");
    }

    @Test
    void unblockCard_throws_when_card_not_found() {
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unblockCard(CARD_ID, USER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ── getStatement ────────────────────────────────────────────────────────────

    @Test
    void getStatement_returns_stub_transactions_for_owned_card() {
        Card card = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(card)));

        List<CardStatementEntry> entries = service.getStatement(CARD_ID, USER_ID, null, null);

        assertThat(entries).isNotEmpty();
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.date()).isNotBlank();
            assertThat(e.description()).isNotBlank();
            assertThat(e.type()).isIn("debit", "credit");
        });
    }

    @Test
    void getStatement_is_deterministic_for_same_card() {
        Card card = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(card)));

        List<CardStatementEntry> first  = service.getStatement(CARD_ID, USER_ID, null, null);
        List<CardStatementEntry> second = service.getStatement(CARD_ID, USER_ID, null, null);

        assertThat(first).usingRecursiveComparison().isEqualTo(second);
    }

    @Test
    void getStatement_throws_when_card_not_owned() {
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatement(CARD_ID, USER_ID, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    // ── getLimitTypes ────────────────────────────────────────────────────────────

    @Test
    void getLimitTypes_returns_non_empty_list_excluding_mcc_types() {
        List<CardLimitTypeDto> types = service.getLimitTypes();

        assertThat(types).isNotEmpty();
        assertThat(types).noneMatch(t -> t.code().equals("LIM_MCC1"));
        assertThat(types).noneMatch(t -> t.code().equals("LIM_MCC2"));
        assertThat(types).noneMatch(t -> t.code().equals("LIM_CRD_EXTRA_LIMIT"));
    }

    @Test
    void getLimitTypes_daily_and_monthly_types_do_not_require_date_selection() {
        List<CardLimitTypeDto> types = service.getLimitTypes();

        types.stream()
                .filter(t -> t.code().contains("1DAY") || t.code().contains("1MON"))
                .forEach(t -> assertThat(t.selectDate())
                        .as("Daily/monthly type %s should not require date selection", t.code())
                        .isFalse());
    }

    @Test
    void getLimitTypes_period_types_require_date_selection() {
        List<CardLimitTypeDto> types = service.getLimitTypes();

        types.stream()
                .filter(t -> t.code().contains("PERIOD"))
                .forEach(t -> assertThat(t.selectDate())
                        .as("Period type %s should require date selection", t.code())
                        .isTrue());
    }

    // ── getLimits ────────────────────────────────────────────────────────────────

    @Test
    void getLimits_returns_empty_stub_for_owned_card() {
        Card card = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(card)));

        List<CardLimitDto> limits = service.getLimits(CARD_ID, USER_ID);

        assertThat(limits).isEmpty();
    }

    @Test
    void getLimits_throws_when_card_not_owned() {
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLimits(CARD_ID, USER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ── setLimits ─────────────────────────────────────────────────────────────────

    @Test
    void setLimits_echoes_input_as_confirmation() {
        Card card = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(card)));

        var input = List.of(
                new SetCardLimitsRequest.LimitInput("LIM_1DAY_CASHLESS", 5_000_000_00L, null, null),
                new SetCardLimitsRequest.LimitInput("LIM_1MON_CASH", 20_000_000_00L, null, null)
        );

        List<CardLimitDto> result = service.setLimits(CARD_ID, USER_ID, input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo("LIM_1DAY_CASHLESS");
        assertThat(result.get(1).type()).isEqualTo("LIM_1MON_CASH");
    }

    // ── setPinHumo ───────────────────────────────────────────────────────────────

    @Test
    void setPinHumo_succeeds_for_humo_card() {
        Card humo = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(humo)));

        assertThatNoException().isThrownBy(() -> service.setPinHumo(CARD_ID, USER_ID));
    }

    @Test
    void setPinHumo_throws_for_uzcard_card() {
        Card uzcard = card(CARD_ID, CardStatus.VERIFIED, "uzcard");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(uzcard)));

        assertThatThrownBy(() -> service.setPinHumo(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("HUMO");
    }

    // ── setPinUzcard ─────────────────────────────────────────────────────────────

    @Test
    void setPinUzcard_succeeds_for_uzcard_card() {
        Card uzcard = card(CARD_ID, CardStatus.VERIFIED, "uzcard");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(uzcard)));

        assertThatNoException().isThrownBy(() -> service.setPinUzcard(CARD_ID, USER_ID));
    }

    @Test
    void setPinUzcard_throws_for_humo_card() {
        Card humo = card(CARD_ID, CardStatus.VERIFIED, "humo");
        when(cardRepository.findByIdAndOwnerUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(CardEntity.fromDomain(humo)));

        assertThatThrownBy(() -> service.setPinUzcard(CARD_ID, USER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("UzCard");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private static Card card(UUID id, CardStatus status, String network) {
        return new Card(id, "token", "**** 1234", network, null, null,
                "Test User", (short) 12, (short) 27,
                status, null, false, false, null, 0L);
    }
}
