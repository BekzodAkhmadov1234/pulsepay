package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.party.InstrumentEntity;

import java.util.Optional;
import java.util.UUID;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, UUID> {

    Optional<InstrumentEntity> findByIdAndOwnerPartyId(UUID id, UUID ownerPartyId);
}
