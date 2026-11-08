package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago;

public record MercadoPagoPaymentInfo(String status, String statusDetail, String externalReference) {
}
