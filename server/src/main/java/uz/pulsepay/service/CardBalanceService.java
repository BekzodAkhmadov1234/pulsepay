package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.repository.CardRepository;

import java.util.UUID;

/**
 * Stub-phase shadow balance tracker for cards.
 * Delegates atomic debit/credit to CardRepository (single-row SQL UPDATEs).
 */
@Service
public class CardBalanceService {

    private final CardRepository cardRepository;

    public CardBalanceService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public void debit(UUID cardId, long amountTiyin) {
        cardRepository.debitBalance(cardId, amountTiyin);
    }

    public void credit(UUID cardId, long amountTiyin) {
        cardRepository.creditBalance(cardId, amountTiyin);
    }
}
