package uz.pulsepay.routing.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record CreateRouteRequest(
        @NotBlank String routeCode,
        @NotNull @Pattern(regexp = "uzcard|humo") String sourceNetwork,
        @NotNull @Pattern(regexp = "uzcard|humo") String destinationNetwork,
        @NotBlank String processorName,
        Long maxAmount,
        int priority,
        Integer avgProcessingSeconds,
        Integer transferTypeId,
        Instant effectiveFrom,
        Instant effectiveTo
) {}
