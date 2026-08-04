package uz.pulsepay.compliance.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.compliance.adapter.out.jpa.entity.ComplianceFlagEntity;
import uz.pulsepay.compliance.domain.model.ComplianceFlag;
import uz.pulsepay.compliance.domain.port.out.ComplianceFlagRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
class ComplianceFlagJpaAdapter implements ComplianceFlagRepository {

    private final ComplianceFlagJpaRepository jpa;

    ComplianceFlagJpaAdapter(ComplianceFlagJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ComplianceFlag save(ComplianceFlag flag) {
        return jpa.save(ComplianceFlagEntity.fromDomain(flag)).toDomain();
    }

    @Override
    public Optional<ComplianceFlag> findById(UUID id) {
        return jpa.findById(id).map(ComplianceFlagEntity::toDomain);
    }
}
