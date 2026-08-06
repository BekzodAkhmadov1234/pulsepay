package uz.pulsepay.card.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.port.in.ListCardsPort;
import uz.pulsepay.card.domain.port.out.CardRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ListCardsUseCase implements ListCardsPort {

    private final CardRepository cardRepository;

    public ListCardsUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> listCards(UUID userId) {
        return cardRepository.findByOwnerUserId(userId);
    }
}
