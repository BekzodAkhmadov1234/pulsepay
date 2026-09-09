package uz.pulsepay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloseAccountConfirmRequest(
        @NotBlank @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        String code
) {}
