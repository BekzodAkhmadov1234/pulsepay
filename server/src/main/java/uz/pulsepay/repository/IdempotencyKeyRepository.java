package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.shared.IdempotencyKeyEntity;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying
    @Query("UPDATE IdempotencyKeyEntity e SET e.responseSnapshot = :snapshot WHERE e.key = :key")
    void updateResponseSnapshot(@Param("key") String key, @Param("snapshot") String snapshot);
}
