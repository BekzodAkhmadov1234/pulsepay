package uz.pulsepay.card.domain.port.in;

import uz.pulsepay.card.domain.model.Card;

import java.util.UUID;

public interface AddCardPort {
    Card addCard(UUID userId, String cardToken, String maskedPan,
                 String cardNetwork, String cardHolderName,
                 short expMonth, short expYear);
}
