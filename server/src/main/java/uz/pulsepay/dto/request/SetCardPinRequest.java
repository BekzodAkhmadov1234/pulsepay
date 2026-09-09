package uz.pulsepay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SetCardPinRequest(
        @NotBlank
        @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
        @Pattern(regexp = "\\d{4}", message = "PIN must contain only digits")
        String pin
) {}
