package com.fiap.mechanical_hub.billing_service.application.dto.saga;

import java.util.UUID;

/**
 * Espelha o OrcamentoSolicitadoEvent do core-service -- cada servico
 * mantem a propria visao do contrato, nao compartilha a classe entre
 * repositorios (mesmo padrao usado em todo o projeto).
 */
public record OrcamentoSolicitadoEvent(UUID serviceOrderId, String orderNumber, String requestDescription) {
}
