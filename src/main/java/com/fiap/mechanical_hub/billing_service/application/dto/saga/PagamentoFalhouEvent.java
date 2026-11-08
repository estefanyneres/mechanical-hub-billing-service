package com.fiap.mechanical_hub.billing_service.application.dto.saga;

import java.util.UUID;

public record PagamentoFalhouEvent(UUID serviceOrderId, String motivo) {
}
