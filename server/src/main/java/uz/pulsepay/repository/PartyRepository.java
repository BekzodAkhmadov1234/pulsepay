package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.party.PartyEntity;
import uz.pulsepay.domain.party.PartyType;

import java.util.Optional;
import java.util.UUID;

public interface PartyRepository extends JpaRepository<PartyEntity, UUID> {

    Optional<PartyEntity> findByIdAndPartyType(UUID id, PartyType partyType);
}
