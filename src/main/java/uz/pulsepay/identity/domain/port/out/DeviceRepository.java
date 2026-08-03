package uz.pulsepay.identity.domain.port.out;

import uz.pulsepay.identity.domain.model.Device;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository {
    Optional<Device> findByUserIdAndFingerprint(UUID userId, String fingerprint);
    Device save(Device device);
}
