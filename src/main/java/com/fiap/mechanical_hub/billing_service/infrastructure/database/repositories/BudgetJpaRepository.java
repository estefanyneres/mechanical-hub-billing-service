package com.fiap.mechanical_hub.billing_service.infrastructure.database.repositories;

import com.fiap.mechanical_hub.billing_service.infrastructure.database.models.BudgetModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetJpaRepository extends JpaRepository<BudgetModel, UUID> {

    List<BudgetModel> findByServiceOrderIdOrderByCreatedAtDesc(UUID serviceOrderId);
}
