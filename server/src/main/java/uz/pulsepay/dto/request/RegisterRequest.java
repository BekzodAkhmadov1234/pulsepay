package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/v1/auth/register}.
 *
 * <p>All fields are validated by Bean Validation before the request reaches the use case.
 * Constraint violations produce a structured {@code 400 Bad Request} response via the
 * global exception handler.
 */
@Schema(description = "New user registration payload")
public record RegisterRequest(

        @Schema(
                description = "E.164-formatted Uzbekistan mobile number (unique account key)",
                example = "+998901234567"
        )
        @NotBlank(message = "Phone number must not be blank")
        @Pattern(
                regexp = "\\+998\\d{9}",
                message = "Phone number must be a valid Uzbekistan E.164 number (+998XXXXXXXXX)"
        )
        String phoneE164,

        @Schema(
                description = "User's full display name",
                example = "Jasur Yusupov",
                minLength = 2,
                maxLength = 100
        )
        @NotBlank(message = "Full name must not be blank")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName

) {}
