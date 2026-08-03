package uz.pulsepay.identity.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.pulsepay.identity.adapter.in.rest.dto.AuthResponse;
import uz.pulsepay.identity.adapter.in.rest.dto.RequestOtpRequest;
import uz.pulsepay.identity.adapter.in.rest.dto.VerifyOtpRequest;
import uz.pulsepay.identity.application.usecase.VerifyOtpUseCase;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.model.User;
import uz.pulsepay.identity.domain.port.in.RequestOtpPort;
import uz.pulsepay.identity.domain.port.in.VerifyOtpPort;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.infrastructure.security.JwtTokenProvider;
import uz.pulsepay.shared.exception.NotFoundException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RequestOtpPort requestOtpPort;
    private final VerifyOtpPort verifyOtpPort;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserRepository userRepository,
                          RequestOtpPort requestOtpPort,
                          VerifyOtpPort verifyOtpPort,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.requestOtpPort = requestOtpPort;
        this.verifyOtpPort = verifyOtpPort;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/otp")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        User user = userRepository.findByPhoneE164(request.phoneE164())
                .orElseThrow(() -> new NotFoundException("User not registered"));
        requestOtpPort.requestOtp(user.id(), OtpPurpose.LOGIN, null);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                   HttpServletRequest httpRequest) {
        User user = userRepository.findByPhoneE164(request.phoneE164())
                .orElseThrow(() -> new NotFoundException("User not registered"));
        String ip = httpRequest.getRemoteAddr();
        VerifyOtpUseCase.VerifyOtpResult result = verifyOtpPort.verifyOtp(
                user.id(), request.code(), OtpPurpose.LOGIN,
                request.deviceFingerprint(), request.platform(), ip);
        String accessToken = jwtTokenProvider.generateUserToken(user.id(), result.session().id());
        // requiresBiometricStepUp is included in the response so the client can prompt AUTH-02
        return ResponseEntity.ok(AuthResponse.of(accessToken, "refresh-token-placeholder", 900,
                result.requiresBiometricStepUp()));
    }
}
