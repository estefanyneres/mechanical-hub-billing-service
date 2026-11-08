package com.fiap.mechanical_hub.billing_service.infrastructure.database.mappers;

import com.fiap.mechanical_hub.billing_service.domain.entities.PaymentTransaction;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.models.PaymentTransactionModel;

public class PaymentTransactionJpaMapper {

    private PaymentTransactionJpaMapper() {}

    public static PaymentTransactionModel toJpa(PaymentTransaction transaction) {
        return new PaymentTransactionModel(transaction.getId(), transaction.getServiceOrderId(),
                transaction.getMercadoPagoPaymentId(), transaction.getStatus(), transaction.getCreatedAt());
    }

    public static PaymentTransaction toDomain(PaymentTransactionModel model) {
        return new PaymentTransaction(model.getId(), model.getServiceOrderId(), model.getMercadoPagoPaymentId(),
                model.getStatus(), model.getCreatedAt());
    }
}
