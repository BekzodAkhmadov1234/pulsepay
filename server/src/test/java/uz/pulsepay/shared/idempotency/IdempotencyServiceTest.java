package uz.pulsepay.shared.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import uz.pulsepay.shared.exception.IdempotencyConflictException;
import uz.pulsepay.shared.idempotency.port.out.IdempotencyKeyRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 0 MANDATORY test: idempotency key enforcement.
 *
 * Rules:
 *  - First claim: succeeds (inserts the key)
 *  - Duplicate with cached response: throws IdempotencyConflictException carrying the cached response
 *  - Duplicate in-flight (no response yet): passes through silently (caller handles)
 *  - recordResponse: delegates to repository
 */
class IdempotencyServiceTest {

    private IdempotencyKeyRepository repository;
    private IdempotencyService service;

    private static final UUID USER_ID  = UUID.randomUUID();
    private static final String KEY     = "test-idempotency-key-001";
    private static final String HASH    = "request-hash-abc";

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyKeyRepository.class);
        service = new IdempotencyService(repository);
    }

    @Test
    void first_claim_inserts_and_succeeds() {
        service.claimKey(KEY, USER_ID, HASH);

        verify(repository).insert(any(IdempotencyKey.class));
    }

    @Test
    void duplicate_key_with_cached_response_throws_conflict_exception() {
        // Simulate UNIQUE constraint violation on insert
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(repository).insert(any(IdempotencyKey.class));

        String cachedResponse = "{\"id\":\"abc\",\"status\":\"COMPLETED\"}";
        IdempotencyKey existing = new IdempotencyKey(
                KEY, USER_ID, HASH, cachedResponse, Instant.now(), Instant.now().plusSeconds(86400));
        when(repository.findByKey(KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.claimKey(KEY, USER_ID, HASH))
                .isInstanceOf(IdempotencyConflictException.class)
                .satisfies(ex -> {
                    IdempotencyConflictException ice = (IdempotencyConflictException) ex;
                    assertThat(ice.getCachedResponse()).isEqualTo(cachedResponse);
                });
    }

    @Test
    void duplicate_key_in_flight_no_response_does_not_throw() {
        // Key exists but response not yet recorded (transfer still processing)
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(repository).insert(any(IdempotencyKey.class));

        IdempotencyKey inFlight = new IdempotencyKey(
                KEY, USER_ID, HASH, null,  // null response = in-flight
                Instant.now(), Instant.now().plusSeconds(86400));
        when(repository.findByKey(KEY)).thenReturn(Optional.of(inFlight));

        // Must NOT throw — the concurrent duplicate should be allowed to proceed
        assertThatNoException().isThrownBy(() -> service.claimKey(KEY, USER_ID, HASH));
    }

    @Test
    void duplicate_key_not_found_after_conflict_does_not_throw() {
        // Edge: constraint violation but row not yet visible (race)
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(repository).insert(any(IdempotencyKey.class));
        when(repository.findByKey(KEY)).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> service.claimKey(KEY, USER_ID, HASH));
    }

    @Test
    void record_response_delegates_to_repository() {
        String snapshot = "{\"status\":\"ok\"}";
        service.recordResponse(KEY, snapshot);

        verify(repository).updateResponseSnapshot(KEY, snapshot);
    }
}
