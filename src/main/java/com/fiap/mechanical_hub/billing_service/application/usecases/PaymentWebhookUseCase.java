package com.fiap.mechanical_hub.billing_service.application.usecases;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.PaymentTransactionGateway;
import com.fiap.mechanical_hub.billing_service.domain.entities.PaymentTransaction;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoClient;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoPaymentInfo;
import com.fiap.mechanical_hub.billing_service.infrastructure.messaging.SagaEventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;

/**
 * Processa a notificacao do webhook do Mercado Pago (TASK-035) -- o
 * payload em si so traz o id do pagamento, a chamada de verdade pra
 * saber o status acontece aqui (GET /v1/payments/{id}).
 */
@Slf4j
public class PaymentWebhookUseCase {

    private static final Set<String> APPROVED_STATUSES = Set.of("approved");
    private static final Set<String> FAILED_STATUSES = Set.of("rejected", "cancelled");

    private final MercadoPagoClient mercadoPagoClient;
    private final BudgetGateway budgetGateway;
    private final PaymentTransactionGateway paymentTransactionGateway;
    private final SagaEventPublisher publisher;

    public PaymentWebhookUseCase(MercadoPagoClient mercadoPagoClient, BudgetGateway budgetGateway,
                                  PaymentTransactionGateway paymentTransactionGateway, SagaEventPublisher publisher) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.budgetGateway = budgetGateway;
        this.paymentTransactionGateway = paymentTransactionGateway;
        this.publisher = publisher;
    }

    public void processNotification(String paymentId) {
        MercadoPagoPaymentInfo payment = mercadoPagoClient.getPayment(paymentId);

        UUID serviceOrderId;
        try {
            serviceOrderId = UUID.fromString(payment.externalReference());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Pagamento {} sem external_reference valido - ignorado", paymentId);
            return;
        }

        if (budgetGateway.findByServiceOrderId(serviceOrderId).isEmpty()) {
            log.warn("Pagamento {} referencia a OS {} sem orcamento conhecido - ignorado", paymentId, serviceOrderId);
            return;
        }

        paymentTransactionGateway.save(
                PaymentTransaction.register(serviceOrderId, paymentId, payment.status()));

        // Tri-estado deliberado: "approved"/"rejected"/"cancelled" sao
        // finais e disparam o Saga; qualquer outro status ("pending",
        // "in_process", etc.) so fica registrado pra auditoria -- o
        // Mercado Pago manda uma NOVA notificacao quando o status real
        // mudar, nao da pra adivinhar o resultado agora.
        if (APPROVED_STATUSES.contains(payment.status())) {
            publisher.publishPagamentoConfirmado(serviceOrderId);
        } else if (FAILED_STATUSES.contains(payment.status())) {
            String motivo = payment.statusDetail() != null ? payment.statusDetail() : payment.status();
            publisher.publishPagamentoFalhou(serviceOrderId, motivo);
        } else {
            log.info("Pagamento {} da OS {} em status intermediario ({}) - aguardando proxima notificacao",
                    paymentId, serviceOrderId, payment.status());
        }
    }
}
