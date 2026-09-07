package uz.pulsepay.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.pulsepay.domain.shared.Language;

import java.io.IOException;

/**
 * Reads the {@code X-Lang} request header and sets {@link LocaleContextHolder}
 * for the duration of the request so that {@code MessageSource} and
 * {@link GlobalExceptionHandler} return messages in the correct language.
 *
 * <p>Supported values: {@code uz} (default), {@code ru}, {@code en}, {@code zh}, {@code uz_c}.
 * If the header is absent or unrecognised, the locale defaults to Uzbek Latin ({@code uz}).
 *
 * <p>Clients should send: {@code X-Lang: ru} (or whatever language they support).
 */
public class LocaleFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Lang";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String raw = request.getHeader(HEADER);
        Language lang = Language.fromCode(raw);
        LocaleContextHolder.setLocale(lang.toLocale());

        try {
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
