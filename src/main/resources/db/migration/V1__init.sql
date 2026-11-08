-- Migrations do billing-service comecam aqui (TASK-033/034: orcamentos,
-- transacoes de pagamento).
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SET timezone TO 'UTC';
