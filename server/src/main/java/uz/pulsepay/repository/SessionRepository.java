package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.identity.SessionEntity;

import java.util.UUID;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
}
