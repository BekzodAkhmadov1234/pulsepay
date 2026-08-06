package uz.pulsepay.card.application.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.card.domain.port.in.CardBalancePort;
import uz.pulsepay.card.domain.port.out.CardRepository;

import java.util.UUID;

/**
 * Stub-phase implementation of CardBalancePort.
 * Delegates atomic debit/credit to CardRepository which issues single-row SQL UPDATEs.
 *
 * <p>When real Humo/UzCard APIs are integrated, remove the CardBalancePort injection from
 * the gateway classes — this service can stay as a shadow-balance tracker if needed.
 */
@Service
public class CardBalanceService implements CardBalancePort {

    private final CardRepository cardRepository;

    public CardBalanceService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public void debit(UUID cardId, long amountTiyin) {
        cardRepository.debitBalance(cardId, amountTiyin);
    }

    @Override
    public void credit(UUID cardId, long amountTiyin) {
        cardRepository.creditBalance(cardId, amountTiyin);
    }
}
