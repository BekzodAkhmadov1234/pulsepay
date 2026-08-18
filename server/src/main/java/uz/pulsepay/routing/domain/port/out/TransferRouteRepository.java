package uz.pulsepay.routing.domain.port.out;

import uz.pulsepay.routing.domain.model.TransferRoute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRouteRepository {
    List<TransferRoute> findActiveRoutes(String sourceNetwork, String destNetwork, int transferTypeId);
    List<TransferRoute> findAll();
    Optional<TransferRoute> findById(UUID id);
    TransferRoute save(TransferRoute route);
}
