package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.repair;

import java.math.BigDecimal;

/**
 * Espelha o MaterialResponse do repair-service (GET /materials/{id}) --
 * so o campo de preco que o billing-service precisa.
 */
record MaterialResponseDto(BigDecimal unitPrice) {
}
