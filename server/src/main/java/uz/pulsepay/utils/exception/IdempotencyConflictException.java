package uz.pulsepay.utils.exception;

public class IdempotencyConflictException extends DomainException {

    private final String cachedResponse;

    public IdempotencyConflictException(String cachedResponse) {
        super("Duplicate request: returning cached response");
        this.cachedResponse = cachedResponse;
    }

    public String getCachedResponse() {
        return cachedResponse;
    }
}
