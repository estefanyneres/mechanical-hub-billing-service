package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.repair;

import java.util.List;
import java.util.UUID;

/**
 * Espelha o OrderTaskResponse do repair-service (GET
 * /service-orders/{id}/tasks) -- so os campos que o billing-service
 * precisa pra calcular o orcamento.
 */
record OrderTaskResponseDto(UUID serviceId, List<MaterialDto> materials) {

    record MaterialDto(UUID materialId, int quantity) {
    }
}
