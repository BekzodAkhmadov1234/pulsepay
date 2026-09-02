package uz.pulsepay.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.pulsepay.domain.identity.AccountInactiveException;
import uz.pulsepay.domain.identity.DuplicatePhoneException;
import uz.pulsepay.domain.identity.InvalidOtpException;
import uz.pulsepay.dto.response.ErrorResponse;
import uz.pulsepay.domain.shared.ConflictException;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.IdempotencyConflictException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 401 Unauthorized — missing or invalid JWT ─────────────────────────────

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
