package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.reference.domain.model.PurposeCode;
import uz.pulsepay.reference.domain.port.out.PurposeCodeRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class PurposeCodeJpaAdapter implements PurposeCodeRepository {

    private final PurposeCodeJpaRepository jpa;

    PurposeCodeJpaAdapter(PurposeCodeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PurposeCode> findById(int id) {
        return jpa.findById(id).map(e -> e.toDomain());
    }

    @Override
    public List<PurposeCode> findByTransferTypeId(int transferTypeId) {
        return jpa.findByApplicableTransferTypeId(transferTypeId).stream().map(e -> e.toDomain()).toList();
    }
}
