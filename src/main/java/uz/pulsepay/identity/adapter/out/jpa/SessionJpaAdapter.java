package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.identity.adapter.out.jpa.entity.SessionEntity;
import uz.pulsepay.identity.domain.model.Session;
import uz.pulsepay.identity.domain.port.out.SessionRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class SessionJpaAdapter implements SessionRepository {

    private final SessionJpaRepository jpa;

    SessionJpaAdapter(SessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Session> findById(UUID id) {
        return jpa.findById(id).map(SessionEntity::toDomain);
    }

    @Override
    public Session save(Session session) {
        return jpa.save(SessionEntity.fromDomain(session)).toDomain();
    }
}
