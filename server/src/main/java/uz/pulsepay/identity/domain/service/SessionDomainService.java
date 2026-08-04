package uz.pulsepay.identity.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.identity.domain.model.*;
import uz.pulsepay.shared.domain.port.CardDeactivationPort;
import uz.pulsepay.identity.domain.port.out.DeviceRepository;
import uz.pulsepay.identity.domain.port.out.SecurityCooldownRepository;
import uz.pulsepay.identity.domain.port.out.SessionRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Session lifecycle — open, detect new device, trigger security events.
 *
 * Phase 2 MANDATORY (DEV-01/DEV-02):
 * "Device trust tracking and new-device detection — when a new device is first seen,
 *  all linked cards move to INACTIVE and a CARD_REACTIVATION_PENDING cooldown is created."
 */
@Service
public class SessionDomainService {

    // TODO(platform-decision): session TTL is a platform decision — confirm with product.
    private static final long SESSION_TTL_SECONDS = 900;

    private final SessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final CardDeactivationPort cardDeactivationPort;
    private final SecurityCooldownRepository cooldownRepository;

    public SessionDomainService(SessionRepository sessionRepository,
                                DeviceRepository deviceRepository,
                                CardDeactivationPort cardDeactivationPort,
                                SecurityCooldownRepository cooldownRepository) {
        this.sessionRepository    = sessionRepository;
        this.deviceRepository     = deviceRepository;
        this.cardDeactivationPort = cardDeactivationPort;
        this.cooldownRepository   = cooldownRepository;
    }

    /**
     * Opens a session for the given user/device.
     *
     * If this is the first time this device fingerprint is seen (new device), it:
     *   1. Creates the device record (trusted=false)
     *   2. Deactivates all VERIFIED cards (MANDATORY security event — Phase 2)
     *   3. Creates a CARD_REACTIVATION_PENDING cooldown so the caller knows to prompt OTP
     *
     * @param userId            the authenticated user
     * @param deviceFingerprint OS device fingerprint
     * @param platform          ios or android
     * @param ipAddress         client IP for audit
     * @return the new session
     */
    public SessionOpenResult openSession(UUID userId, String deviceFingerprint,
                                         String platform, String ipAddress) {
        Optional<Device> existingDevice = deviceRepository.findByUserIdAndFingerprint(userId, deviceFingerprint);
        boolean isNewDevice = existingDevice.isEmpty();

        Device device = existingDevice.orElseGet(() -> deviceRepository.save(new Device(
                UUID.randomUUID(), userId, deviceFingerprint,
                platform, null, Instant.now(), Instant.now(), false)));

        // Update last_seen_at for existing devices
        if (!isNewDevice) {
            device = deviceRepository.save(new Device(
                    device.id(), device.userId(), device.deviceFingerprint(),
                    device.platform(), device.pushToken(),
                    device.firstSeenAt(), Instant.now(), device.trusted()));
        }

        Session session = sessionRepository.save(new Session(
                UUID.randomUUID(), userId, device.id(),
                ipAddress, Instant.now(),
                Instant.now().plusSeconds(SESSION_TTL_SECONDS), null));

        if (isNewDevice) {
            // MANDATORY Phase 2: deactivate all VERIFIED cards for this user
            int deactivated = cardDeactivationPort.deactivateCardsOnSecurityEvent(
                    userId, "new-device-login:" + device.id());

            // Create a reactivation-pending cooldown (indefinite — cleared when user OTPs each card)
            cooldownRepository.save(new SecurityCooldown(
                    UUID.randomUUID(), userId, CooldownType.CARD_REACTIVATION_PENDING,
                    Instant.now().plusSeconds(86400 * 30L),  // 30-day window for reactivation
                    "new device detected, " + deactivated + " card(s) deactivated",
                    Instant.now()));
        }

        return new SessionOpenResult(session, device, isNewDevice);
    }

    /**
     * Returns value from {@link #openSession} — bundles session, device, and new-device flag
     * so the caller can decide whether to prompt biometric step-up (AUTH-02).
     */
    public record SessionOpenResult(Session session, Device device, boolean isNewDevice) {}
}
