package uz.pulsepay.identity.domain.exception;

import uz.pulsepay.domain.shared.DomainException;

/**
 * Thrown when a registration attempt supplies a phone number that is already associated
 * with an existing account, whether active or closed.
 *
 * <p>Maps to <b>HTTP 409 Conflict</b> via {@link uz.pulsepay.shared.adapter.in.rest.GlobalExceptionHandler}.
 */
public class DuplicatePhoneException extends DomainException {

    public DuplicatePhoneException(String phoneE164) {
        super("Phone number is already registered: " + phoneE164);
    }
}
