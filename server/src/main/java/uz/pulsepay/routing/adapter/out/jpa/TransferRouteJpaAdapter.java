package uz.pulsepay.routing.adapter.out.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import uz.pulsepay.routing.adapter.out.jpa.entity.TransferRouteEntity;
import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.routing.domain.port.out.TransferRouteRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransferRouteJpaAdapter implements TransferRouteRepository {

    private final TransferRouteJpaRepository jpa;

    @PersistenceContext
    private EntityManager em;

    TransferRouteJpaAdapter(TransferRouteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TransferRoute> findActiveRoutes(String sourceNetwork, String destNetwork, int transferTypeId) {
        return jpa.findActiveRoutes(sourceNetwork, destNetwork, transferTypeId, Instant.now())
                .stream().map(TransferRouteEntity::toDomain).toList();
    }

    @Override
    public List<TransferRoute> findAll() {
        return jpa.findAll().stream().map(TransferRouteEntity::toDomain).toList();
    }

    @Override
    public Optional<TransferRoute> findById(UUID id) {
        return jpa.findById(id).map(TransferRouteEntity::toDomain);
    }

    @Override
    public TransferRoute save(TransferRoute route) {
        TransferRouteEntity entity = TransferRouteEntity.fromDomain(route);
        return em.merge(entity).toDomain();
    }
}
