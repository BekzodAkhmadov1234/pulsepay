package uz.pulsepay.network.adapter.out.http;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import uz.pulsepay.card.domain.port.in.CardBalancePort;
import uz.pulsepay.network.domain.port.out.CardNetworkGateway;
import uz.pulsepay.shared.domain.Money;

import java.util.UUID;

/**
 * Stub Humo gateway. Replace with actual Humo/NIPS HTTP integration when PaySys/MONTRA is wired.
 * During the stub phase, debit/credit update the card's local shadow balance in the DB.
 * When the real integration is ready, remove the CardBalancePort injection and call the real API.
 */
@Slf4j
@Component
public class HumoGateway implements CardNetworkGateway {

    private final CardBalancePort cardBalancePort;

    public HumoGateway(CardBalancePort cardBalancePort) {
        this.cardBalancePort = cardBalancePort;
    }

    @Override
    public String networkCode() {
        return "humo";
    }

    @Override
    public String debitCard(UUID cardId, Money amount, String referenceId) {
        String ref = "humo_debit_" + referenceId;
        log.info("[STUB] Humo debit: cardId={}, amount={}, ref={}", cardId, amount, ref);
        cardBalancePort.debit(cardId, amount.amount());
        return ref;
    }

    @Override
    public String creditCard(UUID cardId, Money amount, String referenceId) {
        String ref = "humo_credit_" + referenceId;
        log.info("[STUB] Humo credit: cardId={}, amount={}, ref={}", cardId, amount, ref);
        cardBalancePort.credit(cardId, amount.amount());
        return ref;
    }

    @Override
    public void reverseTransaction(String networkTransactionId) {
        log.info("[STUB] Humo reversal: txId={}", networkTransactionId);
    }
}
