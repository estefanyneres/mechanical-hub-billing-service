package com.fiap.mechanical_hub.billing_service.domain.entities;

import com.fiap.mechanical_hub.billing_service.domain.enums.BudgetStatusEnum;
import com.fiap.mechanical_hub.billing_service.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetTest {

    @Test
    @DisplayName("Deve criar um orcamento pendente com ID gerado e data de criacao preenchida")
    void create_ShouldGenerateIdAndCreatedAt() {
        UUID serviceOrderId = UUID.randomUUID();
        BigDecimal total = new BigDecimal("200.00");

        Budget budget = Budget.create(serviceOrderId, total);

        assertThat(budget.getId()).isNotNull();
        assertThat(budget.getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(budget.getTotalValue()).isEqualTo(total);
        assertThat(budget.getStatus()).isEqualTo(BudgetStatusEnum.PENDENTE);
        assertThat(budget.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve aprovar um orcamento pendente, guardando a preferencia do Mercado Pago")
    void approve_ShouldSetStatusAndPreferenceData_WhenPending() {
        Budget budget = Budget.create(UUID.randomUUID(), new BigDecimal("100.00"));

        budget.approve("pref-123", "https://mercadopago.com/pay/pref-123");

        assertThat(budget.getStatus()).isEqualTo(BudgetStatusEnum.APROVADO);
        assertThat(budget.getMercadoPagoPreferenceId()).isEqualTo("pref-123");
        assertThat(budget.getPaymentLink()).isEqualTo("https://mercadopago.com/pay/pref-123");
    }

    @Test
    @DisplayName("Deve recusar um orcamento pendente, guardando o motivo")
    void reject_ShouldSetStatusAndReason_WhenPending() {
        Budget budget = Budget.create(UUID.randomUUID(), new BigDecimal("100.00"));

        budget.reject("Preço muito alto");

        assertThat(budget.getStatus()).isEqualTo(BudgetStatusEnum.RECUSADO);
        assertThat(budget.getRejectionReason()).isEqualTo("Preço muito alto");
    }

    @Test
    @DisplayName("Nao deve permitir aprovar um orcamento que ja foi aprovado")
    void approve_ShouldThrow_WhenAlreadyApproved() {
        Budget budget = Budget.create(UUID.randomUUID(), new BigDecimal("100.00"));
        budget.approve("pref-123", "https://mercadopago.com/pay/pref-123");

        assertThatThrownBy(() -> budget.approve("pref-456", "https://mercadopago.com/pay/pref-456"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aprovado");
    }

    @Test
    @DisplayName("Nao deve permitir recusar um orcamento que ja foi recusado")
    void reject_ShouldThrow_WhenAlreadyRejected() {
        Budget budget = Budget.create(UUID.randomUUID(), new BigDecimal("100.00"));
        budget.reject("Motivo original");

        assertThatThrownBy(() -> budget.reject("Outro motivo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("recusado");
    }

    @Test
    @DisplayName("Nao deve permitir recusar um orcamento que ja foi aprovado")
    void reject_ShouldThrow_WhenAlreadyApproved() {
        Budget budget = Budget.create(UUID.randomUUID(), new BigDecimal("100.00"));
        budget.approve("pref-123", "https://mercadopago.com/pay/pref-123");

        assertThatThrownBy(() -> budget.reject("Motivo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aprovado");
    }
}
