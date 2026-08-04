package uz.pulsepay.identity.adapter.out.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uz.pulsepay.identity.domain.model.Device;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class DeviceEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(name = "platform", length = 10)
    private String platform;

    @Column(name = "push_token")
    private String pushToken;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "trusted", nullable = false)
    private boolean trusted;

    protected DeviceEntity() {}

    DeviceEntity(UUID id, UUID userId, String deviceFingerprint, String platform,
                 String pushToken, Instant firstSeenAt, Instant lastSeenAt, boolean trusted) {
        this.id = id;
        this.userId = userId;
        this.deviceFingerprint = deviceFingerprint;
        this.platform = platform;
        this.pushToken = pushToken;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.trusted = trusted;
    }

    public Device toDomain() {
        return new Device(id, userId, deviceFingerprint, platform, pushToken, firstSeenAt, lastSeenAt, trusted);
    }

    public static DeviceEntity fromDomain(Device d) {
        return new DeviceEntity(d.id(), d.userId(), d.deviceFingerprint(), d.platform(),
                d.pushToken(), d.firstSeenAt(), d.lastSeenAt(), d.trusted());
    }
}
