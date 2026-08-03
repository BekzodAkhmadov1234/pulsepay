package uz.pulsepay.card.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.card.adapter.out.jpa.entity.SavedRecipientCardEntity;
import uz.pulsepay.card.domain.model.SavedRecipientCard;
import uz.pulsepay.card.domain.port.out.SavedRecipientCardRepository;

import java.util.List;
import java.util.UUID;

@Repository
class SavedRecipientCardJpaAdapter implements SavedRecipientCardRepository {

    private final SavedRecipientCardJpaRepository jpa;

    SavedRecipientCardJpaAdapter(SavedRecipientCardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SavedRecipientCard save(SavedRecipientCard card) {
        return jpa.save(SavedRecipientCardEntity.fromDomain(card)).toDomain();
    }

    @Override
    public List<SavedRecipientCard> findByOwnerUserId(UUID userId) {
        return jpa.findByOwnerUserId(userId).stream().map(SavedRecipientCardEntity::toDomain).toList();
    }
}
