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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetApprovalUseCaseTest {

    @Mock
    private BudgetGateway budgetGateway;
    @Mock
    private CoreServiceGateway coreServiceGateway;
    @Mock
    private MercadoPagoClient mercadoPagoClient;
    @Mock
    private SagaEventPublisher publisher;

    private BudgetApprovalUseCase useCase;
    private UUID serviceOrderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        useCase = new BudgetApprovalUseCase(budgetGateway, coreServiceGateway, mercadoPagoClient, publisher);
        serviceOrderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve aprovar o orcamento, criar a preferencia no Mercado Pago e publicar orcamento.aprovado")
    void approve_ShouldCreatePreferenceAndPublishEvent_WhenCustomerOwnsOrder() {
        var order = new ServiceOrderInfo("OS-202601-0001", customerId);
        Budget budget = Budget.create(serviceOrderId, new BigDecimal("150.00"));
        var preference = new MercadoPagoPreference("pref-123", "https://mercadopago.com/pay/pref-123");

        when(coreServiceGateway.findServiceOrder(serviceOrderId)).thenReturn(order);
        when(budgetGateway.findByServiceOrderId(serviceOrderId)).thenReturn(Optional.of(budget));
        when(mercadoPagoClient.createPreference("OS-202601-0001", serviceOrderId, new BigDecimal("150.00")))
                .thenReturn(preference);
        when(budgetGateway.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        String paymentLink = useCase.approve(serviceOrderId, customerId);

        assertThat(paymentLink).isEqualTo("https://mercadopago.com/pay/pref-123");
        verify(budgetGateway).save(eq(budget));
        assertThat(budget.getMercadoPagoPreferenceId()).isEqualTo("pref-123");
        verify(publisher).publishOrcamentoAprovado(serviceOrderId);
    }

    @Test
    @DisplayName("Deve recusar o orcamento e publicar orcamento.recusado com o motivo")
    void reject_ShouldPublishEvent_WhenCustomerOwnsOrder() {
        var order = new ServiceOrderInfo("OS-202601-0001", customerId);
        Budget budget = Budget.create(serviceOrderId, new BigDecimal("150.00"));

        when(coreServiceGateway.findServiceOrder(serviceOrderId)).thenReturn(order);
        when(budgetGateway.findByServiceOrderId(serviceOrderId)).thenReturn(Optional.of(budget));
        when(budgetGateway.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.reject(serviceOrderId, customerId, "Preço muito alto");

        verify(budgetGateway).save(eq(budget));
        assertThat(budget.getRejectionReason()).isEqualTo("Preço muito alto");
        verify(publisher).publishOrcamentoRecusado(serviceOrderId, "Preço muito alto");
        verifyNoInteractions(mercadoPagoClient);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando a OS pertencer a outro cliente")
    void approve_ShouldThrowForbidden_WhenOrderBelongsToAnotherCustomer() {
        var order = new ServiceOrderInfo("OS-202601-0001", UUID.randomUUID());
        when(coreServiceGateway.findServiceOrder(serviceOrderId)).thenReturn(order);

        assertThatThrownBy(() -> useCase.approve(serviceOrderId, customerId))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(mercadoPagoClient, publisher);
        verify(budgetGateway, never()).findByServiceOrderId(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando a OS não existir (mesma mensagem, não revela diferença)")
    void approve_ShouldThrowForbidden_WhenOrderDoesNotExist() {
        when(coreServiceGateway.findServiceOrder(serviceOrderId)).thenReturn(null);

        assertThatThrownBy(() -> useCase.approve(serviceOrderId, customerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o orcamento ainda não existir pra essa OS")
    void approve_ShouldThrowNotFound_WhenBudgetDoesNotExist() {
        var order = new ServiceOrderInfo("OS-202601-0001", customerId);
        when(coreServiceGateway.findServiceOrder(serviceOrderId)).thenReturn(order);
        when(budgetGateway.findByServiceOrderId(serviceOrderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.approve(serviceOrderId, customerId))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(mercadoPagoClient);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o orcamento já tiver sido decidido (aprovar duas vezes)")
    void approve_ShouldPropagateException_WhenBudgetAlreadyDecided() {
        var order = new ServiceOrderInfo("OS-202601-0001", customerId);
        Budget budget = Budget.create(serviceOrderId, new BigDecimal("150.00"));
        budget.reject("já recusado antes");

        when(coreServiceGateway.findServiceOrder(serviceOrderId)).thenReturn(order);
        when(budgetGateway.findByServiceOrderId(serviceOrderId)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> useCase.approve(serviceOrderId, customerId))
                .isInstanceOf(com.fiap.mechanical_hub.billing_service.domain.exceptions.BusinessRuleException.class);

        verifyNoInteractions(mercadoPagoClient, publisher);
    }
}
