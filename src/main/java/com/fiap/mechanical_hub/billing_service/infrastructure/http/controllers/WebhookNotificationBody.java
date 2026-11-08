package com.fiap.mechanical_hub.billing_service.infrastructure.http.controllers;

/**
 * Payload minimo que o Mercado Pago manda no corpo do webhook (TASK-035)
 * -- so usado como fallback: o data.id "oficial" vem do query param da
 * requisicao, nao do corpo (ver MercadoPagoSignatureValidator).
 */
record WebhookNotificationBody(String type, String action, WebhookData data) {
    record WebhookData(String id) {}
}
