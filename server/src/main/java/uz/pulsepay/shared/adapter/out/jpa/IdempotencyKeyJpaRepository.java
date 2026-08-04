package uz.pulsepay.shared.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying
    @Query("UPDATE IdempotencyKeyEntity e SET e.responseSnapshot = :snapshot WHERE e.key = :key")
    void updateResponseSnapshot(@Param("key") String key, @Param("snapshot") String snapshot);
}
