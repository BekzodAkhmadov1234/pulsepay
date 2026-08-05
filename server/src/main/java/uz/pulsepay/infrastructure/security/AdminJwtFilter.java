package uz.pulsepay.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.pulsepay.shared.audit.AuditContext;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Stateless JWT authentication filter for the admin API plane ({@code /admin/**}).
 *
 * <p>Validates tokens whose {@code type} claim equals {@code "admin"} and grants
 * {@code ROLE_ADMIN} plus a role-specific authority (e.g. {@code ROLE_OPS}, {@code ROLE_SUPER}).
 * Also populates {@link AuditContext} with the admin UUID so that JPA audit fields
 * ({@code @CreatedBy} / {@code @LastModifiedBy}) are stamped correctly.
 *
 * <p>Token validation is fully delegated to {@link JwtService}, which handles signature
 * verification and expiry checking in a single call.
 */
@Slf4j
public class AdminJwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtService jwtService;

    public AdminJwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = jwtService.extractAllClaims(header.substring(BEARER_PREFIX.length()));

                if (JwtService.TYPE_ADMIN.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                    String role    = claims.get(JwtService.CLAIM_ROLE, String.class);
                    UUID   adminId = UUID.fromString(claims.getSubject());

                    var auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                            ));

                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Stamp the audit context so JPA @CreatedBy / @LastModifiedBy capture the admin ID.
                    AuditContext.setAdminId(adminId);
                    log.trace("Admin security context set: adminId={}, role={}", adminId, role);
                }

            } catch (IllegalArgumentException ex) {
                // Token is expired, tampered with, or structurally invalid.
                // Proceed unauthenticated; Spring Security's route authorization enforces 401/403.
                log.debug("Admin JWT validation failed: {}", ex.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the thread-local audit context to prevent leaks across requests
            // when the servlet container reuses threads from a pool.
            AuditContext.clear();
        }
    }
}
