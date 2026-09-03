package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.domain.paynet.PaynetProvider;
import uz.pulsepay.domain.paynet.PaynetProviderEntity;
import uz.pulsepay.domain.shared.DomainException;
import uz.pulsepay.repository.PaynetProviderRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Thin, stateless service for Paynet provider lookups and prepayment field validation.
 * No state is written to the database here — that happens in {@link P2STransferService}.
 */
@Service
public class PaynetPaymentService {

    private final PaynetProviderRepository paynetProviderRepository;

    public PaynetPaymentService(PaynetProviderRepository paynetProviderRepository) {
        this.paynetProviderRepository = paynetProviderRepository;
    }

    /**
     * Returns all active Paynet providers.
     */
    public List<PaynetProvider> listProviders() {
        return paynetProviderRepository.findAllByIsActiveTrue()
                .stream()
                .map(PaynetProviderEntity::toDomain)
                .toList();
    }

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
}
