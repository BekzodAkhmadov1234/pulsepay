package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.pulsepay.domain.merchant.MerchantEntity;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<MerchantEntity, UUID> {

    Optional<MerchantEntity> findByEmail(String email);

    @Modifying
    @Query(nativeQuery = true,
           value = "INSERT INTO parties (id, party_type, created_at) VALUES (:id, 'merchant', NOW()) ON CONFLICT DO NOTHING")
    void upsertMerchantParty(@Param("id") UUID id);

    @Modifying
    @Query(nativeQuery = true,
           value = "INSERT INTO instruments (id, owner_party_id, instrument_type, status, created_at) " +
                   "VALUES (:instrId, :ownerId, 'merchant_account', 'active', NOW()) ON CONFLICT DO NOTHING")
    void upsertMerchantAccountInstrument(@Param("instrId") UUID instrId, @Param("ownerId") UUID ownerId);
}
