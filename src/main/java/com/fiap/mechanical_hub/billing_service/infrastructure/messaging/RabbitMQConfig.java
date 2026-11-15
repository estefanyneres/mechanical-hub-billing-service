package com.fiap.mechanical_hub.billing_service.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mesma exchange do Saga que o core-service declara (nome/tipo/durabilidade
 * tem que ser identicos entre os servicos -- o RabbitMQ rejeita uma segunda
 * declaracao da "mesma" exchange com propriedades diferentes). Fila propria
 * deste servico, igual ao padrao ja usado nos outros 3.
 *
 * Contrato (ver RabbitMQConfig do core-service pro contrato completo):
 *   billing-service CONSOME (fila billing-service.eventos.queue):
 *     - "orcamento.solicitado" {serviceOrderId, orderNumber, requestDescription} (core-service, TASK-033)
 *   billing-service PUBLICA:
 *     - "orcamento.gerado" {serviceOrderId, valor} (TASK-033, consumido pelo core-service)
 *     - "orcamento.aprovado" {serviceOrderId} (TASK-034, cliente aprovou - consumido pelo core-service)
 *     - "orcamento.recusado" {serviceOrderId, motivo} (TASK-034, cliente recusou - consumido pelo core-service)
 *     - "pagamento.confirmado" {serviceOrderId} (TASK-035, pagamento aprovado no Mercado Pago - consumido
 *                                                 pelo core-service E pelo repair-service, que tenta reservar
 *                                                 material - fan-out via exchange, cada servico com a propria fila)
 *     - "pagamento.falhou" {serviceOrderId, motivo} (TASK-035, pagamento rejeitado/cancelado - consumido
 *                                                     pelo core-service)
 */
@Configuration
public class RabbitMQConfig {

    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String BILLING_SERVICE_QUEUE = "billing-service.eventos.queue";

    public static final String ROUTING_KEY_ORCAMENTO_SOLICITADO = "orcamento.solicitado";
    public static final String ROUTING_KEY_ORCAMENTO_GERADO = "orcamento.gerado";
    public static final String ROUTING_KEY_ORCAMENTO_APROVADO = "orcamento.aprovado";
    public static final String ROUTING_KEY_ORCAMENTO_RECUSADO = "orcamento.recusado";
    public static final String ROUTING_KEY_PAGAMENTO_CONFIRMADO = "pagamento.confirmado";
    public static final String ROUTING_KEY_PAGAMENTO_FALHOU = "pagamento.falhou";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    @Bean
    public Queue billingServiceQueue() {
        return new Queue(BILLING_SERVICE_QUEUE, true);
    }

    @Bean
    public Binding orcamentoSolicitadoBinding(Queue billingServiceQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(billingServiceQueue).to(sagaExchange).with(ROUTING_KEY_ORCAMENTO_SOLICITADO);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    // Sem Jackson2JsonMessageConverter aqui: ele tenta resolver o header
    // __TypeId__ (classe de quem publicou) via Class.forName(), que nunca
    // existe no classpath de quem consome -- descartava a mensagem calada.
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }
}
