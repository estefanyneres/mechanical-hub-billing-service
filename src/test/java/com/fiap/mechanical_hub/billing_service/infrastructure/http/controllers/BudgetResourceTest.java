package com.fiap.mechanical_hub.billing_service.infrastructure.http.controllers;

import com.fiap.mechanical_hub.billing_service.application.dto.RejectBudgetRequest;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetApprovalUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetResourceTest {

    @Mock
    private BudgetApprovalUseCase useCase;

    private BudgetResource resource;
    private UUID serviceOrderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        resource = new BudgetResource(useCase);
        serviceOrderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve aprovar o orcamento e devolver 200 com o link de pagamento")
    void approve_ShouldReturn200WithPaymentLink() {
        when(useCase.approve(serviceOrderId, customerId)).thenReturn("https://mercadopago.com/pay/pref-123");

        ResponseEntity<Map<String, String>> result = resource.approve(serviceOrderId, customerId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("paymentLink", "https://mercadopago.com/pay/pref-123");
    }

    @Test
    @DisplayName("Deve repropagar a excecao quando o useCase falhar ao aprovar")
    void approve_ShouldRethrowWhenUseCaseFails() {
        when(useCase.approve(serviceOrderId, customerId)).thenThrow(new RuntimeException("falha simulada"));

        assertThatThrownBy(() -> resource.approve(serviceOrderId, customerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("falha simulada");
    }

    @Test
    @DisplayName("Deve recusar o orcamento e devolver 204")
    void reject_ShouldReturn204() {
        var request = new RejectBudgetRequest("Preço muito alto");

        ResponseEntity<Void> result = resource.reject(serviceOrderId, customerId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(useCase).reject(serviceOrderId, customerId, "Preço muito alto");
    }
}
