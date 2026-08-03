package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.identity.adapter.out.jpa.entity.SessionEntity;

import java.util.UUID;

interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {
}
