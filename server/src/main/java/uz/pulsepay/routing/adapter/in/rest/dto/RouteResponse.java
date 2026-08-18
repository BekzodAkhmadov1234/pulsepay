package uz.pulsepay.routing.adapter.in.rest.dto;

import uz.pulsepay.routing.domain.model.TransferRoute;

import java.time.Instant;
import java.util.UUID;

public record RouteResponse(
        UUID id,
        String routeCode,
        String sourceNetwork,
        String destinationNetwork,
        String processorName,
        Long maxAmount,
        int priority,
        Integer avgProcessingSeconds,
        boolean isActive,
        Integer transferTypeId,
        Instant effectiveFrom,
        Instant effectiveTo
) {
    public static RouteResponse from(TransferRoute r) {
        return new RouteResponse(
                r.id(), r.routeCode(), r.sourceNetwork(), r.destinationNetwork(),
                r.processorName(), r.maxAmount(), r.priority(), r.avgProcessingSeconds(),
                r.isActive(), r.transferTypeId(), r.effectiveFrom(), r.effectiveTo());
    }
}
