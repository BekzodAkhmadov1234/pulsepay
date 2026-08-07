package uz.pulsepay.identity.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.domain.exception.AccountInactiveException;
import uz.pulsepay.identity.domain.model.Admin;
import uz.pulsepay.identity.domain.port.in.LoginAdminPort;
import uz.pulsepay.identity.domain.port.out.AdminRepository;
import uz.pulsepay.infrastructure.security.JwtService;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

@Slf4j
@Service
public class LoginAdminUseCase implements LoginAdminPort {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginAdminUseCase(AdminRepository adminRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder  = passwordEncoder;
        this.jwtService       = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public String login(String email, String password) {
        log.info("Admin login attempt: email={}", email);

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No admin account found for email: " + email));

        if (!admin.isActive()) {
            log.warn("Admin login rejected — account inactive: adminId={}", admin.id());
            throw new AccountInactiveException(email);
        }

        if (!passwordEncoder.matches(password, admin.passwordHash())) {
            log.warn("Admin login failed — wrong password: adminId={}", admin.id());
            throw new DomainException("Invalid credentials");
        }

        log.info("Admin login successful: adminId={}, role={}", admin.id(), admin.role());
        return jwtService.generateAdminToken(admin.id(), admin.role().name().toLowerCase(), admin.email());
    }
}
