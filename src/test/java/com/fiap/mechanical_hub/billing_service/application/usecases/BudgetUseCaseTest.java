package com.fiap.mechanical_hub.billing_service.application.usecases;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceTask;
import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.infrastructure.messaging.SagaEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetUseCaseTest {

    @Mock
    private RepairServiceGateway repairServiceGateway;
    @Mock
    private BudgetGateway budgetGateway;
    @Mock
    private SagaEventPublisher publisher;

    private BudgetUseCase useCase;
    private UUID serviceOrderId;

    @BeforeEach
    void setUp() {
        useCase = new BudgetUseCase(repairServiceGateway, budgetGateway, publisher);
        serviceOrderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve somar mao de obra e materiais de uma unica tarefa")
    void generateBudget_ShouldSumLaborAndMaterials_ForSingleTask() {
        UUID serviceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        var task = new RepairServiceTask(serviceId, List.of(new RepairServiceTask.MaterialUsage(materialId, 3)));

        when(repairServiceGateway.findTasks(serviceOrderId)).thenReturn(List.of(task));
        when(repairServiceGateway.findServiceLaborCost(serviceId)).thenReturn(new BigDecimal("80.00"));
        when(repairServiceGateway.findMaterialUnitPrice(materialId)).thenReturn(new BigDecimal("10.00"));
        when(budgetGateway.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.generateBudget(serviceOrderId);

        // 80.00 (mao de obra) + 10.00 * 3 (material) = 110.00
        verify(budgetGateway).save(argThat(b -> b.getTotalValue().compareTo(new BigDecimal("110.00")) == 0));
        verify(publisher).publishOrcamentoGerado(serviceOrderId, new BigDecimal("110.00"));
    }

    @Test
    @DisplayName("Nao deve buscar o preco do mesmo servico/material duas vezes quando aparece em mais de uma tarefa")
    void generateBudget_ShouldCachePrices_WhenSameServiceOrMaterialAppearsInMultipleTasks() {
        UUID serviceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        var task1 = new RepairServiceTask(serviceId, List.of(new RepairServiceTask.MaterialUsage(materialId, 1)));
        var task2 = new RepairServiceTask(serviceId, List.of(new RepairServiceTask.MaterialUsage(materialId, 2)));

        when(repairServiceGateway.findTasks(serviceOrderId)).thenReturn(List.of(task1, task2));
        when(repairServiceGateway.findServiceLaborCost(serviceId)).thenReturn(new BigDecimal("50.00"));
        when(repairServiceGateway.findMaterialUnitPrice(materialId)).thenReturn(new BigDecimal("5.00"));
        when(budgetGateway.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.generateBudget(serviceOrderId);

        verify(repairServiceGateway, times(1)).findServiceLaborCost(serviceId);
        verify(repairServiceGateway, times(1)).findMaterialUnitPrice(materialId);
        // (50 + 50) mao de obra + (5*1 + 5*2) material = 100 + 15 = 115.00
        verify(publisher).publishOrcamentoGerado(serviceOrderId, new BigDecimal("115.00"));
    }

    @Test
    @DisplayName("Deve publicar orcamento zero quando a OS nao tem nenhuma tarefa")
    void generateBudget_ShouldPublishZero_WhenNoTasks() {
        when(repairServiceGateway.findTasks(serviceOrderId)).thenReturn(List.of());
        when(budgetGateway.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.generateBudget(serviceOrderId);

        verify(repairServiceGateway, never()).findServiceLaborCost(any());
        verify(repairServiceGateway, never()).findMaterialUnitPrice(any());
        verify(publisher).publishOrcamentoGerado(serviceOrderId, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve persistir o orcamento gerado antes de publicar o evento")
    void generateBudget_ShouldPersistBudget() {
        when(repairServiceGateway.findTasks(serviceOrderId)).thenReturn(List.of());
        when(budgetGateway.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.generateBudget(serviceOrderId);

        verify(budgetGateway).save(argThat(b -> b.getServiceOrderId().equals(serviceOrderId)));
        assertThat(serviceOrderId).isNotNull();
    }
}
