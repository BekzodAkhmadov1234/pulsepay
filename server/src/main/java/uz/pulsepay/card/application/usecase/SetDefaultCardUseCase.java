package uz.pulsepay.card.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.port.in.SetDefaultCardPort;
import uz.pulsepay.card.domain.port.out.CardRepository;

import java.util.UUID;

@Slf4j
@Service
public class SetDefaultCardUseCase implements SetDefaultCardPort {

    private final CardRepository cardRepository;

    public SetDefaultCardUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public Card setDefault(UUID cardId, UUID userId) {
        Card card = cardRepository.setDefault(cardId, userId);
        log.info("Default card changed: cardId={}, userId={}", cardId, userId);
        return card;
    }
}
