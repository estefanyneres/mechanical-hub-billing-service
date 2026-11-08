package com.fiap.mechanical_hub.billing_service.domain.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de auditoria de cada notificacao de pagamento recebida do
 * Mercado Pago (TASK-035) -- sem metodos de transicao de estado, e so
 * um log; a decisao do que fazer com o status (confirmar/falhar/esperar)
 * acontece no use case, nao aqui.
 */
public class PaymentTransaction {

    private UUID id;
    private UUID serviceOrderId;
    private String mercadoPagoPaymentId;
    private String status;
    private LocalDateTime createdAt;

    public PaymentTransaction() {}

    public PaymentTransaction(UUID id, UUID serviceOrderId, String mercadoPagoPaymentId, String status,
                               LocalDateTime createdAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.mercadoPagoPaymentId = mercadoPagoPaymentId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PaymentTransaction register(UUID serviceOrderId, String mercadoPagoPaymentId, String status) {
        return new PaymentTransaction(UUID.randomUUID(), serviceOrderId, mercadoPagoPaymentId, status,
                LocalDateTime.now());
    }

    public UUID getId() { return id; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public String getMercadoPagoPaymentId() { return mercadoPagoPaymentId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
