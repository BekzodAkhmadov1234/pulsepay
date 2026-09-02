package uz.pulsepay.utils;

import uz.pulsepay.domain.enums.CardStatus;
import uz.pulsepay.domain.shared.DomainException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Explicit card state machine (Phase 2 MANDATORY).
 *
 * Allowed transitions:
 *   UNVERIFIED → VERIFIED  (OTP verification completed)
 *   UNVERIFIED → BLOCKED   (admin/fraud block before verification)
 *   VERIFIED   → INACTIVE  (security event: new-device login or password/phone change — MANDATORY)
 *   VERIFIED   → BLOCKED   (fraud/admin block)
 *   VERIFIED   → EXPIRED   (card expired)
 *   INACTIVE   → VERIFIED  (OTP reactivation by the cardholder)
 *   INACTIVE   → BLOCKED   (escalation)
 *   BLOCKED    → (terminal — no further transitions allowed)
 *   EXPIRED    → (terminal — no further transitions allowed)
 */
public final class CardStateMachine {

    private static final Map<CardStatus, Set<CardStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(CardStatus.class);
        ALLOWED.put(CardStatus.UNVERIFIED, EnumSet.of(CardStatus.VERIFIED, CardStatus.BLOCKED));
        ALLOWED.put(CardStatus.VERIFIED,   EnumSet.of(CardStatus.INACTIVE, CardStatus.BLOCKED, CardStatus.EXPIRED));
        ALLOWED.put(CardStatus.INACTIVE,   EnumSet.of(CardStatus.VERIFIED, CardStatus.BLOCKED));
        ALLOWED.put(CardStatus.BLOCKED,    EnumSet.noneOf(CardStatus.class));
        ALLOWED.put(CardStatus.EXPIRED,    EnumSet.noneOf(CardStatus.class));
    }

    private CardStateMachine() {}

    /**
     * Validates that transitioning from {@code current} to {@code next} is legal.
     *
     * @throws DomainException if the transition is not in the allowed set
     */
    public static void assertTransition(CardStatus current, CardStatus next) {
        Set<CardStatus> allowed = ALLOWED.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new DomainException(
                    "Illegal card status transition: %s → %s".formatted(current, next));
        }
    }

    public static boolean isLegal(CardStatus current, CardStatus next) {
        Set<CardStatus> allowed = ALLOWED.get(current);
        return allowed != null && allowed.contains(next);
    }

    public static boolean isTerminal(CardStatus status) {
        Set<CardStatus> allowed = ALLOWED.get(status);
        return allowed != null && allowed.isEmpty();
    }
}
