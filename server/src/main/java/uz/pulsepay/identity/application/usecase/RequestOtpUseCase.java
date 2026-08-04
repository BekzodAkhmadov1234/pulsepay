package uz.pulsepay.identity.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.port.in.RequestOtpPort;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.identity.domain.service.OtpDomainService;
import uz.pulsepay.shared.exception.NotFoundException;

import java.util.UUID;

@Slf4j
@Service
public class RequestOtpUseCase implements RequestOtpPort {

    private final UserRepository userRepository;
    private final OtpDomainService otpDomainService;

    public RequestOtpUseCase(UserRepository userRepository, OtpDomainService otpDomainService) {
        this.userRepository = userRepository;
        this.otpDomainService = otpDomainService;
    }

    @Override
    public void requestOtp(UUID userId, OtpPurpose purpose, UUID targetId) {
        log.info("OTP requested: userId={}, purpose={}", userId, purpose);
        userRepository.findById(userId)
                .filter(u -> u.isActive())
                .orElseThrow(() -> new NotFoundException("User not found or inactive"));
        String rawCode = otpDomainService.generateAndSave(userId, purpose, targetId);
        log.debug("OTP generated and saved: userId={}, purpose={}", userId, purpose);
        // TODO: dispatch rawCode via SMS gateway
        log.warn("SMS dispatch not yet implemented — OTP will not be delivered: userId={}", userId);
    }
}
