package com.fiap.mechanical_hub.billing_service.application.gateways;

import java.util.UUID;

/**
 * Projecao do que o billing-service precisa saber sobre uma OS (GET
 * /service-orders/{id} do core-service): o numero (pra exibir na
 * preferencia do Mercado Pago) e o id do cliente dono (pra conferir
 * contra o JWT do cliente que esta aprovando/recusando - TASK-034).
 */
public record ServiceOrderInfo(String orderNumber, UUID customerId) {
}
