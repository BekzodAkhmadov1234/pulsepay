package uz.pulsepay.compliance.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pulsepay.compliance.domain.model.RegulatoryParameter;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 0 MANDATORY test: BCV resolver converts N BCV → UZS tiyin correctly.
 *
 * Rules under test:
 *  - unit='uzs' parameters are returned as-is
 *  - unit='bcv' parameters are multiplied by the current BCV tiyin value
 *  - BCV parameter itself must always have unit='uzs'
 *  - Unknown unit raises DomainException
 */
class BcvResolverTest {

    // BCV = 412,000 UZS = 41,200,000 tiyin
    private static final long BCV_TIYIN = 41_200_000L;

    private RegulatoryParameterResolver resolver;
    private BcvResolver bcvResolver;

    @BeforeEach
    void setUp() {
        resolver = mock(RegulatoryParameterResolver.class);
        bcvResolver = new BcvResolver(resolver);

        when(resolver.resolve(BcvResolver.BCV_CODE))
                .thenReturn(param("bcv", BCV_TIYIN, "uzs"));
    }

    @Test
    void currentBcvTiyin_returns_seeded_bcv_in_tiyin() {
        assertThat(bcvResolver.currentBcvTiyin()).isEqualTo(BCV_TIYIN);
    }

    @Test
    void currentBcvUzs_returns_412000() {
        assertThat(bcvResolver.currentBcvUzs()).isEqualTo(412_000L);
    }

    @Test
    void resolveToUzsTiyin_unit_uzs_returns_value_directly() {
        // 175,000,000 UZS = 17,500,000,000 tiyin (AML one-off CDD threshold)
        long tiyinValue = 17_500_000_000L;
        when(resolver.resolve("aml_cdd_one_off_threshold"))
                .thenReturn(param("aml_cdd_one_off_threshold", tiyinValue, "uzs"));

        assertThat(bcvResolver.resolveToUzsTiyin("aml_cdd_one_off_threshold"))
                .isEqualTo(tiyinValue);
    }

    @Test
    void resolveToUzsTiyin_unit_bcv_multiplies_by_current_bcv() {
        // 500 BCV × 41,200,000 tiyin = 20,600,000,000 tiyin
        when(resolver.resolve("aml_large_operation_transfer_threshold"))
                .thenReturn(param("aml_large_operation_transfer_threshold", 500L, "bcv"));

        long result = bcvResolver.resolveToUzsTiyin("aml_large_operation_transfer_threshold");

        assertThat(result).isEqualTo(500L * BCV_TIYIN);
    }

    @Test
    void resolveToUzsTiyin_1000_bcv_inflow_threshold() {
        // 1000 BCV × 41,200,000 tiyin = 41,200,000,000 tiyin
        when(resolver.resolve("aml_large_operation_inflow_threshold"))
                .thenReturn(param("aml_large_operation_inflow_threshold", 1000L, "bcv"));

        assertThat(bcvResolver.resolveToUzsTiyin("aml_large_operation_inflow_threshold"))
                .isEqualTo(1000L * BCV_TIYIN);
    }

    @Test
    void resolveToUzsTiyin_25_bcv_enhanced_info_threshold() {
        // 25 BCV × 41,200,000 tiyin = 1,030,000,000 tiyin
        when(resolver.resolve("aml_enhanced_info_threshold"))
                .thenReturn(param("aml_enhanced_info_threshold", 25L, "bcv"));

        assertThat(bcvResolver.resolveToUzsTiyin("aml_enhanced_info_threshold"))
                .isEqualTo(25L * BCV_TIYIN);
    }

    @Test
    void resolveToUzsTiyin_unknown_unit_throws_domain_exception() {
        when(resolver.resolve("bad_param"))
                .thenReturn(param("bad_param", 100L, "eur"));

        assertThatThrownBy(() -> bcvResolver.resolveToUzsTiyin("bad_param"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Unknown unit")
                .hasMessageContaining("eur");
    }

    @Test
    void bcv_param_with_non_uzs_unit_throws_domain_exception() {
        when(resolver.resolve(BcvResolver.BCV_CODE))
                .thenReturn(param("bcv", 412L, "bcv"));  // wrong: BCV in BCV units makes no sense

        assertThatThrownBy(() -> bcvResolver.currentBcvTiyin())
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("unit='uzs'");
    }

    @Test
    void missing_regulatory_parameter_propagates_not_found_exception() {
        when(resolver.resolve("missing_code"))
                .thenThrow(new NotFoundException("No active regulatory parameter for code: missing_code"));

        assertThatThrownBy(() -> bcvResolver.resolveToUzsTiyin("missing_code"))
                .isInstanceOf(NotFoundException.class);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static RegulatoryParameter param(String code, long valueAmount, String unit) {
        return new RegulatoryParameter(
                UUID.randomUUID(), code, valueAmount, unit, "UZS",
                Instant.parse("2025-08-01T00:00:00Z"), null, "test", Instant.now());
    }
}
