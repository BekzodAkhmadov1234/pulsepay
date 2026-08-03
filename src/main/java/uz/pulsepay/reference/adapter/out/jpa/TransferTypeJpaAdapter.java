package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.reference.domain.model.TransferType;
import uz.pulsepay.reference.domain.port.out.TransferTypeRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class TransferTypeJpaAdapter implements TransferTypeRepository {

    private final TransferTypeJpaRepository jpa;

    TransferTypeJpaAdapter(TransferTypeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<TransferType> findById(int id) {
        return jpa.findById(id).map(e -> e.toDomain());
    }

    @Override
    public Optional<TransferType> findByCode(String code) {
        return jpa.findByCode(code).map(e -> e.toDomain());
    }

    @Override
    public List<TransferType> findAllActive() {
        return jpa.findByIsActiveTrue().stream().map(e -> e.toDomain()).toList();
    }
}
