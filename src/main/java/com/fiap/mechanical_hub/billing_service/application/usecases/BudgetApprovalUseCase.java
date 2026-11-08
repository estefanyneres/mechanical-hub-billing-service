package com.fiap.mechanical_hub.billing_service.application.usecases;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.CoreServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.ServiceOrderInfo;
import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.domain.exceptions.ForbiddenException;
import com.fiap.mechanical_hub.billing_service.domain.exceptions.NotFoundException;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoClient;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoPreference;
import com.fiap.mechanical_hub.billing_service.infrastructure.messaging.SagaEventPublisher;

import java.util.UUID;

/**
 * Aprovacao/recusa do orcamento pelo cliente (TASK-034) -- endpoint que
 * nenhuma task cobria explicitamente, combinado com o usuario durante a
 * implementacao. Este servico nao tem a entidade Customer nem sabe de
 * quem e a OS, por isso toda chamada confere o dono consultando o
 * core-service (mesmo ServiceTokenIssuer de servico-pra-servico da
 * TASK-033) e comparando com o id do cliente extraido do JWT. Mensagem de
 * erro generica nos dois casos (OS inexistente ou de outro cliente) pra
 * nao revelar pra quem nao e o dono se a OS existe.
 */
public class BudgetApprovalUseCase {

    private final BudgetGateway budgetGateway;
    private final CoreServiceGateway coreServiceGateway;
    private final MercadoPagoClient mercadoPagoClient;
    private final SagaEventPublisher publisher;

    public BudgetApprovalUseCase(BudgetGateway budgetGateway, CoreServiceGateway coreServiceGateway,
                                  MercadoPagoClient mercadoPagoClient, SagaEventPublisher publisher) {
        this.budgetGateway = budgetGateway;
        this.coreServiceGateway = coreServiceGateway;
        this.mercadoPagoClient = mercadoPagoClient;
        this.publisher = publisher;
    }

    public String approve(UUID serviceOrderId, UUID requestingCustomerId) {
        ServiceOrderInfo order = verifyOwnership(serviceOrderId, requestingCustomerId);
        Budget budget = findBudget(serviceOrderId);
        // Confere ANTES de chamar o Mercado Pago: criar uma preferencia de
        // pagamento tem efeito colateral do lado de fora, nao da pra
        // descobrir que o orcamento ja tinha sido decidido so depois.
        budget.validatePending();

        MercadoPagoPreference preference = mercadoPagoClient.createPreference(
                order.orderNumber(), serviceOrderId, budget.getTotalValue());

        budget.approve(preference.preferenceId(), preference.paymentLink());
        budgetGateway.save(budget);

        publisher.publishOrcamentoAprovado(serviceOrderId);

        return preference.paymentLink();
    }

    public void reject(UUID serviceOrderId, UUID requestingCustomerId, String motivo) {
        verifyOwnership(serviceOrderId, requestingCustomerId);
        Budget budget = findBudget(serviceOrderId);

        budget.reject(motivo);
        budgetGateway.save(budget);

        publisher.publishOrcamentoRecusado(serviceOrderId, motivo);
    }

    private Budget findBudget(UUID serviceOrderId) {
        return budgetGateway.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado para a OS: " + serviceOrderId));
    }

    private ServiceOrderInfo verifyOwnership(UUID serviceOrderId, UUID requestingCustomerId) {
        ServiceOrderInfo order = coreServiceGateway.findServiceOrder(serviceOrderId);
        if (order == null || !order.customerId().equals(requestingCustomerId)) {
            throw new ForbiddenException("Ordem de serviço não encontrada ou não pertence ao cliente autenticado");
        }
        return order;
    }
}
