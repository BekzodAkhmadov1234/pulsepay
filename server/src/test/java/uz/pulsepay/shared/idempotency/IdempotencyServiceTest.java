package uz.pulsepay.shared.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import uz.pulsepay.repository.IdempotencyKeyRepository;
import uz.pulsepay.service.IdempotencyService;
import uz.pulsepay.utils.exception.IdempotencyConflictException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 0 MANDATORY test: idempotency key enforcement.
 *
 * Rules:
 *  - First claim: inserts via jdbcTemplate
 *  - Duplicate with cached response: throws IdempotencyConflictException carrying the cached response
 *  - Duplicate in-flight (no response yet): passes through silently (caller handles)
 *  - recordResponse: delegates to repository
 */
class IdempotencyServiceTest {

    private IdempotencyKeyRepository repository;
    private JdbcTemplate jdbcTemplate;
    private IdempotencyService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String KEY   = "test-idempotency-key-001";
    private static final String HASH  = "request-hash-abc";

    @BeforeEach
    void setUp() {
        repository   = mock(IdempotencyKeyRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service      = new IdempotencyService(repository, jdbcTemplate);
    }

    @Test
    void first_claim_inserts_and_succeeds() {
        service.claimKey(KEY, USER_ID, HASH);

        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicate_key_with_cached_response_throws_conflict_exception() {
        String cachedResponse = "{\"id\":\"abc\",\"status\":\"COMPLETED\"}";
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(KEY)))
                .thenReturn(cachedResponse);

        assertThatThrownBy(() -> service.claimKey(KEY, USER_ID, HASH))
                .isInstanceOf(IdempotencyConflictException.class)
                .satisfies(ex -> {
                    IdempotencyConflictException ice = (IdempotencyConflictException) ex;
                    assertThat(ice.getCachedResponse()).isEqualTo(cachedResponse);
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicate_key_in_flight_no_response_does_not_throw() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(KEY)))
                .thenReturn(null);

        // Must NOT throw — in-flight duplicate should be allowed to proceed
        assertThatNoException().isThrownBy(() -> service.claimKey(KEY, USER_ID, HASH));
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicate_key_not_found_after_conflict_does_not_throw() {
        // Edge: constraint violation but row not yet visible (race)
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(KEY)))
                .thenReturn(null);

        assertThatNoException().isThrownBy(() -> service.claimKey(KEY, USER_ID, HASH));
    }

    @Test
    void record_response_delegates_to_repository() {
        String snapshot = "{\"status\":\"ok\"}";
        service.recordResponse(KEY, snapshot);

        verify(repository).updateResponseSnapshot(KEY, snapshot);
    }
}
