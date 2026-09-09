package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.CloseAccountConfirmRequest;
import uz.pulsepay.service.UserAuthService;

import java.util.UUID;

@Tag(name = "User", description = "User account management")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserAuthService userAuthService;

    public UserController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    // ── POST /api/v1/user/close/otp ───────────────────────────────────────────

    @Operation(summary = "Step 1 of account deletion — send OTP",
               description = "Sends a 6-digit confirmation OTP to the user's registered phone. "
                           + "The user must be active.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "OTP dispatched"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "400", description = "Account is not active")
    })
    @PostMapping("/close/otp")
    public ResponseEntity<Void> closeAccountOtp(Authentication authentication) {
        userAuthService.closeAccountOtp(extractUserId(authentication));
        return ResponseEntity.accepted().build();
    }

    // ── POST /api/v1/user/close/confirm ──────────────────────────────────────

    @Operation(summary = "Step 2 of account deletion — confirm with OTP",
               description = "Verifies the OTP and permanently closes the account. "
                           + "All active sessions are revoked immediately.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account closed"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/close/confirm")
    public ResponseEntity<Void> closeAccountConfirm(@Valid @RequestBody CloseAccountConfirmRequest request,
                                                     Authentication authentication) {
        userAuthService.closeAccountConfirm(extractUserId(authentication), request.code());
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static UUID extractUserId(Authentication authentication) {
        String raw = (String) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return UUID.fromString(raw);
    }
}
