package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uz.pulsepay.identity.adapter.out.jpa.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhoneE164(String phoneE164);

    @Modifying
    @Query(nativeQuery = true,
           value = "INSERT INTO parties (id, party_type, created_at) VALUES (:id, 'person', NOW()) ON CONFLICT DO NOTHING")
    void upsertParty(@org.springframework.data.repository.query.Param("id") UUID id);
}
