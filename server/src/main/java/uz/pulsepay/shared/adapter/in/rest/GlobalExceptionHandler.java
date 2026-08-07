package uz.pulsepay.shared.adapter.in.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.pulsepay.identity.domain.exception.AccountInactiveException;
import uz.pulsepay.identity.domain.exception.DuplicatePhoneException;
import uz.pulsepay.identity.domain.exception.InvalidOtpException;
import uz.pulsepay.shared.adapter.in.rest.dto.ErrorResponse;
import uz.pulsepay.shared.exception.ConflictException;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.IdempotencyConflictException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 401 Unauthorized — missing or invalid JWT ─────────────────────────────
    // Spring Security throws AuthenticationException when no valid token is present.
    // Returning 401 here gives API clients a structured JSON body instead of the
    // default Spring Security plain-text response.

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication required: " + ex.getMessage());
    }

    // ── 403 Forbidden — authenticated but not permitted ───────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage());
    }

    // ── 403 Forbidden — account is inactive or closed ────────────────────────
    // Checked before the generic DomainException handler; Spring MVC picks the
    // most specific matching handler automatically.

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactive(AccountInactiveException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ── 409 Conflict — duplicate phone registration ───────────────────────────

    @ExceptionHandler(DuplicatePhoneException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePhone(DuplicatePhoneException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── 409 Conflict — general business conflict (fee rule overlap, etc.) ─────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── 400 Bad Request — invalid OTP (wrong code, expired, locked out) ──────
    // Intentionally 400 rather than 422 to give the client a clear, actionable
    // signal that the OTP itself is the problem, distinct from a business rule error.

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 422 Unprocessable Entity (business rule violations) ──────────────────
    // HttpStatus.UNPROCESSABLE_ENTITY is deprecated in Spring 6+; use the raw code.

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(422)
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(422).body(body);
    }

    // ── 400 Bad Request — validation ─────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for " + fieldErrors.size() + " field(s)")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── 400 Bad Request — fractional tiyin (Risk #8) ─────────────────────────

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ErrorResponse> handleArithmetic(ArithmeticException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Amount must be a whole number of tiyin (no fractional sub-units)");
    }

    // ── 200 OK — idempotency replay ───────────────────────────────────────────

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotency(IdempotencyConflictException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .error("Idempotent Replay")
                .message("Request was already processed; returning cached response")
                .build();
        return ResponseEntity.ok(body);
    }

    // ── 500 Internal Server Error — catch-all ────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
