package com.fiap.mechanical_hub.billing_service.infrastructure.config;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.CoreServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.PaymentTransactionGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetApprovalUseCase;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetUseCase;
import com.fiap.mechanical_hub.billing_service.application.usecases.PaymentWebhookUseCase;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoClient;
import com.fiap.mechanical_hub.billing_service.infrastructure.messaging.SagaEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Casos de uso sao POJOs simples (sem @Component), mesmo padrao de
 * composicao manual ja usado nos outros 3 servicos -- precisa de um
 * @Bean explicito aqui porque o listener/controller (@Component) precisa
 * injetar uma instancia gerenciada pelo Spring.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public BudgetUseCase budgetUseCase(RepairServiceGateway repairServiceGateway, BudgetGateway budgetGateway,
                                        SagaEventPublisher publisher) {
        return new BudgetUseCase(repairServiceGateway, budgetGateway, publisher);
    }

    @Bean
    public BudgetApprovalUseCase budgetApprovalUseCase(BudgetGateway budgetGateway,
                                                         CoreServiceGateway coreServiceGateway,
                                                         MercadoPagoClient mercadoPagoClient,
                                                         SagaEventPublisher publisher) {
        return new BudgetApprovalUseCase(budgetGateway, coreServiceGateway, mercadoPagoClient, publisher);
    }

    @Bean
    public PaymentWebhookUseCase paymentWebhookUseCase(MercadoPagoClient mercadoPagoClient,
                                                         BudgetGateway budgetGateway,
                                                         PaymentTransactionGateway paymentTransactionGateway,
                                                         SagaEventPublisher publisher) {
        return new PaymentWebhookUseCase(mercadoPagoClient, budgetGateway, paymentTransactionGateway, publisher);
    }
}
