package uz.pulsepay.identity.domain.port.in;

/**
 * Inbound port: user self-registration.
 *
 * <p>The implementing use case is responsible for:
 * <ul>
 *   <li>Rejecting duplicate phone numbers.</li>
 *   <li>Constructing and persisting a new {@link uz.pulsepay.identity.domain.model.User} record.</li>
 *   <li>Returning a signed JWT access token so the client is immediately authenticated
 *       after successful registration.</li>
 * </ul>
 */
public interface RegisterUserPort {

    /**
     * Registers a new user account and issues an access token.
     *
     * @param phoneE164 the E.164-formatted mobile number (unique business key)
     * @param fullName  the user's display name
     * @return a signed JWT access token for the newly created account
     * @throws uz.pulsepay.identity.domain.exception.DuplicatePhoneException
     *         if a registered account already exists for the given phone number
     */
    String register(String phoneE164, String fullName);
}
