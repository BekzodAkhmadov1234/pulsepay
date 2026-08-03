package uz.pulsepay.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.pulsepay.shared.audit.AuditContext;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class AdminJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public AdminJwtFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = jwtTokenProvider.validateAndParseClaims(header.substring(7));
                if ("admin".equals(claims.get("type"))) {
                    String role = (String) claims.get("role");
                    var auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    AuditContext.setAdminId(UUID.fromString(claims.getSubject()));
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid token — proceed unauthenticated
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }
}
