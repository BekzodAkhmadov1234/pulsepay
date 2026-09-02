package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.identity.AdminEntity;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<AdminEntity, UUID> {

    Optional<AdminEntity> findByEmail(String email);
}
