package com.fiap.mechanical_hub.billing_service.application.gateways;

import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;

import java.util.Optional;
import java.util.UUID;

public interface BudgetGateway {

    Budget save(Budget budget);

    Optional<Budget> findByServiceOrderId(UUID serviceOrderId);
}
