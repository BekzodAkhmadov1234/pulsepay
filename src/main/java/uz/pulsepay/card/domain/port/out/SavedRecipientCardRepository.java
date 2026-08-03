package uz.pulsepay.card.domain.port.out;

import uz.pulsepay.card.domain.model.SavedRecipientCard;

import java.util.List;
import java.util.UUID;

public interface SavedRecipientCardRepository {
    SavedRecipientCard save(SavedRecipientCard card);
    List<SavedRecipientCard> findByOwnerUserId(UUID userId);
}
