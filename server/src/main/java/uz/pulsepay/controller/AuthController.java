package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.config.JwtProperties;
import uz.pulsepay.dto.request.LoginRequest;
import uz.pulsepay.dto.request.RegisterConfirmRequest;
import uz.pulsepay.dto.request.RegisterOtpRequest;
import uz.pulsepay.dto.request.RequestOtpRequest;
import uz.pulsepay.dto.request.VerifyOtpRequest;
import uz.pulsepay.dto.response.AuthResponse;
import uz.pulsepay.dto.response.TokenResponse;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.service.UserAuthService;
import uz.pulsepay.utils.security.JwtService;

@Tag(name = "Authentication", description = "Registration, login, and OTP-based authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAuthService userAuthService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(UserAuthService userAuthService,
                          JwtService jwtService,
                          JwtProperties jwtProperties) {
        this.userAuthService = userAuthService;
        this.jwtService      = jwtService;
        this.jwtProperties   = jwtProperties;
    }

    // ── POST /api/v1/auth/register/otp ────────────────────────────────────────

    @Operation(summary = "Step 1 of registration — send OTP",
               description = "Sends a 6-digit OTP to the given phone number. "
                           + "Creates a pending account if the phone is new. "
                           + "Returns 409 if the phone is already registered and active.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "OTP dispatched"),
            @ApiResponse(responseCode = "409", description = "Phone already registered")
    })
    @PostMapping("/register/otp")
    public ResponseEntity<Void> registerOtp(@Valid @RequestBody RegisterOtpRequest request) {
        userAuthService.registerOtp(request.phoneE164());
        return ResponseEntity.accepted().build();
    }

    // ── POST /api/v1/auth/register/confirm ────────────────────────────────────

    @Operation(summary = "Step 2 of registration — verify OTP and activate account",
               description = "Verifies the OTP sent to the phone, sets the user's full name, "
                           + "activates the account, and returns a JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account activated — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "No pending registration found for this phone")
    })
    @PostMapping("/register/confirm")
    public ResponseEntity<TokenResponse> registerConfirm(
            @Valid @RequestBody RegisterConfirmRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String accessToken = userAuthService.registerConfirm(
                request.phoneE164(), request.code(), request.fullName(),
                request.resolvedFingerprint(), request.resolvedPlatform(), ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.of(accessToken, jwtProperties.getUserExpirySeconds()));
    }

    // ── POST /api/v1/auth/otp ──────────────────────────────────────────────────

    @Operation(summary = "Request login OTP",
               description = "Sends a 6-digit OTP to the registered phone. Valid for 59 seconds.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "OTP dispatched"),
            @ApiResponse(responseCode = "404", description = "Phone not registered")
    })
    @PostMapping("/otp")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        User user = userAuthService.findByPhone(request.phoneE164());
        userAuthService.requestOtp(user.id(), OtpPurpose.LOGIN, null);
        return ResponseEntity.accepted().build();
    }

    // ── POST /api/v1/auth/verify ───────────────────────────────────────────────

    @Operation(summary = "Verify OTP and obtain JWT",
               description = "Validates the OTP and issues an access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP accepted — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Phone not registered")
    })
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                   HttpServletRequest httpRequest) {
        User user = userAuthService.findByPhone(request.phoneE164());
        String ip = httpRequest.getRemoteAddr();
        UserAuthService.VerifyOtpResult result = userAuthService.verifyOtp(
                user.id(), request.code(), OtpPurpose.LOGIN,
                request.deviceFingerprint(), request.platform(), ip);
        String accessToken = jwtService.generateUserToken(user);
        return ResponseEntity.ok(AuthResponse.of(accessToken, "refresh-token-placeholder", 900,
                result.requiresBiometricStepUp()));
    }

    // ── POST /api/v1/auth/login (dev convenience — no OTP) ────────────────────

    @Operation(summary = "Dev-only: login by phone without OTP",
               description = "Returns a JWT directly. Only for development and testing — "
                           + "not for production use.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String accessToken = userAuthService.login(request.phoneE164());
        return ResponseEntity.ok(TokenResponse.of(accessToken, jwtProperties.getUserExpirySeconds()));
    }
}
