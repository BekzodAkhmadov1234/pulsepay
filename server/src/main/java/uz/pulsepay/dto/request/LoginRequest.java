package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for {@code POST /api/v1/auth/login}.
 *
 * <p><strong>Temporary:</strong> OTP verification is skipped in this phase.
 * The endpoint authenticates by phone number alone and returns a JWT immediately.
 * OTP enforcement will be added when the SMS gateway is integrated.
 */
@Schema(description = "Login payload: registered phone number")
public record LoginRequest(

        @Schema(
                description = "Registered E.164 Uzbekistan mobile number",
                example = "+998901234567"
        )
        @NotBlank(message = "Phone number must not be blank")
        @Pattern(
                regexp = "\\+998\\d{9}",
                message = "Phone number must be a valid Uzbekistan E.164 number (+998XXXXXXXXX)"
        )
        String phoneE164

) {}
