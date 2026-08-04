package uz.pulsepay.network.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.network.adapter.out.jpa.entity.CardTransactionEntity;
import uz.pulsepay.network.domain.model.CardTransaction;
import uz.pulsepay.network.domain.port.out.CardTransactionRepository;

@Repository
class CardTransactionJpaAdapter implements CardTransactionRepository {

    private final CardTransactionJpaRepository jpa;

    CardTransactionJpaAdapter(CardTransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CardTransaction save(CardTransaction transaction) {
        return jpa.save(CardTransactionEntity.fromDomain(transaction)).toDomain();
    }
}
