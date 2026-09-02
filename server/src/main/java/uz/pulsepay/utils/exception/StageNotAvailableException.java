package uz.pulsepay.utils.exception;

public class StageNotAvailableException extends DomainException {

    public StageNotAvailableException(String feature) {
        super("Feature '" + feature + "' is not available in the current deployment stage");
    }
}
