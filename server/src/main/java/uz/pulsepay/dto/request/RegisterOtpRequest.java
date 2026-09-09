package uz.pulsepay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterOtpRequest(
        @NotBlank
        @Pattern(regexp = "\\+\\d{7,15}", message = "Phone must be in E.164 format")
        String phoneE164
) {}
