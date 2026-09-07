package uz.pulsepay.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.pulsepay.utils.LocaleFilter;

import java.nio.charset.StandardCharsets;

/**
 * Registers the {@link MessageSource} (reads {@code messages*.properties})
 * and the {@link LocaleFilter} (reads {@code X-Lang} per request).
 *
 * <p>Supported bundles:
 * <ul>
 *   <li>messages.properties          — English (default fallback)</li>
 *   <li>messages_uz.properties       — Uzbek Latin</li>
 *   <li>messages_uz_c.properties     — Uzbek Cyrillic</li>
 *   <li>messages_ru.properties       — Russian</li>
 *   <li>messages_zh.properties       — Chinese (Simplified)</li>
 * </ul>
 */
@Configuration
public class LocaleConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding(StandardCharsets.UTF_8.name());
        ms.setUseCodeAsDefaultMessage(true); // return code if key missing
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    @Bean
    public OncePerRequestFilter localeFilter() {
        return new LocaleFilter();
    }
}
