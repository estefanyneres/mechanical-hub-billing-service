package com.fiap.mechanical_hub.billing_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetUseCase;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        debugRaw("onMessage ENTROU, thread=" + Thread.currentThread().getName());
        try {
            log.warn("[DEBUG-SAGA] mensagem recebida na fila: {}", new String(message.getBody()));
            JsonNode body = objectMapper.readTree(message.getBody());
            UUID serviceOrderId = UUID.fromString(body.get("serviceOrderId").asText());
            log.warn("[DEBUG-SAGA] gerando orcamento para OS {}", serviceOrderId);
            budgetUseCase.generateBudget(serviceOrderId);
            log.warn("[DEBUG-SAGA] orcamento gerado com sucesso para OS {}", serviceOrderId);
        } catch (Throwable throwable) {
            debugRaw("falhou: " + throwable);
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(throwable);
        }
    }

    @SuppressWarnings("java:S106")
    private static void debugRaw(String text) {
        System.err.println("[DEBUG-SAGA-RAW] " + text);
        System.err.flush();
    }
}
