package com.fiap.mechanical_hub.billing_service.infrastructure.messaging;

import com.fiap.mechanical_hub.billing_service.application.dto.saga.OrcamentoAprovadoEvent;
import com.fiap.mechanical_hub.billing_service.application.dto.saga.OrcamentoGeradoEvent;
import com.fiap.mechanical_hub.billing_service.application.dto.saga.OrcamentoRecusadoEvent;
import com.fiap.mechanical_hub.billing_service.application.dto.saga.PagamentoConfirmadoEvent;
import com.fiap.mechanical_hub.billing_service.application.dto.saga.PagamentoFalhouEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public SagaEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrcamentoGerado(UUID serviceOrderId, BigDecimal valor) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.ROUTING_KEY_ORCAMENTO_GERADO,
                new OrcamentoGeradoEvent(serviceOrderId, valor));
    }

    public void publishOrcamentoAprovado(UUID serviceOrderId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.ROUTING_KEY_ORCAMENTO_APROVADO,
                new OrcamentoAprovadoEvent(serviceOrderId));
    }

    public void publishOrcamentoRecusado(UUID serviceOrderId, String motivo) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.ROUTING_KEY_ORCAMENTO_RECUSADO,
                new OrcamentoRecusadoEvent(serviceOrderId, motivo));
    }

    public void publishPagamentoConfirmado(UUID serviceOrderId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.ROUTING_KEY_PAGAMENTO_CONFIRMADO,
                new PagamentoConfirmadoEvent(serviceOrderId));
    }

    public void publishPagamentoFalhou(UUID serviceOrderId, String motivo) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.ROUTING_KEY_PAGAMENTO_FALHOU,
                new PagamentoFalhouEvent(serviceOrderId, motivo));
    }
}
