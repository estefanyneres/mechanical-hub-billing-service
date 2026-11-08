-- service_order_id sem FK: ordens de servico vivem no core-service,
-- banco diferente (ADR-001).
CREATE TABLE orcamentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_order_id UUID NOT NULL,
    total_value NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orcamentos_service_order_id ON orcamentos(service_order_id);
