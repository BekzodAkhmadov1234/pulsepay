package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.identity.adapter.out.jpa.entity.DeviceEntity;
import uz.pulsepay.identity.domain.model.Device;
import uz.pulsepay.identity.domain.port.out.DeviceRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class DeviceJpaAdapter implements DeviceRepository {

    private final DeviceJpaRepository jpa;

    DeviceJpaAdapter(DeviceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Device> findByUserIdAndFingerprint(UUID userId, String fingerprint) {
        return jpa.findByUserIdAndDeviceFingerprint(userId, fingerprint).map(DeviceEntity::toDomain);
    }

    @Override
    public Device save(Device device) {
        return jpa.save(DeviceEntity.fromDomain(device)).toDomain();
    }
}
