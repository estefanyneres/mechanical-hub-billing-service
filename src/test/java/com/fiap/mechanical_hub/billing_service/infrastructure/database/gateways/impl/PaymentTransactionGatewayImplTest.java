package com.fiap.mechanical_hub.billing_service.infrastructure.database.gateways.impl;

import com.fiap.mechanical_hub.billing_service.domain.entities.PaymentTransaction;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.mappers.PaymentTransactionJpaMapper;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.models.PaymentTransactionModel;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.repositories.PaymentTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionGatewayImplTest {

    @Mock
    private PaymentTransactionJpaRepository jpaRepository;

    private PaymentTransactionGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new PaymentTransactionGatewayImpl(jpaRepository);
    }

    @Test
    @DisplayName("Deve salvar e converter o modelo JPA de volta pra entidade de dominio")
    void save_ShouldPersistAndReturnDomainEntity() {
        PaymentTransaction transaction = PaymentTransaction.register(UUID.randomUUID(), "999", "approved");
        PaymentTransactionModel model = PaymentTransactionJpaMapper.toJpa(transaction);
        when(jpaRepository.save(any(PaymentTransactionModel.class))).thenReturn(model);

        PaymentTransaction saved = gateway.save(transaction);

        assertThat(saved.getId()).isEqualTo(transaction.getId());
        assertThat(saved.getMercadoPagoPaymentId()).isEqualTo("999");
        assertThat(saved.getStatus()).isEqualTo("approved");
    }
}
