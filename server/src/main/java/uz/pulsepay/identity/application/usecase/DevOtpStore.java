package uz.pulsepay.identity.application.usecase;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dev-only in-memory store that holds the most recent raw OTP code per user.
 * Only registered when the {@code dev} Spring profile is active.
 * Exposed via {@link uz.pulsepay.identity.adapter.in.rest.DevController}.
 */
@Component
@Profile("dev")
public class DevOtpStore {

    private final ConcurrentHashMap<UUID, String> store = new ConcurrentHashMap<>();

    public void put(UUID userId, String rawCode) {
        store.put(userId, rawCode);
    }

    public Optional<String> get(UUID userId) {
        return Optional.ofNullable(store.get(userId));
    }
}
