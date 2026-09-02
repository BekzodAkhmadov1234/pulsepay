package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.identity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByPhoneE164(String phoneE164);

    @Modifying
    @Query(nativeQuery = true,
           value = "INSERT INTO parties (id, party_type, created_at) VALUES (:id, 'person', NOW()) ON CONFLICT DO NOTHING")
    void upsertParty(@Param("id") UUID id);
}
