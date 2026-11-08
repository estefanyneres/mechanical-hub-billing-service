package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;

record PaymentResponseDto(
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("external_reference") String externalReference
) {
}
