package uz.pulsepay.domain.shared;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
