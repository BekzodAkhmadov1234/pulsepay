package uz.pulsepay.shared.domain.port;

import java.util.UUID;

/**
 * Outbound port: deactivate cards in response to a security event.
 *
 * Defined in shared so identity (which only imports shared) can call it
 * and card (which also imports shared) can implement it — avoiding a
 * direct identity → card dependency.
 */
public interface CardDeactivationPort {

    /**
     * Deactivates all VERIFIED cards for the given user.
     *
     * @param userId the user whose cards to deactivate
     * @param reason audit reason (e.g. "new-device-login:&lt;deviceId&gt;")
     * @return number of cards deactivated
     */
    int deactivateCardsOnSecurityEvent(UUID userId, String reason);
}
