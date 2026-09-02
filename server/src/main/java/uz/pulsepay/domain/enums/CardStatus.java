package uz.pulsepay.domain.enums;

public enum CardStatus {
    /** Card added but OTP verification not yet completed. */
    UNVERIFIED,
    /** Card verified and available for use. */
    VERIFIED,
    /**
     * Deactivated by a security event (new-device login or password/phone change).
     * Reactivatable via OTP. MANDATORY per business rules Phase 2.
     */
    INACTIVE,
    /** Card expired — terminal state. */
    EXPIRED,
    /** Card hard-blocked by fraud/admin — terminal state. */
    BLOCKED
}
