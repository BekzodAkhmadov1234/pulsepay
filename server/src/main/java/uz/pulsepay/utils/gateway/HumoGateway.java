package uz.pulsepay.utils.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.pulsepay.service.CardBalanceService;
import uz.pulsepay.domain.shared.Money;

import java.util.UUID;

/**
 * Stub Humo gateway. Replace with actual Humo/NIPS HTTP integration when PaySys/MONTRA is wired.
 * During the stub phase, debit/credit update the card's local shadow balance in the DB.
 * When the real integration is ready, remove the CardBalancePort injection and call the real API.
 */
@Slf4j
@Component
public class HumoGateway implements CardNetworkGateway {

    private final CardBalanceService cardBalanceService;

    public HumoGateway(CardBalanceService cardBalanceService) {
        this.cardBalanceService = cardBalanceService;
    }

    @Override
    public String networkCode() {
        return "humo";
    }

    @Override
    public String debitCard(UUID cardId, Money amount, String referenceId) {
        String ref = "humo_debit_" + referenceId;
        log.info("[STUB] Humo debit: cardId={}, amount={}, ref={}", cardId, amount, ref);
        cardBalanceService.debit(cardId, amount.amount());
        return ref;
    }

    @Override
    public String creditCard(UUID cardId, Money amount, String referenceId) {
        String ref = "humo_credit_" + referenceId;
        log.info("[STUB] Humo credit: cardId={}, amount={}, ref={}", cardId, amount, ref);
        cardBalanceService.credit(cardId, amount.amount());
        return ref;
    }

    @Override
    public void reverseTransaction(String networkTransactionId) {
        log.info("[STUB] Humo reversal: txId={}", networkTransactionId);
    }
}
