package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.paynet.PaynetProviderEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaynetProviderRepository extends JpaRepository<PaynetProviderEntity, UUID> {

    Optional<PaynetProviderEntity> findByServiceCode(String serviceCode);

    /** All active providers, ordered by sort_order ascending (natural display order). */
    List<PaynetProviderEntity> findAllByIsActiveTrueOrderBySortOrderAsc();

    /** Active providers for a specific category, ordered by sort_order. */
    List<PaynetProviderEntity> findAllByIsActiveTrueAndCategoryOrderBySortOrderAsc(String category);

    /** Case-insensitive name search across active providers. */
    List<PaynetProviderEntity> findByServiceNameContainingIgnoreCaseAndIsActiveTrue(String query);
}
