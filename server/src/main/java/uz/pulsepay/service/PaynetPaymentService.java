package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.domain.paynet.PaynetProvider;
import uz.pulsepay.domain.paynet.PaynetProviderEntity;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.dto.response.PaynetCategoryResponse;
import uz.pulsepay.repository.PaynetProviderRepository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thin, stateless service for Paynet provider lookups and prepayment field validation.
 * No state is written to the database here — that happens in {@link P2STransferService}.
 */
@Service
public class PaynetPaymentService {

    // Display names for each provider category (Uzbek-first)
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "gas",         "Gaz",
            "water",       "Suv",
            "electricity", "Elektr",
            "mobile",      "Mobil to'ldirish",
            "internet",    "Internet"
    );

    // Default mobile provider used by the /mobile shortcut endpoint
    private static final String DEFAULT_MOBILE_SERVICE_CODE = "mobile-uzb";

    private final PaynetProviderRepository paynetProviderRepository;

    public PaynetPaymentService(PaynetProviderRepository paynetProviderRepository) {
        this.paynetProviderRepository = paynetProviderRepository;
    }

    // ── Provider listing ──────────────────────────────────────────────────────

    /**
     * All active providers, sorted by sort_order (natural display order).
     */
    public List<PaynetProvider> listProviders() {
        return paynetProviderRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(PaynetProviderEntity::toDomain)
                .toList();
    }

    /**
     * Active providers filtered by category, sorted by sort_order.
     */
    public List<PaynetProvider> listByCategory(String category) {
        return paynetProviderRepository
                .findAllByIsActiveTrueAndCategoryOrderBySortOrderAsc(category)
                .stream()
                .map(PaynetProviderEntity::toDomain)
                .toList();
    }

    /**
     * Case-insensitive name search across active providers.
     * Returns the full sorted list when the query is blank.
     */
    public List<PaynetProvider> searchProviders(String query) {
        if (query == null || query.isBlank()) return listProviders();
        return paynetProviderRepository
                .findByServiceNameContainingIgnoreCaseAndIsActiveTrue(query.strip())
                .stream()
                .map(PaynetProviderEntity::toDomain)
                .sorted(Comparator.comparingInt(PaynetProvider::sortOrder))
                .toList();
    }

    /**
     * Returns the top {@code count} providers by sort_order (1–20 inclusive).
     */
    public List<PaynetProvider> listPopular(int count) {
        int limit = Math.max(1, Math.min(count, 20));
        return paynetProviderRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(PaynetProviderEntity::toDomain)
                .limit(limit)
                .toList();
    }

    /**
     * Distinct categories derived from active providers, with localised display names
     * and provider counts. Sorted alphabetically by category key.
     */
    public List<PaynetCategoryResponse> listCategories() {
        return paynetProviderRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(PaynetProviderEntity::toDomain)
                .collect(Collectors.groupingBy(PaynetProvider::category, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PaynetCategoryResponse(
                        e.getKey(),
                        CATEGORY_LABELS.getOrDefault(e.getKey(), e.getKey()),
                        (int) (long) e.getValue()))
                .sorted(Comparator.comparing(PaynetCategoryResponse::category))
                .toList();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates that all required fields for the given service are present in the request.
     *
     * @param serviceCode Provider service code (e.g. "gas-uzb")
     * @param fields      Caller-supplied field map (e.g. {account_number: "12345"})
     * @return            The matched provider (for response construction)
     * @throws DomainException if service not found or required fields are missing
     */
    public PaynetProvider validatePrepayment(String serviceCode, Map<String, String> fields) {
        PaynetProvider provider = paynetProviderRepository.findByServiceCode(serviceCode)
                .map(PaynetProviderEntity::toDomain)
                .filter(PaynetProvider::isActive)
                .orElseThrow(() -> new DomainException("Unknown or inactive service code: " + serviceCode));

        List<String> missing = Arrays.stream(provider.fieldNames())
                .filter(required -> !fields.containsKey(required) || fields.get(required).isBlank())
                .toList();

        if (!missing.isEmpty()) {
            throw new DomainException("Missing required fields for service '" + serviceCode + "': " + missing);
        }

        return provider;
    }

    /**
     * Mobile top-up shortcut: validates the phone number against the default mobile provider
     * (UzMobile) and returns its details. The caller then initiates a P2S transfer normally.
     *
     * @param phone      Subscriber phone number (e.g. +998901234567)
     * @param serviceCode Service code override; defaults to {@value DEFAULT_MOBILE_SERVICE_CODE}
     */
    public PaynetProvider validateMobileTopUp(String phone, String serviceCode) {
        String code = (serviceCode != null && !serviceCode.isBlank())
                ? serviceCode : DEFAULT_MOBILE_SERVICE_CODE;
        return validatePrepayment(code, Map.of("phone", phone));
    }
}
