package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.identity.adapter.out.jpa.entity.AdminEntity;
import uz.pulsepay.identity.domain.model.Admin;
import uz.pulsepay.identity.domain.port.out.AdminRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class AdminJpaAdapter implements AdminRepository {

    private final AdminJpaRepository jpa;

    AdminJpaAdapter(AdminJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Admin> findById(UUID id) {
        return jpa.findById(id).map(AdminEntity::toDomain);
    }

    @Override
    public Optional<Admin> findByEmail(String email) {
        return jpa.findByEmail(email).map(AdminEntity::toDomain);
    }

    @Override
    public Admin save(Admin admin) {
        return jpa.save(AdminEntity.fromDomain(admin)).toDomain();
    }
}
