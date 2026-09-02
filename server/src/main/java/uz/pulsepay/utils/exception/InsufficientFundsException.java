package uz.pulsepay.utils.exception;

/**
 * Thrown when a card's stub balance cannot cover the requested debit amount.
 * Extends DomainException so GlobalExceptionHandler maps it to HTTP 422 automatically.
 */
public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
