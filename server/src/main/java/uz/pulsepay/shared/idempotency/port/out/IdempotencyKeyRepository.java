package uz.pulsepay.shared.idempotency.port.out;

import uz.pulsepay.shared.idempotency.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyKeyRepository {

    void insert(IdempotencyKey idempotencyKey);

    Optional<IdempotencyKey> findByKey(String key);

    void updateResponseSnapshot(String key, String responseSnapshot);
}
