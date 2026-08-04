package uz.pulsepay.card.domain.port.in;

import java.util.UUID;

public interface RemoveCardPort {
    void removeCard(UUID cardId, UUID requestingUserId);
}
