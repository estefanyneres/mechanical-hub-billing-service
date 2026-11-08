package com.fiap.mechanical_hub.billing_service.domain.entities;

import com.fiap.mechanical_hub.billing_service.domain.enums.BudgetStatusEnum;
import com.fiap.mechanical_hub.billing_service.domain.exceptions.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Budget {

    private UUID id;
    private UUID serviceOrderId;
    private BigDecimal totalValue;
    private BudgetStatusEnum status;
    private String mercadoPagoPreferenceId;
    private String paymentLink;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public Budget() {}

    public Budget(UUID id, UUID serviceOrderId, BigDecimal totalValue, BudgetStatusEnum status,
                  String mercadoPagoPreferenceId, String paymentLink, String rejectionReason,
                  LocalDateTime createdAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.totalValue = totalValue;
        this.status = status;
        this.mercadoPagoPreferenceId = mercadoPagoPreferenceId;
        this.paymentLink = paymentLink;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public static Budget create(UUID serviceOrderId, BigDecimal totalValue) {
        return new Budget(UUID.randomUUID(), serviceOrderId, totalValue, BudgetStatusEnum.PENDENTE,
                null, null, null, LocalDateTime.now());
    }

    /**
     * TASK-034: cliente aprovou -- guarda a preferencia de pagamento ja
     * criada no Mercado Pago (a chamada em si acontece no use case, antes
     * de invocar este metodo, porque precisa do valor/numero da OS pra
     * montar a preferencia).
     */
    public void approve(String mercadoPagoPreferenceId, String paymentLink) {
        validatePending();
        this.status = BudgetStatusEnum.APROVADO;
        this.mercadoPagoPreferenceId = mercadoPagoPreferenceId;
        this.paymentLink = paymentLink;
    }

    public void reject(String motivo) {
        validatePending();
        this.status = BudgetStatusEnum.RECUSADO;
        this.rejectionReason = motivo;
    }

    /**
     * Checagem publica, separada de approve(): o use case precisa
     * confirmar que o orcamento ainda esta pendente ANTES de chamar a API
     * do Mercado Pago (criar uma preferencia de pagamento tem efeito
     * colateral do lado de fora -- descobrir que o orcamento ja tinha
     * sido decidido só depois de criar a preferencia deixaria uma
     * preferencia orfa no Mercado Pago).
     */
    public void validatePending() {
        if (this.status != BudgetStatusEnum.PENDENTE) {
            throw new BusinessRuleException(
                    "Orçamento já foi " + (this.status == BudgetStatusEnum.APROVADO ? "aprovado" : "recusado")
                            + " anteriormente");
        }
    }

    public UUID getId() { return id; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public BigDecimal getTotalValue() { return totalValue; }
    public BudgetStatusEnum getStatus() { return status; }
    public String getMercadoPagoPreferenceId() { return mercadoPagoPreferenceId; }
    public String getPaymentLink() { return paymentLink; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
