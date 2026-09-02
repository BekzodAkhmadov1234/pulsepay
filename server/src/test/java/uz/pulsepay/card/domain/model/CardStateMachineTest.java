package uz.pulsepay.card.domain.model;

import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.card.CardStateMachine;
import uz.pulsepay.domain.card.CardStatus;
import uz.pulsepay.domain.shared.DomainException;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 2 MANDATORY tests: card state machine transitions.
 *
 * Critical rule: VERIFIED → INACTIVE must be legal (security event deactivation).
 * INACTIVE → VERIFIED must be legal (OTP reactivation).
 * BLOCKED and EXPIRED are terminal — no transitions allowed.
 */
class CardStateMachineTest {

    // ── Legal transitions ──────────────────────────────────────────────────

    @Test
    void unverified_to_verified_is_legal() {
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.UNVERIFIED, CardStatus.VERIFIED));
    }

    @Test
    void unverified_to_blocked_is_legal() {
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.UNVERIFIED, CardStatus.BLOCKED));
    }

    @Test
    void verified_to_inactive_is_legal_security_event_deactivation() {
        // This is the MANDATORY Phase 2 transition
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.INACTIVE));
    }

    @Test
    void verified_to_blocked_is_legal() {
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.BLOCKED));
    }

    @Test
    void verified_to_expired_is_legal() {
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.EXPIRED));
    }

    @Test
    void inactive_to_verified_is_legal_otp_reactivation() {
        // OTP reactivation path (MANDATORY Phase 2)
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.INACTIVE, CardStatus.VERIFIED));
    }

    @Test
    void inactive_to_blocked_is_legal() {
        assertThatNoException().isThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.INACTIVE, CardStatus.BLOCKED));
    }

    // ── Illegal transitions ─────────────────────────────────────────────────

    @Test
    void unverified_to_inactive_is_illegal() {
        assertThatThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.UNVERIFIED, CardStatus.INACTIVE))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void verified_to_unverified_is_illegal() {
        assertThatThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.VERIFIED, CardStatus.UNVERIFIED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void inactive_to_unverified_is_illegal() {
        assertThatThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.INACTIVE, CardStatus.UNVERIFIED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void inactive_to_expired_is_illegal() {
        assertThatThrownBy(() ->
                CardStateMachine.assertTransition(CardStatus.INACTIVE, CardStatus.EXPIRED))
                .isInstanceOf(DomainException.class);
    }

    // ── Terminal states ──────────────────────────────────────────────────────

    @Test
    void blocked_is_terminal_no_transitions_allowed() {
        for (CardStatus next : CardStatus.values()) {
            assertThat(CardStateMachine.isLegal(CardStatus.BLOCKED, next))
                    .as("BLOCKED → %s should be illegal", next)
                    .isFalse();
        }
    }

    @Test
    void expired_is_terminal_no_transitions_allowed() {
        for (CardStatus next : CardStatus.values()) {
            assertThat(CardStateMachine.isLegal(CardStatus.EXPIRED, next))
                    .as("EXPIRED → %s should be illegal", next)
                    .isFalse();
        }
    }

    @Test
    void blocked_and_expired_are_detected_as_terminal() {
        assertThat(CardStateMachine.isTerminal(CardStatus.BLOCKED)).isTrue();
        assertThat(CardStateMachine.isTerminal(CardStatus.EXPIRED)).isTrue();
    }

    @Test
    void non_terminal_statuses_are_not_terminal() {
        assertThat(CardStateMachine.isTerminal(CardStatus.UNVERIFIED)).isFalse();
        assertThat(CardStateMachine.isTerminal(CardStatus.VERIFIED)).isFalse();
        assertThat(CardStateMachine.isTerminal(CardStatus.INACTIVE)).isFalse();
    }
}
