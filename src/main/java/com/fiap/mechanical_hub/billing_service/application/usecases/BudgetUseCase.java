package com.fiap.mechanical_hub.billing_service.application.usecases;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceTask;
import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.infrastructure.messaging.SagaEventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TASK-033: ao receber "orcamento.solicitado", consulta o repair-service
 * pra saber o que foi selecionado na OS (tarefas + materiais) e calcula o
 * total somando a mao-de-obra de cada servico com o custo dos materiais
 * usados -- preco sempre buscado fresco no catalogo (nunca cacheado na
 * tarefa), um lookup por servico/material unico, nao por ocorrencia.
 */
@Slf4j
public class BudgetUseCase {

    private final RepairServiceGateway repairServiceGateway;
    private final BudgetGateway budgetGateway;
    private final SagaEventPublisher publisher;

    public BudgetUseCase(RepairServiceGateway repairServiceGateway, BudgetGateway budgetGateway,
                          SagaEventPublisher publisher) {
        this.repairServiceGateway = repairServiceGateway;
        this.budgetGateway = budgetGateway;
        this.publisher = publisher;
    }

    public void generateBudget(UUID serviceOrderId) {
        List<RepairServiceTask> tasks = repairServiceGateway.findTasks(serviceOrderId);

        BigDecimal total = calculateTotal(tasks);

        Budget budget = Budget.create(serviceOrderId, total);
        budgetGateway.save(budget);

        publisher.publishOrcamentoGerado(serviceOrderId, total);
        log.info("Orçamento de {} gerado pra OS {} ({} tarefa(s))", total, serviceOrderId, tasks.size());
    }

    private BigDecimal calculateTotal(List<RepairServiceTask> tasks) {
        // OS sem nenhuma tarefa selecionada (diagnostico nao produziu
        // nada): orcamento zero, mas ainda publica o evento -- o fluxo do
        // Saga segue normalmente, nao trava esperando uma tarefa que
        // nunca vai existir.
        if (tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<UUID, BigDecimal> serviceLaborCostCache = new HashMap<>();
        Map<UUID, BigDecimal> materialUnitPriceCache = new HashMap<>();

        BigDecimal total = BigDecimal.ZERO;

        for (RepairServiceTask task : tasks) {
            BigDecimal laborCost = serviceLaborCostCache.computeIfAbsent(
                    task.serviceId(), repairServiceGateway::findServiceLaborCost);
            total = total.add(laborCost);

            for (RepairServiceTask.MaterialUsage material : task.materials()) {
                BigDecimal unitPrice = materialUnitPriceCache.computeIfAbsent(
                        material.materialId(), repairServiceGateway::findMaterialUnitPrice);
                total = total.add(unitPrice.multiply(BigDecimal.valueOf(material.quantity())));
            }
        }

        return total;
    }
}
