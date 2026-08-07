package uz.pulsepay.identity.domain.port.in;

public interface LoginAdminPort {
    /**
     * Authenticates an admin by email and password.
     *
     * @param email    admin email address
     * @param password plaintext password to verify against stored BCrypt hash
     * @return signed JWT admin token
     */
    String login(String email, String password);
}
