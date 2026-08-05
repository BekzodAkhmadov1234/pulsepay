package uz.pulsepay.identity.adapter.in.rest;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.identity.application.usecase.DevOtpStore;
import uz.pulsepay.identity.domain.port.out.UserRepository;

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

    private final UserRepository userRepository;
    private final DevOtpStore devOtpStore;

    public DevController(UserRepository userRepository, DevOtpStore devOtpStore) {
        this.userRepository = userRepository;
        this.devOtpStore = devOtpStore;
    }

    @GetMapping("/otp/{phoneE164}")
    public ResponseEntity<Map<String, String>> getOtp(@PathVariable String phoneE164) {
        return userRepository.findByPhoneE164(phoneE164)
                .flatMap(user -> devOtpStore.get(user.id()))
                .map(code -> ResponseEntity.ok(Map.of("code", code)))
                .orElse(ResponseEntity.notFound().build());
    }
}
