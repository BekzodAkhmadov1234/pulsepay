package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.Session;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    Optional<Session> findById(UUID id);
    Session save(Session session);
}
