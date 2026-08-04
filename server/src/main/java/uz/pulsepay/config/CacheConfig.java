package uz.pulsepay.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String REGULATORY_PARAMETERS_CACHE = "regulatory_parameters";
}
