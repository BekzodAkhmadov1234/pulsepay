package uz.pulsepay.identity.domain.port.in;

/**
 * Inbound port: phone-based login for a registered user.
 *
 * <p>The implementing use case is responsible for:
 * <ul>
 *   <li>Locating the account by phone number.</li>
 *   <li>Verifying the account is active.</li>
 *   <li>Returning a signed JWT access token on success.</li>
 * </ul>
 *
 * <p><strong>Temporary:</strong> OTP verification is intentionally omitted in this phase.
 * The signature will be extended to {@code login(String phoneE164, String otpCode)} once
 * the SMS gateway is integrated.
 */
public interface LoginUserPort {

    /**
     * Authenticates a user by phone number and returns a JWT access token.
     *
     * @param phoneE164 the registered E.164 phone number
     * @return a signed JWT access token valid for the configured user-expiry window
     * @throws uz.pulsepay.shared.exception.NotFoundException
     *         if no account exists for the given phone number
     * @throws uz.pulsepay.identity.domain.exception.AccountInactiveException
     *         if the account exists but is not in {@code active} status
     */
    String login(String phoneE164);
}
