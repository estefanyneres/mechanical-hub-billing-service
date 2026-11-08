package com.fiap.mechanical_hub.billing_service.application.dto.saga;

import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoGeradoEvent(UUID serviceOrderId, BigDecimal valor) {
}
