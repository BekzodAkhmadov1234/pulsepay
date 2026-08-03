package uz.pulsepay.identity.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank @Pattern(regexp = "\\+998\\d{9}") String phoneE164,
        @NotBlank @Size(min = 6, max = 6) String code,
        @NotBlank String deviceFingerprint,
        @NotBlank @Pattern(regexp = "ios|android") String platform
) {}
