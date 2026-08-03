package uz.pulsepay.routing.domain.port.out;

import uz.pulsepay.routing.domain.model.TransferRoute;

import java.util.List;

public interface TransferRouteRepository {
    List<TransferRoute> findActiveRoutes(String sourceNetwork, String destNetwork, int transferTypeId);
}
