package uz.pulsepay.mapper;

import org.springframework.stereotype.Component;
import uz.pulsepay.domain.card.Card;
import uz.pulsepay.dto.response.CardResponse;

import java.math.BigDecimal;

@Component
public class CardMapper {

    public CardResponse toResponse(Card card) {
        return new CardResponse(
                card.id(),
                card.maskedPan(),
                card.cardNetwork(),
                card.cardHolderName(),
                card.expMonth(),
                card.expYear(),
                card.status().name(),
                card.isDefault(),
                card.verifiedAt(),
                BigDecimal.valueOf(card.balanceTiyin()).divide(BigDecimal.valueOf(100))
        );
    }
}
