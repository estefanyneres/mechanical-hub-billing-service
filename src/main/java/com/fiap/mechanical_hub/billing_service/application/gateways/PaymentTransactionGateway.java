package com.fiap.mechanical_hub.billing_service.application.gateways;

import com.fiap.mechanical_hub.billing_service.domain.entities.PaymentTransaction;

public interface PaymentTransactionGateway {

    PaymentTransaction save(PaymentTransaction transaction);
}
