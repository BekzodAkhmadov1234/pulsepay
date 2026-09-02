package uz.pulsepay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.domain.network.CardTransactionEntity;

import java.util.UUID;

public interface CardTransactionRepository extends JpaRepository<CardTransactionEntity, UUID> {
}
