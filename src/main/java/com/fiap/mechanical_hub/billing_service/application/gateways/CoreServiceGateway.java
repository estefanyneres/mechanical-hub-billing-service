package com.fiap.mechanical_hub.billing_service.application.gateways;

import java.util.UUID;

/**
 * Chamada sincrona ao core-service (TASK-034) pra conferir o dono de uma
 * OS antes de aceitar aprovacao/recusa de orcamento pelo cliente.
 */
public interface CoreServiceGateway {

    /**
     * @return null se a OS nao existir (o core-service respondeu 404) --
     * o chamador decide o que fazer, sem acoplar este gateway a nenhuma
     * excecao especifica do use case.
     */
    ServiceOrderInfo findServiceOrder(UUID serviceOrderId);
}
