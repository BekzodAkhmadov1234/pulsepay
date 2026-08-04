package uz.pulsepay.shared.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import uz.pulsepay.shared.exception.IdempotencyConflictException;
import uz.pulsepay.shared.idempotency.port.out.IdempotencyKeyRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * Insert-first idempotency guard (Risk #6 mitigation).
     * Attempts to insert the key first; if a UNIQUE violation occurs, returns the cached response.
     *
     * @throws IdempotencyConflictException when the key already exists and has a cached response
     */
    public void claimKey(String key, UUID userId, String requestHash) {
        IdempotencyKey record = new IdempotencyKey(
                key, userId, requestHash, null,
                Instant.now(), Instant.now().plusSeconds(86400)
        );
        try {
            repository.insert(record);
        } catch (DataIntegrityViolationException ex) {
            Optional<IdempotencyKey> existing = repository.findByKey(key);
            existing.ifPresent(k -> {
                if (k.responseSnapshot() != null) {
                    throw new IdempotencyConflictException(k.responseSnapshot());
                }
            });
            // Key exists but no response yet — in-flight duplicate; let it through (caller handles)
        }
    }

    public void recordResponse(String key, String responseSnapshot) {
        repository.updateResponseSnapshot(key, responseSnapshot);
    }
}
