package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.compliance.ComplianceFlagEntity;

import java.util.UUID;

public interface ComplianceFlagRepository extends JpaRepository<ComplianceFlagEntity, UUID> {
}
