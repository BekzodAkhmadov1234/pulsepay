package uz.pulsepay.network.adapter.out.http;

import org.springframework.stereotype.Component;
import uz.pulsepay.network.domain.port.out.CardNetworkGateway;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.util.UUID;

/**
 * Stub UzCard gateway. Replace with actual UzCard HTTP integration.
 */
@Component
public class UzCardGateway implements CardNetworkGateway {

    @Override
    public String networkCode() {
        return "uzcard";
    }

    @Override
    public String debitCard(UUID cardId, Money amount, String referenceId) {
        // TODO: implement UzCard debit API call
        throw new DomainException("UzCard gateway not yet implemented");
    }

    @Override
    public String creditCard(UUID cardId, Money amount, String referenceId) {
        // TODO: implement UzCard credit API call
        throw new DomainException("UzCard gateway not yet implemented");
    }

    @Override
    public void reverseTransaction(String networkTransactionId) {
        // TODO: implement UzCard reversal API call
        throw new DomainException("UzCard gateway not yet implemented");
    }
}
