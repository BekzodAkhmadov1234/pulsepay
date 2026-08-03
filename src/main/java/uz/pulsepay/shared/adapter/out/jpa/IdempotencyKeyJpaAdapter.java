package uz.pulsepay.shared.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.shared.idempotency.IdempotencyKey;
import uz.pulsepay.shared.idempotency.port.out.IdempotencyKeyRepository;

import java.util.Optional;

@Repository
class IdempotencyKeyJpaAdapter implements IdempotencyKeyRepository {

    private final IdempotencyKeyJpaRepository jpaRepository;

    IdempotencyKeyJpaAdapter(IdempotencyKeyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void insert(IdempotencyKey idempotencyKey) {
        jpaRepository.save(new IdempotencyKeyEntity(
                idempotencyKey.key(),
                idempotencyKey.userId(),
                idempotencyKey.requestHash(),
                idempotencyKey.responseSnapshot(),
                idempotencyKey.createdAt(),
                idempotencyKey.expiresAt()
        ));
    }

    @Override
    public Optional<IdempotencyKey> findByKey(String key) {
        return jpaRepository.findById(key).map(e -> new IdempotencyKey(
                e.getKey(), e.getUserId(), e.getRequestHash(),
                e.getResponseSnapshot(), e.getCreatedAt(), e.getExpiresAt()
        ));
    }

    @Override
    public void updateResponseSnapshot(String key, String responseSnapshot) {
        jpaRepository.updateResponseSnapshot(key, responseSnapshot);
    }
}
