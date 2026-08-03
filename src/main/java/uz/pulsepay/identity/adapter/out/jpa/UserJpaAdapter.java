package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.adapter.out.jpa.entity.UserEntity;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.out.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserJpaAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByPhoneE164(String phoneE164) {
        return jpa.findByPhoneE164(phoneE164).map(UserEntity::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        jpa.upsertParty(user.id());
        UserEntity saved = jpa.save(UserEntity.fromDomain(user));
        return saved.toDomain();
    }
}
