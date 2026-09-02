package uz.pulsepay.transfer.domain.model;

import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.transfer.TransferStateMachine;
import uz.pulsepay.domain.transfer.TransferStatus;
import uz.pulsepay.domain.shared.DomainException;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 0 MANDATORY test: all legal and all illegal transfer status transitions.
 */
class TransferStateMachineTest {

    // ── Legal transitions ──────────────────────────────────────────────────

    @Test
    void initiated_to_otpPending_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.INITIATED, TransferStatus.OTP_PENDING));
    }

    @Test
    void initiated_to_processing_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.INITIATED, TransferStatus.PROCESSING));
    }

    @Test
    void initiated_to_failed_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.INITIATED, TransferStatus.FAILED));
    }

    @Test
    void otpPending_to_processing_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.OTP_PENDING, TransferStatus.PROCESSING));
    }

    @Test
    void otpPending_to_failed_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.OTP_PENDING, TransferStatus.FAILED));
    }

    @Test
    void processing_to_completed_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.PROCESSING, TransferStatus.COMPLETED));
    }

    @Test
    void processing_to_failed_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.PROCESSING, TransferStatus.FAILED));
    }

    @Test
    void completed_to_reversed_is_legal() {
        assertThatNoException().isThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.COMPLETED, TransferStatus.REVERSED));
    }

    // ── Illegal transitions — each must throw DomainException ─────────────

    @Test
    void initiated_to_completed_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.INITIATED, TransferStatus.COMPLETED))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("INITIATED")
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void initiated_to_reversed_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.INITIATED, TransferStatus.REVERSED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void otpPending_to_completed_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.OTP_PENDING, TransferStatus.COMPLETED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void otpPending_to_reversed_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.OTP_PENDING, TransferStatus.REVERSED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void processing_to_otpPending_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.PROCESSING, TransferStatus.OTP_PENDING))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void processing_to_reversed_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.PROCESSING, TransferStatus.REVERSED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void completed_to_processing_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.COMPLETED, TransferStatus.PROCESSING))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void completed_to_failed_is_illegal() {
        assertThatThrownBy(() ->
                TransferStateMachine.assertTransition(TransferStatus.COMPLETED, TransferStatus.FAILED))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void failed_has_no_allowed_transitions() {
        for (TransferStatus next : TransferStatus.values()) {
            assertThat(TransferStateMachine.isLegal(TransferStatus.FAILED, next))
                    .as("FAILED → %s should be illegal", next)
                    .isFalse();
        }
    }

    @Test
    void reversed_has_no_allowed_transitions() {
        for (TransferStatus next : TransferStatus.values()) {
            assertThat(TransferStateMachine.isLegal(TransferStatus.REVERSED, next))
                    .as("REVERSED → %s should be illegal", next)
                    .isFalse();
        }
    }

    // ── Terminal state detection ───────────────────────────────────────────

    @Test
    void failed_and_reversed_are_terminal() {
        assertThat(TransferStateMachine.isTerminal(TransferStatus.FAILED)).isTrue();
        assertThat(TransferStateMachine.isTerminal(TransferStatus.REVERSED)).isTrue();
    }

    @Test
    void non_terminal_states_are_not_terminal() {
        assertThat(TransferStateMachine.isTerminal(TransferStatus.INITIATED)).isFalse();
        assertThat(TransferStateMachine.isTerminal(TransferStatus.OTP_PENDING)).isFalse();
        assertThat(TransferStateMachine.isTerminal(TransferStatus.PROCESSING)).isFalse();
        assertThat(TransferStateMachine.isTerminal(TransferStatus.COMPLETED)).isFalse();
    }
}
