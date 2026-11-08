package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.core;

import java.util.UUID;

/**
 * Espelha o ServiceOrderDetailResponse do core-service (GET
 * /service-orders/{id}) -- so os campos que o billing-service precisa:
 * o numero da OS e o id do cliente dono.
 */
record ServiceOrderDto(String orderNumber, CustomerDto customer) {

    record CustomerDto(UUID id) {
    }
}
