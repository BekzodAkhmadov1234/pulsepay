package uz.pulsepay.compliance.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.compliance.adapter.out.jpa.entity.RegulatoryParameterEntity;
import uz.pulsepay.compliance.domain.model.RegulatoryParameter;
import uz.pulsepay.compliance.domain.port.out.RegulatoryParameterRepository;

import java.time.Instant;
import java.util.Optional;

@Repository
class RegulatoryParameterJpaAdapter implements RegulatoryParameterRepository {

    private final RegulatoryParameterJpaRepository jpa;

    RegulatoryParameterJpaAdapter(RegulatoryParameterJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RegulatoryParameter> findCurrentByCode(String code) {
        return jpa.findCurrentByCode(code, Instant.now()).map(RegulatoryParameterEntity::toDomain);
    }
}
