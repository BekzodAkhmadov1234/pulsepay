package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Request body to validate Paynet prepayment fields (stateless — no DB write)")
public record PaynetPrepaymentRequest(

        @Schema(description = "Paynet service code", example = "gas-uzb")
        @NotBlank String serviceCode,

        @Schema(description = "Provider-specific field values (e.g. {account_number: '12345'})")
        @NotNull Map<@NotBlank String, @NotBlank String> serviceFields
) {}
