package uz.pulsepay.controller;

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
import uz.pulsepay.service.MerchantService;

@Tag(name = "Merchant Auth", description = "Merchant portal authentication")
@RestController
@RequestMapping("/merchant/v1/auth")
public class MerchantAuthController {

    private final MerchantService merchantService;

    public MerchantAuthController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType) {}

    @Operation(summary = "Merchant login — returns JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        String token = merchantService.login(req.email(), req.password());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }
}
