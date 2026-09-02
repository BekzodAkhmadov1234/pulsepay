package uz.pulsepay.utils.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication filter for the user API plane ({@code /api/**}).
 *
 * <h3>Processing pipeline (per request)</h3>
 * <ol>
 *   <li>Extract the raw token from the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validate signature and expiry via {@link JwtService#extractAllClaims(String)}.</li>
 *   <li>Reject tokens whose {@code type} claim is not {@value JwtService#TYPE_USER}.</li>
 *   <li>Extract {@code phone_e164} and {@code kyc_level} claims from the verified payload.</li>
 *   <li>Build Spring Security's native {@link User} as the principal:
 *       <ul>
 *         <li><b>username</b> = {@code phone_e164} — the authenticated user identity</li>
 *         <li><b>password</b> = {@code ""} — irrelevant in a stateless Bearer scheme</li>
 *         <li><b>authorities</b> = {@code [ROLE_<KYC_LEVEL>]} — drives method-level security</li>
 *       </ul>
 *   </li>
 *   <li>Store the user UUID string in
 *       {@link UsernamePasswordAuthenticationToken#setDetails(Object)} so that controllers
 *       can retrieve it from the {@code Authentication} object without a database round-trip:
 *       <pre>{@code String userId = (String) ((UsernamePasswordAuthenticationToken) auth).getDetails();}</pre>
 *   </li>
 *   <li>Populate the {@link SecurityContextHolder} and continue the filter chain.</li>
 * </ol>
 *
 * <h3>Domain model isolation</h3>
 * The domain {@link uz.pulsepay.domain.identity.User} record is <em>never</em> loaded
 * here. Only the claim values already embedded in the token at generation time are used,
 * eliminating both a database round-trip and any coupling between the security pipeline
 * and the domain layer.
 *
 * <h3>Failure behaviour</h3>
 * Any token validation failure (expired, tampered, malformed, missing claims) is silently
 * swallowed. The request proceeds unauthenticated, and Spring Security's access-control
 * layer enforces a {@code 401 Unauthorized} at the route level.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // ── Filter entry point ────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        String rawToken = extractBearerToken(request);

        if (rawToken != null) {
            populateSecurityContext(rawToken);
        }

        chain.doFilter(request, response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void populateSecurityContext(String rawToken) {
        try {
            Claims claims = jwtService.extractAllClaims(rawToken);

            // Discard admin tokens — they are handled by AdminJwtFilter on the /admin/** chain.
            if (!JwtService.TYPE_USER.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                log.debug("Token type is not '{}'; skipping user authentication", JwtService.TYPE_USER);
                return;
            }

            String phoneE164 = claims.get(JwtService.CLAIM_PHONE,     String.class);
            String kycLevel  = claims.get(JwtService.CLAIM_KYC_LEVEL, String.class);
            String userId    = claims.getSubject(); // UUID string

            if (!StringUtils.hasText(phoneE164) || !StringUtils.hasText(kycLevel)) {
                log.debug("User token is missing required claims (phone_e164 or kyc_level)");
                return;
            }

            // NOTE: this is org.springframework.security.core.userdetails.User — the Spring
            // Security utility class — NOT the domain model uz.pulsepay.domain.identity.User.
            UserDetails springPrincipal = User.builder()
                    .username(phoneE164)
                    .password("")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + kycLevel.toUpperCase())))
                    .build();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            springPrincipal,
                            null,
                            springPrincipal.getAuthorities());

            // Stash the user UUID in the token's details slot.
            // Controllers retrieve it with: (String) authentication.getDetails()
            authToken.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.trace("Security context set: phone={}, kycLevel={}, userId={}", phoneE164, kycLevel, userId);

        } catch (IllegalArgumentException ex) {
            log.debug("JWT authentication failed for request: {}", ex.getMessage());
        }
    }
}
