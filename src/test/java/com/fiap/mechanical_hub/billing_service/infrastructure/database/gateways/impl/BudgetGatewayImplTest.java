package com.fiap.mechanical_hub.billing_service.infrastructure.database.gateways.impl;

import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.domain.enums.BudgetStatusEnum;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.mappers.BudgetJpaMapper;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.models.BudgetModel;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.repositories.BudgetJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetGatewayImplTest {

    @Mock
    private BudgetJpaRepository jpaRepository;

    private BudgetGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new BudgetGatewayImpl(jpaRepository);
    }

    @Test
    @DisplayName("Deve salvar e converter o modelo JPA de volta pra entidade de dominio")
    void save_ShouldPersistAndReturnDomainEntity() {
        Budget budget = Budget.create(UUID.randomUUID(), new BigDecimal("100.00"));
        BudgetModel model = BudgetJpaMapper.toJpa(budget);
        when(jpaRepository.save(org.mockito.ArgumentMatchers.any(BudgetModel.class))).thenReturn(model);

        Budget saved = gateway.save(budget);

        assertThat(saved.getId()).isEqualTo(budget.getId());
        assertThat(saved.getTotalValue()).isEqualTo(budget.getTotalValue());
    }

    @Test
    @DisplayName("Deve retornar o orcamento mais recente de uma OS")
    void findByServiceOrderId_ShouldReturnMostRecent() {
        UUID serviceOrderId = UUID.randomUUID();
        BudgetModel model = new BudgetModel(UUID.randomUUID(), serviceOrderId, new BigDecimal("50.00"),
                BudgetStatusEnum.PENDENTE, null, null, null, LocalDateTime.now());
        when(jpaRepository.findByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId)).thenReturn(List.of(model));

        Optional<Budget> result = gateway.findByServiceOrderId(serviceOrderId);

        assertThat(result).isPresent();
        assertThat(result.get().getServiceOrderId()).isEqualTo(serviceOrderId);
    }

    @Test
    @DisplayName("Deve retornar vazio quando a OS nao tem nenhum orcamento")
    void findByServiceOrderId_ShouldReturnEmpty_WhenNoneExists() {
        UUID serviceOrderId = UUID.randomUUID();
        when(jpaRepository.findByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId)).thenReturn(List.of());

        Optional<Budget> result = gateway.findByServiceOrderId(serviceOrderId);

        assertThat(result).isEmpty();
    }
}
