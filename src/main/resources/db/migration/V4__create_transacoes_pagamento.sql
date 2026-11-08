-- TASK-035: log de cada notificacao de pagamento recebida do Mercado
-- Pago. Sem FK pra orcamentos: pode existir mais de um orcamento por OS
-- ao longo do tempo (reaberturas apos falha de execucao - RFC-001),
-- service_order_id nao e unique em orcamentos.
CREATE TABLE transacoes_pagamento (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_order_id UUID NOT NULL,
    mercado_pago_payment_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transacoes_pagamento_service_order_id ON transacoes_pagamento(service_order_id);
