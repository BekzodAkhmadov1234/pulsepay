package uz.pulsepay.identity.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.identity.adapter.in.rest.dto.AuthResponse;
import uz.pulsepay.identity.adapter.in.rest.dto.RequestOtpRequest;
import uz.pulsepay.identity.adapter.in.rest.dto.VerifyOtpRequest;
import uz.pulsepay.identity.application.usecase.VerifyOtpUseCase;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.in.RequestOtpPort;
import uz.pulsepay.identity.domain.port.in.VerifyOtpPort;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.infrastructure.security.JwtTokenProvider;
import uz.pulsepay.shared.exception.NotFoundException;

@Tag(name = "Authentication", description = "OTP-based passwordless login for registered users")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RequestOtpPort requestOtpPort;
    private final VerifyOtpPort verifyOtpPort;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserRepository userRepository,
                          RequestOtpPort requestOtpPort,
                          VerifyOtpPort verifyOtpPort,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.requestOtpPort = requestOtpPort;
        this.verifyOtpPort = verifyOtpPort;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(summary = "Request login OTP",
               description = "Sends a 6-digit OTP to the registered phone number. Valid for 59 seconds; 3 failed attempts trigger a 15-minute lockout.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "OTP dispatched"),
            @ApiResponse(responseCode = "404", description = "Phone number not registered"),
            @ApiResponse(responseCode = "429", description = "OTP lockout active")
    })
    @PostMapping("/otp")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        User user = userRepository.findByPhoneE164(request.phoneE164())
                .orElseThrow(() -> new NotFoundException("User not registered"));
        requestOtpPort.requestOtp(user.id(), OtpPurpose.LOGIN, null);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Verify OTP and obtain JWT",
               description = "Validates the OTP and issues a short-lived access token (15 min) plus a refresh token. "
                       + "If a new device is detected, `requiresBiometricStepUp=true` is returned and the client must complete AUTH-02 biometric confirmation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP accepted — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Phone number not registered")
    })
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                   HttpServletRequest httpRequest) {
        User user = userRepository.findByPhoneE164(request.phoneE164())
                .orElseThrow(() -> new NotFoundException("User not registered"));
        String ip = httpRequest.getRemoteAddr();
        VerifyOtpUseCase.VerifyOtpResult result = verifyOtpPort.verifyOtp(
                user.id(), request.code(), OtpPurpose.LOGIN,
                request.deviceFingerprint(), request.platform(), ip);
        String accessToken = jwtTokenProvider.generateUserToken(user.id(), result.session().id());
        // requiresBiometricStepUp is included in the response so the client can prompt AUTH-02
        return ResponseEntity.ok(AuthResponse.of(accessToken, "refresh-token-placeholder", 900,
                result.requiresBiometricStepUp()));
    }
}
