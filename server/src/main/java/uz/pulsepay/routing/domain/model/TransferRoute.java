package uz.pulsepay.routing.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TransferRoute(
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
) {}
