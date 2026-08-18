package uz.pulsepay.routing.domain.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.routing.domain.port.in.FindRoutePort;
import uz.pulsepay.routing.domain.port.in.ResolveRoutePort;
import uz.pulsepay.routing.domain.port.out.TransferRouteRepository;
import uz.pulsepay.shared.domain.Money;
import uz.pulsepay.shared.exception.DomainException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoutingResolutionService implements ResolveRoutePort, FindRoutePort {

    private final TransferRouteRepository routeRepository;

    public RoutingResolutionService(TransferRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    public Optional<TransferRoute> findById(UUID id) {
        return routeRepository.findById(id);
    }

    @Override
    public TransferRoute resolve(String sourceNetwork, String destNetwork,
                                 int transferTypeId, Money amount) {
        List<TransferRoute> routes = routeRepository.findActiveRoutes(
                sourceNetwork, destNetwork, transferTypeId);

        return routes.stream()
                .filter(r -> r.maxAmount() == null || r.maxAmount() >= amount.amount())
                .sorted(Comparator.comparingInt(TransferRoute::priority))
                .findFirst()
                .orElseThrow(() -> new DomainException(
                        "No active route found for %s → %s".formatted(sourceNetwork, destNetwork)));
    }
}
