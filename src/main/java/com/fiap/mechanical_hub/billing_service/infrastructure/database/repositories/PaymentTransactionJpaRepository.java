package com.fiap.mechanical_hub.billing_service.infrastructure.database.repositories;

import com.fiap.mechanical_hub.billing_service.infrastructure.database.models.PaymentTransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionModel, UUID> {
}
