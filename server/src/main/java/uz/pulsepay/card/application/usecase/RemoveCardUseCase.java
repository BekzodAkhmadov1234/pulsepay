package uz.pulsepay.card.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.card.domain.port.in.RemoveCardPort;
import uz.pulsepay.card.domain.port.out.CardRepository;
import uz.pulsepay.shared.exception.NotFoundException;

import java.util.UUID;

@Slf4j
@Service
public class RemoveCardUseCase implements RemoveCardPort {

    private final CardRepository cardRepository;

    public RemoveCardUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public void removeCard(UUID cardId, UUID requestingUserId) {
        cardRepository.findByIdAndOwnerUserId(cardId, requestingUserId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

        cardRepository.softDelete(cardId);
        log.info("Card removed: cardId={}, userId={}", cardId, requestingUserId);
    }
}
