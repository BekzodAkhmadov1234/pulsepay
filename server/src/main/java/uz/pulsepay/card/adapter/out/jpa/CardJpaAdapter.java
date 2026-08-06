package uz.pulsepay.card.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.card.adapter.out.jpa.entity.CardEntity;
import uz.pulsepay.card.domain.model.Card;
import uz.pulsepay.card.domain.model.CardStatus;
import uz.pulsepay.card.domain.port.out.CardRepository;
import uz.pulsepay.shared.exception.InsufficientFundsException;
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
    @Transactional
    public Card save(Card card, UUID ownerPartyId) {
        jpa.insertInstrument(card.id(), ownerPartyId);
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

    @Override
    public void softDelete(UUID cardId) {
        jpa.softDelete(cardId);
    }

    @Override
    public Optional<Card> findByMaskedPanPattern(String first6, String last4) {
        return jpa.findByMaskedPanPattern(first6, last4).map(CardEntity::toDomain);
    }

    @Override
    public Optional<UUID> findOwnerIdByCardId(UUID cardId) {
        return jpa.findOwnerIdByCardId(cardId);
    }

    @Override
    @Transactional
    public void debitBalance(UUID cardId, long amountTiyin) {
        int rows = jpa.debitBalance(cardId, amountTiyin);
        if (rows == 0) {
            throw new InsufficientFundsException(
                    "Insufficient balance on card " + cardId + " for amount " + amountTiyin + " tiyin");
        }
    }

    @Override
    @Transactional
    public void creditBalance(UUID cardId, long amountTiyin) {
        jpa.creditBalance(cardId, amountTiyin);
    }

    @Override
    @Transactional
    public Card setDefault(UUID cardId, UUID userId) {
        jpa.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        jpa.clearDefaultForUser(userId);
        jpa.markDefault(cardId);
        return jpa.findById(cardId)
                .map(CardEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
    }
}
