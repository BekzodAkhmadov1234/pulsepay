package uz.pulsepay.card.domain.port.in;

import uz.pulsepay.card.domain.model.Card;

import java.util.UUID;

public interface SetDefaultCardPort {
    Card setDefault(UUID cardId, UUID userId);
}
