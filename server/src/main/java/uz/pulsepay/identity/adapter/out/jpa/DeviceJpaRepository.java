package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.identity.adapter.out.jpa.entity.DeviceEntity;

import java.util.Optional;
import java.util.UUID;

interface DeviceJpaRepository extends JpaRepository<DeviceEntity, UUID> {
    Optional<DeviceEntity> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint);
}
