package uz.pulsepay.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uz.pulsepay.identity.domain.model.OtpProperties;

/**
 * Activates all @ConfigurationProperties bindings in the application.
 */
@Configuration
@EnableConfigurationProperties(OtpProperties.class)
public class AppPropertiesConfig {
}
