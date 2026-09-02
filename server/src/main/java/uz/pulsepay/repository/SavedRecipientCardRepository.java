package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.card.SavedRecipientCardEntity;

import java.util.List;
import java.util.UUID;

public interface SavedRecipientCardRepository extends JpaRepository<SavedRecipientCardEntity, UUID> {

    List<SavedRecipientCardEntity> findByOwnerUserId(UUID ownerUserId);
}
