package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.Admin;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository {
    Optional<Admin> findById(UUID id);
    Optional<Admin> findByEmail(String email);
    Admin save(Admin admin);
}
