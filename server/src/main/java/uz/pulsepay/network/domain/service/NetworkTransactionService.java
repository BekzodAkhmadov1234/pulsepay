package uz.pulsepay.network.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.network.domain.model.CardTransaction;
import uz.pulsepay.network.domain.port.in.ExecuteCardTransferPort;
import uz.pulsepay.network.domain.port.out.CardNetworkGateway;
import uz.pulsepay.network.domain.port.out.CardTransactionRepository;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NetworkTransactionService implements ExecuteCardTransferPort {

    private final Map<String, CardNetworkGateway> gatewaysByCode;
    private final CardTransactionRepository transactionRepository;

    public NetworkTransactionService(List<CardNetworkGateway> gateways,
                                     CardTransactionRepository transactionRepository) {
        this.gatewaysByCode = gateways.stream()
                .collect(Collectors.toMap(CardNetworkGateway::networkCode, Function.identity()));
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void execute(UUID transferId, UUID senderCardId, UUID recipientCardId,
                        Money amount, String routeCode) {
        // Resolve which gateway to use from the route code prefix
        String network = routeCode.startsWith("uzcard") ? "uzcard" : "humo";
        CardNetworkGateway gateway = gatewaysByCode.get(network);
        if (gateway == null) {
            throw new DomainException("No gateway available for network: " + network);
        }

        String debitRef = gateway.debitCard(senderCardId, amount, transferId.toString() + "-debit");
        transactionRepository.save(new CardTransaction(
                UUID.randomUUID(), transferId, 1, senderCardId,
                amount.amount(), debitRef, null, "success", Instant.now(), null));

        String creditRef = gateway.creditCard(recipientCardId, amount, transferId.toString() + "-credit");
        transactionRepository.save(new CardTransaction(
                UUID.randomUUID(), transferId, 2, recipientCardId,
                amount.amount(), creditRef, null, "success", Instant.now(), null));
    }
}
