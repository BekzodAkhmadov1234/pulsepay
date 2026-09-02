package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.repository.TransferRouteRepository;
import uz.pulsepay.domain.routing.TransferRouteEntity;
import uz.pulsepay.domain.routing.TransferRoute;
import uz.pulsepay.domain.shared.Money;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    public record CreateRouteCommand(
            String routeCode,
            String sourceNetwork,
            String destinationNetwork,
            String processorName,
            Long maxAmount,
            int priority,
            Integer avgProcessingSeconds,
            Integer transferTypeId,
            Instant effectiveFrom,
            Instant effectiveTo
    ) {}

    private final TransferRouteRepository routeRepository;

    public RoutingService(TransferRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    // ── Resolution (from RoutingResolutionService) ────────────────────────────

    public Optional<TransferRoute> findById(UUID id) {
        return routeRepository.findById(id).map(TransferRouteEntity::toDomain);
    }

    public TransferRoute resolve(String sourceNetwork, String destNetwork,
                                 int transferTypeId, Money amount) {
        List<TransferRoute> routes = routeRepository
                .findActiveRoutes(sourceNetwork, destNetwork, transferTypeId, Instant.now())
                .stream()
                .map(TransferRouteEntity::toDomain)
                .collect(Collectors.toList());

        return routes.stream()
                .filter(r -> r.maxAmount() == null || r.maxAmount() >= amount.amount())
                .sorted(Comparator.comparingInt(TransferRoute::priority))
                .findFirst()
                .orElseThrow(() -> new DomainException(
                        "No active route found for %s → %s".formatted(sourceNetwork, destNetwork)));
    }

    // ── Management (from ManageRouteUseCase) ─────────────────────────────────

    public TransferRoute createRoute(CreateRouteCommand cmd) {
        TransferRoute route = new TransferRoute(
                UUID.randomUUID(),
                cmd.routeCode(),
                cmd.sourceNetwork(),
                cmd.destinationNetwork(),
                cmd.processorName(),
                cmd.maxAmount(),
                cmd.priority(),
                cmd.avgProcessingSeconds(),
                true,
                cmd.transferTypeId(),
                cmd.effectiveFrom() != null ? cmd.effectiveFrom() : Instant.now(),
                cmd.effectiveTo());
        return routeRepository.save(TransferRouteEntity.fromDomain(route)).toDomain();
    }

    public List<TransferRoute> listAll() {
        return routeRepository.findAll().stream()
                .map(TransferRouteEntity::toDomain)
                .collect(Collectors.toList());
    }

    public TransferRoute getRoute(UUID id) {
        return routeRepository.findById(id)
                .map(TransferRouteEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Route not found: " + id));
    }

    public TransferRoute activate(UUID id) {
        TransferRoute r = getRoute(id);
        return routeRepository.save(TransferRouteEntity.fromDomain(withActive(r, true))).toDomain();
    }

    public TransferRoute deactivate(UUID id) {
        TransferRoute r = getRoute(id);
        return routeRepository.save(TransferRouteEntity.fromDomain(withActive(r, false))).toDomain();
    }

    public TransferRoute updateProcessor(UUID id, String processorName) {
        TransferRoute r = getRoute(id);
        TransferRoute updated = new TransferRoute(
                r.id(), r.routeCode(), r.sourceNetwork(), r.destinationNetwork(),
                processorName, r.maxAmount(), r.priority(), r.avgProcessingSeconds(),
                r.isActive(), r.transferTypeId(), r.effectiveFrom(), r.effectiveTo());
        return routeRepository.save(TransferRouteEntity.fromDomain(updated)).toDomain();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static TransferRoute withActive(TransferRoute r, boolean active) {
        return new TransferRoute(
                r.id(), r.routeCode(), r.sourceNetwork(), r.destinationNetwork(),
                r.processorName(), r.maxAmount(), r.priority(), r.avgProcessingSeconds(),
                active, r.transferTypeId(), r.effectiveFrom(), r.effectiveTo());
    }
}
