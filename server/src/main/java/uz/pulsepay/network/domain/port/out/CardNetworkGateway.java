package uz.pulsepay.network.domain.port.out;

import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

public interface CardNetworkGateway {

    String networkCode();

    String debitCard(UUID cardId, Money amount, String referenceId);

    String creditCard(UUID cardId, Money amount, String referenceId);

    void reverseTransaction(String networkTransactionId);
}
