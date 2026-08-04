package uz.pulsepay.card.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.card.adapter.out.jpa.entity.SavedRecipientCardEntity;

import java.util.List;
import java.util.UUID;

interface SavedRecipientCardJpaRepository extends JpaRepository<SavedRecipientCardEntity, UUID> {
    List<SavedRecipientCardEntity> findByOwnerUserId(UUID ownerUserId);
}
