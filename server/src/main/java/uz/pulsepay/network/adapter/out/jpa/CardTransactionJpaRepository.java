package uz.pulsepay.network.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pulsepay.network.adapter.out.jpa.entity.CardTransactionEntity;

import java.util.UUID;

interface CardTransactionJpaRepository extends JpaRepository<CardTransactionEntity, UUID> {
}
