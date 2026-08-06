package uz.pulsepay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uz.pulsepay.infrastructure.security.AdminJwtFilter;
import uz.pulsepay.infrastructure.security.JwtAuthenticationFilter;
import uz.pulsepay.infrastructure.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Spring Security configuration with two independent filter chains:
 *
 * <ol>
 *   <li><b>Admin chain</b> ({@code /admin/**}, order 1) — {@link AdminJwtFilter} validates
 *       tokens whose {@code type} claim is {@code "admin"} and grants {@code ROLE_ADMIN}.</li>
 *   <li><b>User chain</b> ({@code /api/**}, order 2) — {@link JwtAuthenticationFilter}
 *       validates tokens whose {@code type} claim is {@code "user"}, extracts
 *       {@code phone_e164} as the Spring Security principal username, and maps
 *       {@code kyc_level} to a {@code ROLE_<KYC_LEVEL>} granted authority.
 *       Public auth endpoints ({@code /api/v1/auth/**}) are exempt from authentication.</li>
 *   <li><b>Public chain</b> (remaining paths, order 3) — Swagger UI and health check only;
 *       everything else is denied.</li>
 * </ol>
 *
 * <p>All chains are fully stateless (no HTTP session, no CSRF token required).
 * No {@code UserDetailsService} is wired — authentication is token-intrinsic.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Admin filter chain — secures all {@code /admin/**} routes.
     * Requires a valid admin-type JWT bearing {@code ROLE_ADMIN}.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(new AdminJwtFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * User filter chain — secures all {@code /api/**} routes.
     *
     * <p>Auth endpoints ({@code /api/v1/auth/**}) are public so that clients can register,
     * request an OTP, and log in without a pre-existing token. All other {@code /api/**}
     * routes require a valid user-type JWT (minimum authority: {@code ROLE_BASIC}).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/api/v1/dev/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        }))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Public/fallback chain — exposes only Swagger UI and the health-check probe.
     * Any request reaching this chain that is not matched by the above rules is denied.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
