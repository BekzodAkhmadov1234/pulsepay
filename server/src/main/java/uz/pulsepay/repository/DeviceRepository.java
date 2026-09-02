package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.identity.DeviceEntity;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    Optional<DeviceEntity> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint);
}
