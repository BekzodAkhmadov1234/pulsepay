package uz.pulsepay.identity.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.port.in.RequestOtpPort;
import uz.pulsepay.identity.domain.port.out.UserRepository;
import uz.pulsepay.identity.domain.service.OtpDomainService;
import uz.pulsepay.shared.exception.NotFoundException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RequestOtpUseCase implements RequestOtpPort {

    private final UserRepository userRepository;
    private final OtpDomainService otpDomainService;
    private final Optional<DevOtpStore> devOtpStore;

    public RequestOtpUseCase(UserRepository userRepository,
                             OtpDomainService otpDomainService,
                             Optional<DevOtpStore> devOtpStore) {
        this.userRepository = userRepository;
        this.otpDomainService = otpDomainService;
        this.devOtpStore = devOtpStore;
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
        log.warn("SMS dispatch not yet implemented — OTP will not be delivered: userId={}, rawCode={}", userId, rawCode);
        devOtpStore.ifPresent(s -> s.put(userId, rawCode));
    }
}
