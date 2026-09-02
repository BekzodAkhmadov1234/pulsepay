package uz.pulsepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {

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

    protected Device() {}

    public Device(UUID id, UUID userId, String deviceFingerprint, String platform,
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

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public String getPlatform() { return platform; }
    public String getPushToken() { return pushToken; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public boolean isTrusted() { return trusted; }

    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }
    public void setTrusted(boolean trusted) { this.trusted = trusted; }
}
