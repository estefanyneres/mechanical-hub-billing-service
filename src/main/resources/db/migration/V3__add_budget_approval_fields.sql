-- TASK-034: aprovacao/recusa do orcamento pelo cliente, mais os dados da
-- preferencia de pagamento criada no Mercado Pago quando aprovado.
ALTER TABLE orcamentos
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN mercado_pago_preference_id VARCHAR(255),
    ADD COLUMN payment_link VARCHAR(500),
    ADD COLUMN rejection_reason VARCHAR(500);
