package uz.pulsepay.party.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.party.adapter.out.jpa.entity.PartyEntity;
import uz.pulsepay.party.domain.model.PartyType;

import java.util.Optional;
import java.util.UUID;

interface PartyJpaRepository extends JpaRepository<PartyEntity, UUID> {
    Optional<PartyEntity> findByIdAndPartyType(UUID id, PartyType partyType);
}
