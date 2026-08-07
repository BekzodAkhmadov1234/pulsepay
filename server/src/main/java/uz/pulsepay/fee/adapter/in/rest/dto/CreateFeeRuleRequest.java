package uz.pulsepay.fee.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.pulsepay.fee.domain.model.FeePayer;
import uz.pulsepay.fee.domain.model.FeeRecipient;
import uz.pulsepay.fee.domain.model.FeeType;

import java.time.Instant;
import java.util.List;

public record CreateFeeRuleRequest(
        @NotBlank String name,
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
