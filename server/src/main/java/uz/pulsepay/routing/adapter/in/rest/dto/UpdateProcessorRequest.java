package uz.pulsepay.routing.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProcessorRequest(@NotBlank String processorName) {}
