package com.fiap.mechanical_hub.billing_service.application.usecases;

import com.fiap.mechanical_hub.billing_service.application.gateways.BudgetGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.PaymentTransactionGateway;
import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.domain.entities.PaymentTransaction;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoClient;
import com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago.MercadoPagoPaymentInfo;
import com.fiap.mechanical_hub.billing_service.infrastructure.messaging.SagaEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookUseCaseTest {

    @Mock
    private MercadoPagoClient mercadoPagoClient;
    @Mock
    private BudgetGateway budgetGateway;
    @Mock
    private PaymentTransactionGateway paymentTransactionGateway;
    @Mock
    private SagaEventPublisher publisher;

    private PaymentWebhookUseCase useCase;
    private UUID serviceOrderId;

    @BeforeEach
    void setUp() {
        useCase = new PaymentWebhookUseCase(mercadoPagoClient, budgetGateway, paymentTransactionGateway, publisher);
        serviceOrderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve publicar pagamento.confirmado quando o status for approved")
    void processNotification_ShouldPublishConfirmado_WhenApproved() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("approved", "accredited", serviceOrderId.toString()));
        when(budgetGateway.findByServiceOrderId(serviceOrderId))
                .thenReturn(Optional.of(Budget.create(serviceOrderId, new BigDecimal("100.00"))));

        useCase.processNotification("999");

        verify(publisher).publishPagamentoConfirmado(serviceOrderId);
        verify(publisher, never()).publishPagamentoFalhou(any(), any());
        verify(paymentTransactionGateway).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Deve publicar pagamento.falhou quando o status for rejected, usando o status_detail como motivo")
    void processNotification_ShouldPublishFalhou_WhenRejected() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("rejected", "cc_rejected_insufficient_amount",
                        serviceOrderId.toString()));
        when(budgetGateway.findByServiceOrderId(serviceOrderId))
                .thenReturn(Optional.of(Budget.create(serviceOrderId, new BigDecimal("100.00"))));

        useCase.processNotification("999");

        verify(publisher).publishPagamentoFalhou(serviceOrderId, "cc_rejected_insufficient_amount");
        verify(publisher, never()).publishPagamentoConfirmado(any());
    }

    @Test
    @DisplayName("Deve publicar pagamento.falhou quando o status for cancelled")
    void processNotification_ShouldPublishFalhou_WhenCancelled() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("cancelled", null, serviceOrderId.toString()));
        when(budgetGateway.findByServiceOrderId(serviceOrderId))
                .thenReturn(Optional.of(Budget.create(serviceOrderId, new BigDecimal("100.00"))));

        useCase.processNotification("999");

        verify(publisher).publishPagamentoFalhou(serviceOrderId, "cancelled");
    }

    @Test
    @DisplayName("Nao deve publicar nada quando o status for intermediario (pending)")
    void processNotification_ShouldNotPublish_WhenPending() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("pending", null, serviceOrderId.toString()));
        when(budgetGateway.findByServiceOrderId(serviceOrderId))
                .thenReturn(Optional.of(Budget.create(serviceOrderId, new BigDecimal("100.00"))));

        useCase.processNotification("999");

        verifyNoInteractions(publisher);
        verify(paymentTransactionGateway).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Nao deve processar quando a OS do external_reference nao tem orcamento conhecido")
    void processNotification_ShouldIgnore_WhenOrderUnknown() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("approved", null, serviceOrderId.toString()));
        when(budgetGateway.findByServiceOrderId(serviceOrderId)).thenReturn(Optional.empty());

        useCase.processNotification("999");

        verifyNoInteractions(publisher);
        verify(paymentTransactionGateway, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve processar quando o external_reference nao for um UUID valido")
    void processNotification_ShouldIgnore_WhenExternalReferenceInvalid() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("approved", null, "not-a-uuid"));

        useCase.processNotification("999");

        verifyNoInteractions(publisher);
        verifyNoInteractions(budgetGateway);
        verifyNoInteractions(paymentTransactionGateway);
    }

    @Test
    @DisplayName("Nao deve processar quando o external_reference estiver nulo")
    void processNotification_ShouldIgnore_WhenExternalReferenceNull() {
        when(mercadoPagoClient.getPayment("999"))
                .thenReturn(new MercadoPagoPaymentInfo("approved", null, null));

        useCase.processNotification("999");

        verifyNoInteractions(publisher);
        verifyNoInteractions(budgetGateway);
    }
}
