package uz.pulsepay.routing.application.usecase;

import org.springframework.stereotype.Service;
import uz.pulsepay.routing.domain.model.TransferRoute;
import uz.pulsepay.routing.domain.port.in.ManageRoutePort;
import uz.pulsepay.routing.domain.port.out.TransferRouteRepository;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ManageRouteUseCase implements ManageRoutePort {

    private final TransferRouteRepository routeRepository;

    public ManageRouteUseCase(TransferRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
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
        return routeRepository.save(route);
    }

    @Override
    public List<TransferRoute> listAll() {
        return routeRepository.findAll();
    }

    @Override
    public TransferRoute getRoute(UUID id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Route not found: " + id));
    }

    @Override
    public TransferRoute activate(UUID id) {
        TransferRoute r = getRoute(id);
        return routeRepository.save(withActive(r, true));
    }

    @Override
    public TransferRoute deactivate(UUID id) {
        TransferRoute r = getRoute(id);
        return routeRepository.save(withActive(r, false));
    }

    @Override
    public TransferRoute updateProcessor(UUID id, String processorName) {
        TransferRoute r = getRoute(id);
        return routeRepository.save(new TransferRoute(
                r.id(), r.routeCode(), r.sourceNetwork(), r.destinationNetwork(),
                processorName, r.maxAmount(), r.priority(), r.avgProcessingSeconds(),
                r.isActive(), r.transferTypeId(), r.effectiveFrom(), r.effectiveTo()));
    }

    private static TransferRoute withActive(TransferRoute r, boolean active) {
        return new TransferRoute(
                r.id(), r.routeCode(), r.sourceNetwork(), r.destinationNetwork(),
                r.processorName(), r.maxAmount(), r.priority(), r.avgProcessingSeconds(),
                active, r.transferTypeId(), r.effectiveFrom(), r.effectiveTo());
    }
}
