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

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication filter for the merchant API plane ({@code /merchant/**}).
 *
 * Validates tokens whose {@code type} claim equals {@code "merchant"} and grants
 * {@code ROLE_MERCHANT}. The {@code sub} claim carries the merchant UUID.
 */
@Slf4j
public class MerchantJwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtService jwtService;

    public MerchantJwtFilter(JwtService jwtService) {
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

                if (JwtService.TYPE_MERCHANT.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.trace("Merchant security context set: merchantId={}", claims.getSubject());
                }

            } catch (IllegalArgumentException ex) {
                log.debug("Merchant JWT validation failed: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
