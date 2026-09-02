package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.domain.network.CardTransactionEntity;
import uz.pulsepay.domain.network.CardTransaction;
import uz.pulsepay.repository.CardTransactionRepository;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.utils.gateway.CardNetworkGateway;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NetworkTransactionService {

    private final Map<String, CardNetworkGateway> gatewaysByCode;
    private final CardTransactionRepository transactionRepository;

    public NetworkTransactionService(List<CardNetworkGateway> gateways,
                                     CardTransactionRepository transactionRepository) {
        this.gatewaysByCode = gateways.stream()
                .collect(Collectors.toMap(CardNetworkGateway::networkCode, Function.identity()));
        this.transactionRepository = transactionRepository;
    }

    public void execute(UUID transferId, UUID senderCardId, UUID recipientCardId,
                        Money debitAmount, Money creditAmount, String processorName) {
        // Select gateway by processor name (the configured intermediary for this route)
        CardNetworkGateway gateway = gatewaysByCode.get(processorName);
        if (gateway == null) {
            // Fallback: use any available gateway (covers 'stub' or unknown processors)
            gateway = gatewaysByCode.values().stream().findFirst()
                    .orElseThrow(() -> new DomainException("No gateway available for processor: " + processorName));
            log.warn("Unknown processor '{}' — falling back to gateway '{}'", processorName, gateway.networkCode());
        }

        gateway.debitCard(senderCardId, debitAmount, transferId.toString() + "-debit");
        transactionRepository.save(CardTransactionEntity.fromDomain(new CardTransaction(
                UUID.randomUUID(), transferId, 1, senderCardId,
                debitAmount.amount(), null, null, "success", Instant.now(), null)));

        gateway.creditCard(recipientCardId, creditAmount, transferId.toString() + "-credit");

        // CRITICAL P2A FIX: only save credit-side transaction when the gateway is a card network.
        // Bank account instruments are not in the cards table, so saving would violate FK constraints.
        if (gateway.isCardNetwork()) {
            transactionRepository.save(CardTransactionEntity.fromDomain(new CardTransaction(
                    UUID.randomUUID(), transferId, 2, recipientCardId,
                    creditAmount.amount(), null, null, "success", Instant.now(), null)));
        }
    }
}
