package com.fiap.mechanical_hub.billing_service.application.gateways;

import java.util.List;
import java.util.UUID;

/**
 * Projecao do que o billing-service precisa saber sobre uma tarefa de
 * execucao (GET /service-orders/{id}/tasks do repair-service) -- sem
 * preco, de proposito (o repair-service nao embute preco na tarefa, ver
 * OrderTaskMaterialSnapshot). O preco e buscado separadamente, por ID,
 * direto do catalogo (findServiceLaborCost/findMaterialUnitPrice), pra
 * refletir o valor atual, nao um valor congelado na hora da selecao.
 */
public record RepairServiceTask(UUID serviceId, List<MaterialUsage> materials) {

    public record MaterialUsage(UUID materialId, int quantity) {
    }
}
