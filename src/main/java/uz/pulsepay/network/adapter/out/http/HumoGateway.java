package uz.pulsepay.network.adapter.out.http;

import org.springframework.stereotype.Component;
import uz.pulsepay.network.domain.port.out.CardNetworkGateway;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.util.UUID;

/**
 * Stub Humo gateway. Replace with actual Humo/NIPS HTTP integration.
 */
@Component
public class HumoGateway implements CardNetworkGateway {

    @Override
    public String networkCode() {
        return "humo";
    }

    @Override
    public String debitCard(UUID cardId, Money amount, String referenceId) {
        // TODO: implement Humo debit API call
        throw new DomainException("Humo gateway not yet implemented");
    }

    @Override
    public String creditCard(UUID cardId, Money amount, String referenceId) {
        // TODO: implement Humo credit API call
        throw new DomainException("Humo gateway not yet implemented");
    }

    @Override
    public void reverseTransaction(String networkTransactionId) {
        // TODO: implement Humo reversal API call
        throw new DomainException("Humo gateway not yet implemented");
    }
}
