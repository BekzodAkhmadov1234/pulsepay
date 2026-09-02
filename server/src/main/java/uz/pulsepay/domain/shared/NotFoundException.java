package uz.pulsepay.domain.shared;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
