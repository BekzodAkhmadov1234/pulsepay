package uz.pulsepay.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.service.OtpService;
import uz.pulsepay.service.UserAuthService;

import java.util.Map;

/**
 * Dev-only endpoint that surfaces the last generated OTP for a phone number.
 * Only active when the {@code dev} Spring profile is enabled.
 * Never reachable in production.
 */
@RestController
@Profile("dev")
@RequestMapping("/api/v1/dev")
public class DevController {

    private final UserAuthService userAuthService;
    private final OtpService otpService;

    public DevController(UserAuthService userAuthService, OtpService otpService) {
        this.userAuthService = userAuthService;
        this.otpService      = otpService;
    }

    @GetMapping("/otp/{phoneE164}")
    public ResponseEntity<Map<String, String>> getOtp(@PathVariable String phoneE164) {
        try {
            var user = userAuthService.findByPhone(phoneE164);
            return otpService.devGet(user.id())
                    .map(code -> ResponseEntity.ok(Map.of("code", code)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (uz.pulsepay.domain.shared.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
