package uz.pulsepay.routing.domain.port.in;

import uz.pulsepay.routing.domain.model.TransferRoute;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ManageRoutePort {
    TransferRoute createRoute(CreateRouteCommand cmd);
    List<TransferRoute> listAll();
    TransferRoute getRoute(UUID id);
    TransferRoute activate(UUID id);
    TransferRoute deactivate(UUID id);
    TransferRoute updateProcessor(UUID id, String processorName);

    record CreateRouteCommand(
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
}
