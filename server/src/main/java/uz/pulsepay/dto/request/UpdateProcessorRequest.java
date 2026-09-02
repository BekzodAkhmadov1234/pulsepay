package uz.pulsepay.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProcessorRequest(@NotBlank String processorName) {}
