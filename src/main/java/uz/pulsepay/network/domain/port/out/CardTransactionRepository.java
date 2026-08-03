package uz.pulsepay.network.domain.port.out;

import uz.pulsepay.network.domain.model.CardTransaction;

public interface CardTransactionRepository {
    CardTransaction save(CardTransaction transaction);
}
