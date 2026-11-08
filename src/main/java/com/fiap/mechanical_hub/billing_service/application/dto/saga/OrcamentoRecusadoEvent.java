package com.fiap.mechanical_hub.billing_service.application.dto.saga;

import java.util.UUID;

public record OrcamentoRecusadoEvent(UUID serviceOrderId, String motivo) {
}
