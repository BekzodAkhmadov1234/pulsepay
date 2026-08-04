package uz.pulsepay.party.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.party.adapter.out.jpa.entity.InstrumentEntity;

import java.util.Optional;
import java.util.UUID;

interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, UUID> {
    Optional<InstrumentEntity> findByIdAndOwnerPartyId(UUID id, UUID ownerPartyId);
}
