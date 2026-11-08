package com.fiap.mechanical_hub.billing_service.infrastructure.database.gateways.impl;

import com.fiap.mechanical_hub.billing_service.application.gateways.PaymentTransactionGateway;
import com.fiap.mechanical_hub.billing_service.domain.entities.PaymentTransaction;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.mappers.PaymentTransactionJpaMapper;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.repositories.PaymentTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentTransactionGatewayImpl implements PaymentTransactionGateway {

    private final PaymentTransactionJpaRepository jpaRepository;

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        var saved = jpaRepository.save(PaymentTransactionJpaMapper.toJpa(transaction));
        return PaymentTransactionJpaMapper.toDomain(saved);
    }
}
