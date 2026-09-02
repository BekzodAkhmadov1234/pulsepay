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
import uz.pulsepay.dto.request.AdminLoginRequest;
import uz.pulsepay.dto.response.AuthResponse;
import uz.pulsepay.dto.request.LoginRequest;
import uz.pulsepay.dto.request.RegisterRequest;
import uz.pulsepay.dto.request.RequestOtpRequest;
import uz.pulsepay.dto.response.TokenResponse;
import uz.pulsepay.dto.request.VerifyOtpRequest;
import uz.pulsepay.domain.identity.OtpPurpose;
import uz.pulsepay.domain.identity.User;
import uz.pulsepay.service.UserAuthService;
import uz.pulsepay.utils.security.JwtService;

@Tag(name = "Authentication", description = "User registration, login, and OTP-based authentication")
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

    // ── POST /api/v1/auth/register ─────────────────────────────────────────────

    @Operation(summary = "Register a new user account",
               description = "Creates a new PulsePay account. On success, a JWT access token is returned immediately.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Phone number already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        String accessToken = userAuthService.register(request.phoneE164(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.of(accessToken, jwtProperties.getUserExpirySeconds()));
    }

    // ── POST /api/v1/auth/login ────────────────────────────────────────────────

    @Operation(summary = "Login by phone number",
               description = "Authenticates an existing user and returns a JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned"),
            @ApiResponse(responseCode = "403", description = "Account is inactive"),
            @ApiResponse(responseCode = "404", description = "Phone number not registered")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String accessToken = userAuthService.login(request.phoneE164());
        return ResponseEntity.ok(TokenResponse.of(accessToken, jwtProperties.getUserExpirySeconds()));
    }

    // ── POST /api/v1/auth/otp ──────────────────────────────────────────────────

    @Operation(summary = "Request login OTP",
               description = "Sends a 6-digit OTP to the registered phone number. Valid for 59 seconds.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "OTP dispatched"),
            @ApiResponse(responseCode = "404", description = "Phone number not registered")
    })
    @PostMapping("/otp")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        User user = userAuthService.findByPhone(request.phoneE164());
        userAuthService.requestOtp(user.id(), OtpPurpose.LOGIN, null);
        return ResponseEntity.accepted().build();
    }

    // ── POST /api/v1/auth/verify ───────────────────────────────────────────────

    @Operation(summary = "Verify OTP and obtain JWT",
               description = "Validates the OTP and issues a short-lived access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP accepted — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Phone number not registered")
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
}
