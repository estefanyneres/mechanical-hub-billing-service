package com.fiap.mechanical_hub.billing_service.infrastructure.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacoes_pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionModel {

    @Id
    private UUID id;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "mercado_pago_payment_id", nullable = false)
    private String mercadoPagoPaymentId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
