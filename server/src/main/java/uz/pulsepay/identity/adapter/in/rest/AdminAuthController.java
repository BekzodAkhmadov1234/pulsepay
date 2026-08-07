package uz.pulsepay.identity.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.identity.adapter.in.rest.dto.AdminLoginRequest;
import uz.pulsepay.identity.adapter.in.rest.dto.TokenResponse;
import uz.pulsepay.identity.domain.port.in.LoginAdminPort;

@Tag(name = "Admin Auth", description = "Admin authentication — issues admin JWTs")
@RestController
@RequestMapping("/admin/v1/auth")
public class AdminAuthController {

    private final LoginAdminPort loginAdminPort;

    public AdminAuthController(LoginAdminPort loginAdminPort) {
        this.loginAdminPort = loginAdminPort;
    }

    @Operation(summary = "Admin login", description = "Authenticate with email + password; returns a signed admin JWT.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        String token = loginAdminPort.login(request.email(), request.password());
        return ResponseEntity.ok(TokenResponse.of(token, 0L));
    }
}
