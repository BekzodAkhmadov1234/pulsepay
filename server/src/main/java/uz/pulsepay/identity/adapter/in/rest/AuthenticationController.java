package uz.pulsepay.identity.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.config.JwtProperties;
import uz.pulsepay.identity.adapter.in.rest.dto.LoginRequest;
import uz.pulsepay.identity.adapter.in.rest.dto.RegisterRequest;
import uz.pulsepay.identity.adapter.in.rest.dto.TokenResponse;
import uz.pulsepay.identity.domain.port.in.LoginUserPort;
import uz.pulsepay.identity.domain.port.in.RegisterUserPort;

/**
 * REST adapter for user onboarding and credential-based login.
 *
 * <p>This controller is mounted at {@code /api/v1/auth} (alongside the existing
 * {@link AuthController} which handles the OTP-request step). All endpoints in this
 * class are publicly accessible — they are explicitly permitted in {@link uz.pulsepay.config.SecurityConfig}
 * before any authentication is required.
 *
 * <h3>Endpoint summary</h3>
 * <ul>
 *   <li>{@code POST /api/v1/auth/register} — create a new account and receive a JWT immediately.</li>
 *   <li>{@code POST /api/v1/auth/login} — verify a previously requested OTP and receive a JWT.</li>
 * </ul>
 *
 * <h3>Domain model isolation</h3>
 * This controller depends only on the inbound port interfaces {@link RegisterUserPort} and
 * {@link LoginUserPort}. The domain {@link uz.pulsepay.identity.domain.model.User} record and
 * Spring Security internals are never referenced here.
 */
@Tag(name = "Authentication", description = "User registration and OTP-based login")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final RegisterUserPort registerUserPort;
    private final LoginUserPort    loginUserPort;
    private final JwtProperties    jwtProperties;

    public AuthenticationController(RegisterUserPort registerUserPort,
                                    LoginUserPort    loginUserPort,
                                    JwtProperties    jwtProperties) {
        this.registerUserPort = registerUserPort;
        this.loginUserPort    = loginUserPort;
        this.jwtProperties    = jwtProperties;
    }

    // ── POST /api/v1/auth/register ────────────────────────────────────────────────

    @Operation(
            summary = "Register a new user account",
            description = """
                    Creates a new PulsePay account for the supplied phone number.
                    On success, a JWT access token is returned immediately — no separate
                    login step is required after registration.

                    The account is initialised with:
                    - status = 'active'
                    - kycLevel = 'basic'

                    Returns **409 Conflict** if the phone number is already registered.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid phone format, blank name, etc.)"),
            @ApiResponse(responseCode = "409", description = "Phone number already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        String accessToken = registerUserPort.register(request.phoneE164(), request.fullName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TokenResponse.of(accessToken, jwtProperties.getUserExpirySeconds()));
    }

    // ── POST /api/v1/auth/login ───────────────────────────────────────────────────

    @Operation(
            summary = "Login by phone number",
            description = """
                    Authenticates an existing user by phone number and returns a JWT access token.

                    **Temporary:** OTP verification is skipped in this phase. The endpoint will
                    require a valid OTP code once the SMS gateway is integrated.

                    Returns **403 Forbidden** if the account is inactive or closed.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid phone format)"),
            @ApiResponse(responseCode = "403", description = "Account is inactive or closed"),
            @ApiResponse(responseCode = "404", description = "Phone number not registered")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request) {

        String accessToken = loginUserPort.login(request.phoneE164());

        return ResponseEntity.ok(
                TokenResponse.of(accessToken, jwtProperties.getUserExpirySeconds()));
    }
}
