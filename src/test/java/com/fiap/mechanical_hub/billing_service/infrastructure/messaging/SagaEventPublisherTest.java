package com.fiap.mechanical_hub.billing_service.infrastructure.messaging;

import com.fiap.mechanical_hub.billing_service.application.dto.saga.OrcamentoAprovadoEvent;
import com.fiap.mechanical_hub.billing_service.application.dto.saga.OrcamentoGeradoEvent;
import com.fiap.mechanical_hub.billing_service.application.dto.saga.OrcamentoRecusadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SagaEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SagaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SagaEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve publicar orcamento.gerado na exchange do Saga com o serviceOrderId e o valor")
    void publishOrcamentoGerado_ShouldSendToSagaExchange() {
        UUID serviceOrderId = UUID.randomUUID();
        BigDecimal valor = new BigDecimal("150.00");

        publisher.publishOrcamentoGerado(serviceOrderId, valor);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORCAMENTO_GERADO,
                new OrcamentoGeradoEvent(serviceOrderId, valor));
    }

    @Test
    @DisplayName("Deve publicar orcamento.aprovado na exchange do Saga")
    void publishOrcamentoAprovado_ShouldSendToSagaExchange() {
        UUID serviceOrderId = UUID.randomUUID();

        publisher.publishOrcamentoAprovado(serviceOrderId);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORCAMENTO_APROVADO,
                new OrcamentoAprovadoEvent(serviceOrderId));
    }

    @Test
    @DisplayName("Deve publicar orcamento.recusado na exchange do Saga com o motivo")
    void publishOrcamentoRecusado_ShouldSendToSagaExchange() {
        UUID serviceOrderId = UUID.randomUUID();

        publisher.publishOrcamentoRecusado(serviceOrderId, "Preço muito alto");

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORCAMENTO_RECUSADO,
                new OrcamentoRecusadoEvent(serviceOrderId, "Preço muito alto"));
    }
}
