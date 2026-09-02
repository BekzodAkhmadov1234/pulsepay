package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.identity.DeviceEntity;
import uz.pulsepay.domain.identity.RefreshTokenEntity;
import uz.pulsepay.domain.identity.SecurityCooldownEntity;
import uz.pulsepay.domain.identity.SessionEntity;
import uz.pulsepay.domain.identity.CooldownType;
import uz.pulsepay.domain.identity.Device;
import uz.pulsepay.domain.identity.RefreshToken;
import uz.pulsepay.domain.identity.SecurityCooldown;
import uz.pulsepay.domain.identity.Session;
import uz.pulsepay.repository.DeviceRepository;
import uz.pulsepay.repository.RefreshTokenRepository;
import uz.pulsepay.repository.SecurityCooldownRepository;
import uz.pulsepay.repository.SessionRepository;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private static final long SESSION_TTL_SECONDS = 900;

    private final SessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityCooldownRepository cooldownRepository;
    private final CardService cardService;

    public SessionService(SessionRepository sessionRepository,
                          DeviceRepository deviceRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          SecurityCooldownRepository cooldownRepository,
                          CardService cardService) {
        this.sessionRepository     = sessionRepository;
        this.deviceRepository      = deviceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cooldownRepository    = cooldownRepository;
        this.cardService           = cardService;
    }

    // ── Open Session (from SessionDomainService) ──────────────────────────────

    /**
     * Opens a session for the given user/device.
     * If new device: deactivates VERIFIED cards (MANDATORY Phase 2) and creates a cooldown.
     */
    @Transactional
    public SessionOpenResult openSession(UUID userId, String deviceFingerprint,
                                         String platform, String ipAddress) {
        Optional<DeviceEntity> existingOpt =
                deviceRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
        boolean isNewDevice = existingOpt.isEmpty();
        Instant now = Instant.now();

        DeviceEntity deviceEntity;
        if (isNewDevice) {
            Device newDevice = new Device(UUID.randomUUID(), userId, deviceFingerprint,
                    platform, null, now, now, false);
            deviceEntity = deviceRepository.save(DeviceEntity.fromDomain(newDevice));
        } else {
            Device d = existingOpt.get().toDomain();
            Device updated = new Device(d.id(), d.userId(), d.deviceFingerprint(),
                    d.platform(), d.pushToken(), d.firstSeenAt(), now, d.trusted());
            deviceEntity = deviceRepository.save(DeviceEntity.fromDomain(updated));
        }

        Device device = deviceEntity.toDomain();
        Session session = sessionRepository.save(SessionEntity.fromDomain(new Session(
                UUID.randomUUID(), userId, device.id(), ipAddress,
                now, now.plusSeconds(SESSION_TTL_SECONDS), null))).toDomain();

        if (isNewDevice) {
            int deactivated = cardService.deactivateCardsOnSecurityEvent(
                    userId, "new-device-login:" + device.id());

            cooldownRepository.save(SecurityCooldownEntity.fromDomain(new SecurityCooldown(
                    UUID.randomUUID(), userId, CooldownType.CARD_REACTIVATION_PENDING,
                    now.plusSeconds(86400 * 30L),
                    "new device detected, " + deactivated + " card(s) deactivated",
                    now)));
        }

        return new SessionOpenResult(session, device, isNewDevice);
    }

    public record SessionOpenResult(Session session, Device device, boolean isNewDevice) {}

    // ── Refresh Session ───────────────────────────────────────────────────────

    @Transactional
    public Session refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .map(RefreshTokenEntity::toDomain)
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new DomainException("Invalid or expired refresh token"));
        return sessionRepository.findById(token.sessionId())
                .map(SessionEntity::toDomain)
                .filter(Session::isValid)
                .orElseThrow(() -> new DomainException("Associated session is revoked or expired"));
    }

    // ── Revoke Session ────────────────────────────────────────────────────────

    @Transactional
    public void revoke(UUID sessionId, UUID requestingUserId) {
        Session session = sessionRepository.findById(sessionId)
                .map(SessionEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!session.userId().equals(requestingUserId)) {
            throw new DomainException("Cannot revoke another user's session");
        }
        Session revoked = new Session(session.id(), session.userId(), session.deviceId(),
                session.ipAddress(), session.createdAt(), session.expiresAt(), Instant.now());
        sessionRepository.save(SessionEntity.fromDomain(revoked));
        refreshTokenRepository.revokeAllForSession(sessionId, Instant.now());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
