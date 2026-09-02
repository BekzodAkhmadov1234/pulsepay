package uz.pulsepay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.domain.identity.AdminEntity;
import uz.pulsepay.domain.identity.AccountInactiveException;
import uz.pulsepay.domain.identity.Admin;
import uz.pulsepay.repository.AdminRepository;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.domain.shared.NotFoundException;
import uz.pulsepay.utils.security.JwtService;

@Slf4j
@Service
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(AdminRepository adminRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder  = passwordEncoder;
        this.jwtService       = jwtService;
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        log.info("Admin login attempt: email={}", email);

        Admin admin = adminRepository.findByEmail(email)
                .map(AdminEntity::toDomain)
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
