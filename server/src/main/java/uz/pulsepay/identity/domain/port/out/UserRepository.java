package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByPhoneE164(String phoneE164);

    User save(User user);

    /**
     * Returns {@code true} if any account (active or closed) is already registered
     * for the given phone number.
     *
     * <p>Default implementation delegates to {@link #findByPhoneE164(String)} so that
     * existing JPA adapters do not need to be changed. Adapters may override this with
     * a {@code SELECT COUNT(*)} query for better performance at high cardinality.
     */
    default boolean existsByPhoneE164(String phoneE164) {
        return findByPhoneE164(phoneE164).isPresent();
    }
}
