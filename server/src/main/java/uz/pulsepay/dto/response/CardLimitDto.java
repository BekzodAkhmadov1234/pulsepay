package uz.pulsepay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Active spending limit on a card")
public record CardLimitDto(
        @Schema(description = "Limit type code") String type,
        @Schema(description = "Human-readable name") String name,
        @Schema(description = "Limit amount in UZS") BigDecimal valueUzs,
        @Schema(description = "Effective from (ISO date)") String from,
        @Schema(description = "Effective to (ISO date)") String to
) {}
