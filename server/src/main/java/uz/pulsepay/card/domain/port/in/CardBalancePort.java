package uz.pulsepay.card.domain.port.in;

import java.util.UUID;

/**
 * Stub-phase port: atomically debit or credit a card's local shadow balance.
 *
 * <p>Used by stub gateways (UzCardGateway, HumoGateway) during development.
 * When real Humo/UzCard APIs are wired, remove this injection from the gateway
 * classes — the real network handles balances externally.
 */
public interface CardBalancePort {

    /**
     * Atomically deducts {@code amountTiyin} from the card's balance.
     *
     * @throws uz.pulsepay.shared.exception.InsufficientFundsException if balance &lt; amountTiyin
     */
    void debit(UUID cardId, long amountTiyin);

    /** Atomically adds {@code amountTiyin} to the card's balance. */
    void credit(UUID cardId, long amountTiyin);
}
