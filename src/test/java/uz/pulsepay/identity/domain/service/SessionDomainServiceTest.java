package uz.pulsepay.identity.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.identity.domain.model.*;
import uz.pulsepay.identity.domain.port.out.DeviceRepository;
import uz.pulsepay.identity.domain.port.out.SecurityCooldownRepository;
import uz.pulsepay.identity.domain.port.out.SessionRepository;
import uz.pulsepay.shared.domain.port.CardDeactivationPort;

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
    private CardDeactivationPort cardDeactivationPort;
    private SecurityCooldownRepository cooldownRepo;
    private SessionDomainService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String FINGERPRINT = "device-fp-abc123";
    private static final String IP = "192.168.1.1";

    @BeforeEach
    void setUp() {
        sessionRepo          = mock(SessionRepository.class);
        deviceRepo           = mock(DeviceRepository.class);
        cardDeactivationPort = mock(CardDeactivationPort.class);
        cooldownRepo         = mock(SecurityCooldownRepository.class);
        service              = new SessionDomainService(sessionRepo, deviceRepo,
                                                        cardDeactivationPort, cooldownRepo);

        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cooldownRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cardDeactivationPort.deactivateCardsOnSecurityEvent(any(), any())).thenReturn(2);
    }

    @Test
    void openSession_new_device_deactivates_cards() {
        // No existing device → new device
        when(deviceRepo.findByUserIdAndFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.empty());

        SessionDomainService.SessionOpenResult result =
                service.openSession(USER_ID, FINGERPRINT, "android", IP);

        assertThat(result.isNewDevice()).isTrue();
        // MANDATORY: cards must be deactivated on new device
        verify(cardDeactivationPort).deactivateCardsOnSecurityEvent(eq(USER_ID), anyString());
    }

    @Test
    void openSession_new_device_creates_reactivation_pending_cooldown() {
        when(deviceRepo.findByUserIdAndFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.empty());

        service.openSession(USER_ID, FINGERPRINT, "android", IP);

        verify(cooldownRepo).save(argThat(c ->
                c.userId().equals(USER_ID) &&
                c.cooldownType() == CooldownType.CARD_REACTIVATION_PENDING));
    }

    @Test
    void openSession_known_device_does_not_deactivate_cards() {
        Device existingDevice = device(UUID.randomUUID(), true);
        when(deviceRepo.findByUserIdAndFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.of(existingDevice));

        SessionDomainService.SessionOpenResult result =
                service.openSession(USER_ID, FINGERPRINT, "android", IP);

        assertThat(result.isNewDevice()).isFalse();
        verify(cardDeactivationPort, never()).deactivateCardsOnSecurityEvent(any(), any());
        verify(cooldownRepo, never()).save(any());
    }

    @Test
    void openSession_returns_session_with_correct_user() {
        when(deviceRepo.findByUserIdAndFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.of(device(UUID.randomUUID(), true)));

        SessionDomainService.SessionOpenResult result =
                service.openSession(USER_ID, FINGERPRINT, "ios", IP);

        assertThat(result.session().userId()).isEqualTo(USER_ID);
    }

    @Test
    void openSession_new_device_is_created_with_trusted_false() {
        when(deviceRepo.findByUserIdAndFingerprint(USER_ID, FINGERPRINT))
                .thenReturn(Optional.empty());

        service.openSession(USER_ID, FINGERPRINT, "android", IP);

        verify(deviceRepo).save(argThat(d -> !d.trusted()));
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static Device device(UUID id, boolean trusted) {
        return new Device(id, USER_ID, FINGERPRINT, "android", null,
                Instant.now(), Instant.now(), trusted);
    }
}
