package com.fiap.mechanical_hub.billing_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetUseCase;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Le o JSON bruto em vez de depender do tipo Java do payload (mesmo
 * motivo do SagaEventListener do core-service: nao acoplar ao formato
 * interno de outro servico). So existe um routing key consumido aqui por
 * enquanto (orcamento.solicitado), mas mantem o mesmo formato dos outros
 * servicos pra quando a TASK-034/036 adicionar mais.
 */
@Component
public class SagaEventListener {

    private final BudgetUseCase budgetUseCase;
    private final ObjectMapper objectMapper;

    public SagaEventListener(BudgetUseCase budgetUseCase, ObjectMapper objectMapper) {
        this.budgetUseCase = budgetUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.BILLING_SERVICE_QUEUE)
    public void onMessage(Message message) throws IOException {
        JsonNode body = objectMapper.readTree(message.getBody());
        UUID serviceOrderId = UUID.fromString(body.get("serviceOrderId").asText());
        budgetUseCase.generateBudget(serviceOrderId);
    }
}
