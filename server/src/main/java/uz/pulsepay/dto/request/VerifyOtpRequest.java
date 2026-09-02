package uz.pulsepay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "OTP verification payload")
public record VerifyOtpRequest(
        @Schema(description = "E.164 Uzbekistan mobile number", example = "+998901234567")
        @NotBlank @Pattern(regexp = "\\+998\\d{9}") String phoneE164,

        @Schema(description = "6-digit OTP received via SMS", example = "482916")
        @NotBlank @Size(min = 6, max = 6) String code,

        @Schema(description = "Stable device identifier used for new-device detection (AUTH-01)")
        @NotBlank String deviceFingerprint,

        @Schema(description = "Client platform", allowableValues = {"ios", "android"}, example = "android")
        @NotBlank @Pattern(regexp = "ios|android") String platform
) {}
