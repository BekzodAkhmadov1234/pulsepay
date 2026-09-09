package uz.pulsepay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Single card statement transaction entry")
public record CardStatementEntry(
        @Schema(description = "Transaction date (ISO-8601)") String date,
        @Schema(description = "Transaction description") String description,
        @Schema(description = "Amount in UZS; negative = debit, positive = credit") BigDecimal amountUzs,
        @Schema(description = "'debit' or 'credit'") String type
) {}
