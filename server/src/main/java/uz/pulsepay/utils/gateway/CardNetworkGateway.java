package uz.pulsepay.utils.gateway;

import uz.pulsepay.domain.shared.Money;

import java.util.UUID;

public interface CardNetworkGateway {

    String networkCode();

    String debitCard(UUID cardId, Money amount, String referenceId);

    String creditCard(UUID cardId, Money amount, String referenceId);

    void reverseTransaction(String networkTransactionId);

    default boolean isCardNetwork() { return true; }
}
