package uz.pulsepay.identity.domain.exception;

import uz.pulsepay.shared.exception.DomainException;

/**
 * Thrown when a login or sensitive operation is attempted on an account whose
 * {@link uz.pulsepay.identity.domain.model.User#isActive()} returns {@code false}
 * — i.e. the account status is not {@code "active"} or the {@code closedAt} field is set.
 *
 * <p>Maps to <b>HTTP 403 Forbidden</b> via {@link uz.pulsepay.shared.adapter.in.rest.GlobalExceptionHandler}.
 * Using 403 (rather than 401) signals that the caller is identified but not permitted
 * to act, which is the correct semantic for an inactive account.
 */
public class AccountInactiveException extends DomainException {

    public AccountInactiveException(String phoneE164) {
        super("Account is inactive or closed: " + phoneE164);
    }
}
