package uz.pulsepay.card.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.card.adapter.out.jpa.entity.CardEntity;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.model.CardStatus;
import uz.pulsepay.card.domain.port.out.CardRepository;
import uz.pulsepay.shared.exception.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CardJpaAdapter implements CardRepository {

    private final CardJpaRepository jpa;

    CardJpaAdapter(CardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Card save(Card card) {
        return jpa.save(CardEntity.fromDomain(card)).toDomain();
    }

    @Override
    public Optional<Card> findById(UUID id) {
        return jpa.findById(id).map(CardEntity::toDomain);
    }

    @Override
    public List<Card> findByOwnerUserId(UUID userId) {
        return jpa.findByOwnerUserId(userId).stream().map(CardEntity::toDomain).toList();
    }

    @Override
    public Optional<Card> findByIdAndOwnerUserId(UUID cardId, UUID userId) {
        return jpa.findByIdAndOwnerUserId(cardId, userId).map(CardEntity::toDomain);
    }

    @Override
    public Card updateStatus(UUID cardId, CardStatus newStatus) {
        jpa.updateStatus(cardId, newStatus);
        return jpa.findById(cardId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found after status update: " + cardId));
    }

    @Override
    public int deactivateAllVerifiedByOwner(UUID userId) {
        return jpa.deactivateAllVerifiedByOwner(userId);
    }
}
