package uz.pulsepay.domain.identity;

import uz.pulsepay.domain.shared.DomainException;

/**
 * Thrown when OTP verification fails for any of the following reasons:
 * <ul>
 *   <li>The submitted code does not match the stored OTP.</li>
 *   <li>The OTP has expired (validity window exceeded).</li>
 *   <li>The user has exceeded the maximum allowed verification attempts and is locked out.</li>
 * </ul>
 *
 * <p>The original failure reason (from {@link uz.pulsepay.identity.domain.service.OtpDomainService})
 * is preserved in the message so that callers receive a diagnostic hint without disclosing
 * the actual OTP value.
 *
 * <p>Maps to <b>HTTP 400 Bad Request</b> via {@link uz.pulsepay.shared.adapter.in.rest.GlobalExceptionHandler}.
 */
public class InvalidOtpException extends DomainException {

    public InvalidOtpException(String reason) {
        super(reason);
    }
}
