package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByPhoneE164(String phoneE164);
    User save(User user);
}
