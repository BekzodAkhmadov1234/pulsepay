package uz.pulsepay.card.domain.port.in;

import uz.pulsepay.card.domain.model.Card;

import java.util.List;
import java.util.UUID;

public interface ListCardsPort {
    List<Card> listCards(UUID userId);
}
