package uz.pulsepay.merchant.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.merchant.domain.port.in.MerchantAuthPort;

@Tag(name = "Merchant Auth", description = "Merchant portal authentication")
@RestController
@RequestMapping("/merchant/v1/auth")
public class MerchantAuthController {

    private final MerchantAuthPort merchantAuthPort;

    public MerchantAuthController(MerchantAuthPort merchantAuthPort) {
        this.merchantAuthPort = merchantAuthPort;
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record LoginResponse(String accessToken, String tokenType) {}

    @Operation(summary = "Merchant login — returns JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        String token = merchantAuthPort.login(req.email(), req.password());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }
}
