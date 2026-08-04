package uz.pulsepay.routing.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.routing.adapter.out.jpa.entity.TransferRouteEntity;
import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.routing.domain.port.out.TransferRouteRepository;

import java.time.Instant;
import java.util.List;

@Repository
class TransferRouteJpaAdapter implements TransferRouteRepository {

    private final TransferRouteJpaRepository jpa;

    TransferRouteJpaAdapter(TransferRouteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TransferRoute> findActiveRoutes(String sourceNetwork, String destNetwork, int transferTypeId) {
        return jpa.findActiveRoutes(sourceNetwork, destNetwork, transferTypeId, Instant.now())
                .stream().map(TransferRouteEntity::toDomain).toList();
    }
}
