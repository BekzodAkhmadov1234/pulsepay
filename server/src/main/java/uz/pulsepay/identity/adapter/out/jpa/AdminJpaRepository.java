package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.identity.adapter.out.jpa.entity.AdminEntity;

import java.util.Optional;
import java.util.UUID;

interface AdminJpaRepository extends JpaRepository<AdminEntity, UUID> {
    Optional<AdminEntity> findByEmail(String email);
}
