package uz.pulsepay.identity.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RequestOtpRequest(
        @NotBlank @Pattern(regexp = "\\+998\\d{9}") String phoneE164
) {}
