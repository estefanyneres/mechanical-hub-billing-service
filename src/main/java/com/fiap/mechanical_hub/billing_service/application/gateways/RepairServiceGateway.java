package com.fiap.mechanical_hub.billing_service.application.gateways;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Chamada sincrona ao repair-service (TASK-033) pra saber o que foi
 * selecionado numa OS e quanto cada item custa hoje no catalogo.
 */
public interface RepairServiceGateway {

    List<RepairServiceTask> findTasks(UUID serviceOrderId);

    BigDecimal findServiceLaborCost(UUID serviceId);

    BigDecimal findMaterialUnitPrice(UUID materialId);
}
