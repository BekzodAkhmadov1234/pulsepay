package uz.pulsepay.fee.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;

public record AddTierRequest(
        @Min(0) long tierMinAmount,
        Long tierMaxAmount,
        Long fixedAmount,
        Integer percentageBps
) {}
