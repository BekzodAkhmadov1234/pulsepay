package uz.pulsepay.identity.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.domain.identity.CooldownType;
import uz.pulsepay.domain.identity.Device;
import uz.pulsepay.domain.identity.DeviceEntity;
import uz.pulsepay.repository.DeviceRepository;
import uz.pulsepay.repository.RefreshTokenRepository;
import uz.pulsepay.repository.SecurityCooldownRepository;
import uz.pulsepay.repository.SessionRepository;
import uz.pulsepay.service.CardService;
import uz.pulsepay.service.SessionService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 2 MANDATORY tests: new-device detection and card deactivation (DEV-01/DEV-02).
 *
 * MANDATORY rule: "new-device login → linked cards move to INACTIVE" —
 * deactivation must happen inside session creation, not optionally.
 */
class SessionDomainServiceTest {

    private SessionRepository sessionRepo;
    private DeviceRepository deviceRepo;
    private RefreshTokenRepository refreshTokenRepo;
    private SecurityCooldownRepository cooldownRepo;
    private CardService cardService;
    private SessionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String FINGERPRINT = "device-fp-abc123";
    private static final String IP = "192.168.1.1";

    @BeforeEach
    void setUp() {
        sessionRepo      = mock(SessionRepository.class);
        deviceRepo       = mock(DeviceRepository.class);
        refreshTokenRepo = mock(RefreshTokenRepository.class);
        cooldownRepo     = mock(SecurityCooldownRepository.class);
        cardService      = mock(CardService.class);
        service          = new SessionService(sessionRepo, deviceRepo,
                                              refreshTokenRepo, cooldownRepo, cardService);

        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cooldownRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cardService.deactivateCardsOnSecurityEvent(any(), any())).thenReturn(2);
    }

    @Test
    void openSession_new_device_deactivates_cards() {
        when(deviceRepo.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.empty());

        SessionService.SessionOpenResult result =
                service.openSession(USER_ID, FINGERPRINT, "android", IP);

        assertThat(result.isNewDevice()).isTrue();
        verify(cardService).deactivateCardsOnSecurityEvent(eq(USER_ID), anyString());
    }

    @Test
    void openSession_new_device_creates_reactivation_pending_cooldown() {
        when(deviceRepo.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.empty());

        service.openSession(USER_ID, FINGERPRINT, "android", IP);

        verify(cooldownRepo).save(argThat(entity -> {
            var c = entity.toDomain();
            return c.userId().equals(USER_ID) &&
                   c.cooldownType() == CooldownType.CARD_REACTIVATION_PENDING;
        }));
    }

    @Test
    void openSession_known_device_does_not_deactivate_cards() {
        when(deviceRepo.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.of(deviceEntity(UUID.randomUUID(), true)));

        SessionService.SessionOpenResult result =
                service.openSession(USER_ID, FINGERPRINT, "android", IP);

        assertThat(result.isNewDevice()).isFalse();
        verify(cardService, never()).deactivateCardsOnSecurityEvent(any(), any());
        verify(cooldownRepo, never()).save(any());
    }

    @Test
    void openSession_returns_session_with_correct_user() {
        when(deviceRepo.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.of(deviceEntity(UUID.randomUUID(), true)));

        SessionService.SessionOpenResult result =
                service.openSession(USER_ID, FINGERPRINT, "ios", IP);

        assertThat(result.session().userId()).isEqualTo(USER_ID);
    }

    @Test
    void openSession_new_device_is_created_with_trusted_false() {
        when(deviceRepo.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.empty());

        service.openSession(USER_ID, FINGERPRINT, "android", IP);

        verify(deviceRepo).save(argThat(e -> !e.toDomain().trusted()));
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static DeviceEntity deviceEntity(UUID id, boolean trusted) {
        Device d = new Device(id, USER_ID, FINGERPRINT, "android", null,
                Instant.now(), Instant.now(), trusted);
        return DeviceEntity.fromDomain(d);
    }
}
