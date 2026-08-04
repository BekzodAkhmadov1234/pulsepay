package uz.pulsepay.compliance.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.compliance.adapter.out.jpa.entity.ComplianceFlagEntity;

import java.util.UUID;

interface ComplianceFlagJpaRepository extends JpaRepository<ComplianceFlagEntity, UUID> {
}
