package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;

record PreferenceResponseDto(String id, @JsonProperty("init_point") String initPoint) {
}
