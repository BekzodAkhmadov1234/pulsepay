package uz.pulsepay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MobileTopUpRequest(
        @NotBlank(message = "phone is required")
        @Pattern(regexp = "\\+?[0-9]{9,15}", message = "Invalid phone number format")
        String phone,

        /** Optional: override the default mobile provider (e.g. "beeline-uzb"). Defaults to "mobile-uzb". */
        String serviceCode
) {}
