package uz.pulsepay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Available card spending-limit type (HUMO-specific)")
public record CardLimitTypeDto(
        @Schema(description = "Limit type code, e.g. LIM_1DAY_CASHLESS") String code,
        @Schema(description = "Category: CASHLESS | CASH | TRANSFER") String type,
        @Schema(description = "Human-readable name") String name,
        @Schema(description = "True when a custom date range must be provided") boolean selectDate
) {}
