package uz.pulsepay.service;

import org.springframework.stereotype.Service;
import uz.pulsepay.domain.reference.BankEntity;
import uz.pulsepay.domain.reference.Bank;
import uz.pulsepay.repository.BankRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BankService {

    private final BankRepository bankRepository;

    public BankService(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    public List<Bank> listActiveBanks() {
        return bankRepository.findAll().stream()
                .map(BankEntity::toDomain)
                .filter(Bank::isActive)
                .collect(Collectors.toList());
    }

    public List<Bank> listAllBanks() {
        return bankRepository.findAll().stream()
                .map(BankEntity::toDomain)
                .collect(Collectors.toList());
    }

    public Optional<Bank> findByMfoCode(String mfoCode) {
        return bankRepository.findByMfoCode(mfoCode)
                .map(BankEntity::toDomain);
    }

    public Optional<Bank> findById(UUID id) {
        return bankRepository.findById(id)
                .map(BankEntity::toDomain);
    }
}
