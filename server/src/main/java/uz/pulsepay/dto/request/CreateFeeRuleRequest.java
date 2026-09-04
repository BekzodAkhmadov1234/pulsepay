package uz.pulsepay.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.pulsepay.domain.fee.FeePayer;
import uz.pulsepay.domain.fee.FeeRecipient;
import uz.pulsepay.domain.fee.FeeType;

import java.time.Instant;
import java.util.List;

public record CreateFeeRuleRequest(
        @NotBlank String name,
        @Size(max = 10) String mode,
        @Size(max = 20) String sourceNetwork,
        @Size(max = 20) String destinationNetwork,
        @Min(0) long minAmount,
        Long maxAmount,
        @NotNull FeeType feeType,
        Long fixedAmount,
        Integer percentageBps,
        Long minFeeAmount,
        Long maxFeeAmount,
        @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @Min(1) int priority,
        Instant effectiveFrom,
        Instant effectiveTo,
        Integer transferTypeId,
        @NotNull FeePayer feePayer,
        @NotNull FeeRecipient feeRecipient,
        List<AddTierRequest> tiers
) {}
