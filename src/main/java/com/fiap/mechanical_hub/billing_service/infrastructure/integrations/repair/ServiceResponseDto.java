package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.repair;

import java.math.BigDecimal;

/**
 * Espelha o ServiceResponse do repair-service (GET /services/{id}) --
 * so o campo de preco que o billing-service precisa.
 */
record ServiceResponseDto(BigDecimal laborCost) {
}
