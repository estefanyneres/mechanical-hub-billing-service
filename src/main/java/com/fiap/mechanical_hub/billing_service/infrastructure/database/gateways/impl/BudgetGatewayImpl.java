package com.fiap.mechanical_hub.billing_service.infrastructure.database.gateways.impl;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.mappers.BudgetJpaMapper;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.repositories.BudgetJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BudgetGatewayImpl implements BudgetGateway {

    private final BudgetJpaRepository jpaRepository;

    @Override
    public Budget save(Budget budget) {
        var saved = jpaRepository.save(BudgetJpaMapper.toJpa(budget));
        return BudgetJpaMapper.toDomain(saved);
    }

    @Override
    public Optional<Budget> findByServiceOrderId(UUID serviceOrderId) {
        return jpaRepository.findByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId).stream()
                .findFirst()
                .map(BudgetJpaMapper::toDomain);
    }
}
