package uz.pulsepay.reference.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.reference.domain.model.Bank;
import uz.pulsepay.reference.domain.port.out.BankRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BankJpaAdapter implements BankRepository {

    private final BankJpaRepository jpa;

    BankJpaAdapter(BankJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Bank> findById(UUID id) {
        return jpa.findById(id).map(e -> e.toDomain());
    }

    @Override
    public Optional<Bank> findByMfoCode(String mfoCode) {
        return jpa.findByMfoCode(mfoCode).map(e -> e.toDomain());
    }
}
