package uz.pulsepay.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uz.pulsepay.repository.IdempotencyKeyRepository;
import uz.pulsepay.utils.exception.IdempotencyConflictException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Insert-first idempotency guard (Risk #6 mitigation).
 *
 * Ported from shared/idempotency/IdempotencyService.java.
 * Uses uz.pulsepay.repository.IdempotencyKeyRepository (Spring Data JPA)
 * for updateResponseSnapshot, and JdbcTemplate for the insert+read
 * (IdempotencyKeyEntity is package-private in shared.adapter.out.jpa and
 * cannot be instantiated from this package directly).
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyService(IdempotencyKeyRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository   = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Attempts to insert the key first; if a UNIQUE violation occurs, returns the cached response.
     *
     * @throws IdempotencyConflictException when the key already exists and has a cached response
     */
    public void claimKey(String key, UUID userId, String requestHash) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO idempotency_keys (key, user_id, request_hash, response_snapshot, created_at, expires_at) " +
                    "VALUES (?, ?, ?, NULL, ?, ?)",
                    key,
                    userId,
                    requestHash,
                    Timestamp.from(Instant.now()),
                    Timestamp.from(Instant.now().plusSeconds(86400))
            );
        } catch (DataIntegrityViolationException ex) {
            String snapshot = jdbcTemplate.query(
                    "SELECT response_snapshot FROM idempotency_keys WHERE key = ?",
                    rs -> rs.next() ? rs.getString("response_snapshot") : null,
                    key
            );
            if (snapshot != null) {
                throw new IdempotencyConflictException(snapshot);
            }
            // Key exists but no response yet — in-flight duplicate; let it through (caller handles)
        }
    }

    public void recordResponse(String key, String responseSnapshot) {
        repository.updateResponseSnapshot(key, responseSnapshot);
    }
}
