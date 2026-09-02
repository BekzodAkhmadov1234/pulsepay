package uz.pulsepay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.dto.request.AdminLoginRequest;
import uz.pulsepay.dto.response.TokenResponse;
import uz.pulsepay.service.AdminAuthService;

@Tag(name = "Admin Auth", description = "Admin authentication — issues admin JWTs")
@RestController
@RequestMapping("/admin/v1/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Admin login", description = "Authenticate with email + password; returns a signed admin JWT.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        String token = adminAuthService.login(request.email(), request.password());
        return ResponseEntity.ok(TokenResponse.of(token, 0L));
    }
}
