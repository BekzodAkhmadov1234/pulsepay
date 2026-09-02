package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Phone number to send the login OTP to")
public record RequestOtpRequest(
        @Schema(description = "E.164 Uzbekistan mobile number", example = "+998901234567")
        @NotBlank @Pattern(regexp = "\\+998\\d{9}") String phoneE164
) {}
