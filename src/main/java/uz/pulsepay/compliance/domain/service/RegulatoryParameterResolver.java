package uz.pulsepay.compliance.domain.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uz.pulsepay.compliance.domain.model.RegulatoryParameter;
import uz.pulsepay.compliance.domain.port.out.RegulatoryParameterRepository;
import uz.pulsepay.config.CacheConfig;
import uz.pulsepay.shared.exception.NotFoundException;

/**
 * Resolves the currently-in-force regulatory parameter for a given code.
 * Risk #5: 1-minute TTL cache — never populated at startup.
 */
@Service
public class RegulatoryParameterResolver {

    private final RegulatoryParameterRepository repository;

    public RegulatoryParameterResolver(RegulatoryParameterRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = CacheConfig.REGULATORY_PARAMETERS_CACHE, key = "#code")
    public RegulatoryParameter resolve(String code) {
        return repository.findCurrentByCode(code)
                .orElseThrow(() -> new NotFoundException("No active regulatory parameter for code: " + code));
    }

    @CacheEvict(value = CacheConfig.REGULATORY_PARAMETERS_CACHE, key = "#code")
    public void evict(String code) {
        // Cache eviction — callable by admin endpoint
    }

    @CacheEvict(value = CacheConfig.REGULATORY_PARAMETERS_CACHE, allEntries = true)
    public void evictAll() {
        // Full cache eviction
    }
}
