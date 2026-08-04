package uz.pulsepay.transfer.domain.model;

import uz.pulsepay.shared.exception.DomainException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Explicit transfer state machine.
 *
 * Phase 0 requirement: "implement each state machine as an explicit, enforced set of
 * allowed transitions — not an unvalidated status string. An illegal transition must
 * raise/reject, not silently succeed."
 *
 * Allowed transitions:
 *   INITIATED   → OTP_PENDING | PROCESSING | FAILED
 *   OTP_PENDING → PROCESSING | FAILED
 *   PROCESSING  → COMPLETED | FAILED
 *   COMPLETED   → REVERSED
 *   FAILED      → (terminal)
 *   REVERSED    → (terminal)
 */
public final class TransferStateMachine {

    private static final Map<TransferStatus, Set<TransferStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(TransferStatus.class);
        ALLOWED.put(TransferStatus.INITIATED,   EnumSet.of(TransferStatus.OTP_PENDING,
                                                            TransferStatus.PROCESSING,
                                                            TransferStatus.FAILED));
        ALLOWED.put(TransferStatus.OTP_PENDING, EnumSet.of(TransferStatus.PROCESSING,
                                                            TransferStatus.FAILED));
        ALLOWED.put(TransferStatus.PROCESSING,  EnumSet.of(TransferStatus.COMPLETED,
                                                            TransferStatus.FAILED));
        ALLOWED.put(TransferStatus.COMPLETED,   EnumSet.of(TransferStatus.REVERSED));
        ALLOWED.put(TransferStatus.FAILED,      EnumSet.noneOf(TransferStatus.class));
        ALLOWED.put(TransferStatus.REVERSED,    EnumSet.noneOf(TransferStatus.class));
    }

    private TransferStateMachine() {}

    /**
     * Validates that transitioning from {@code current} to {@code next} is legal.
     *
     * @throws DomainException if the transition is not in the allowed set
     */
    public static void assertTransition(TransferStatus current, TransferStatus next) {
        Set<TransferStatus> allowed = ALLOWED.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new DomainException(
                    "Illegal transfer status transition: %s → %s".formatted(current, next));
        }
    }

    /**
     * Returns true iff the transition is legal (non-throwing variant).
     */
    public static boolean isLegal(TransferStatus current, TransferStatus next) {
        Set<TransferStatus> allowed = ALLOWED.get(current);
        return allowed != null && allowed.contains(next);
    }

    /**
     * Returns true iff the given status is a terminal state (no further transitions allowed).
     */
    public static boolean isTerminal(TransferStatus status) {
        Set<TransferStatus> allowed = ALLOWED.get(status);
        return allowed != null && allowed.isEmpty();
    }
}
