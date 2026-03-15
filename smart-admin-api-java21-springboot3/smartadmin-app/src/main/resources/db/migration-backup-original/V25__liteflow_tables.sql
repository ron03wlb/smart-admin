-- V25: LiteFlow Workflow Engine Tables
-- Description: Create LiteFlow chain and script tables for SQL rule source
-- Author: iGaming Team
-- Date: 2026-03-13

-- ============================================================================
-- Drop existing tables if any (from failed previous migrations)
-- ============================================================================
DROP TABLE IF EXISTS t_liteflow_script CASCADE;
DROP TABLE IF EXISTS t_liteflow_chain CASCADE;

-- ============================================================================
-- Table: t_liteflow_chain
-- Description: LiteFlow workflow chain definitions
-- ============================================================================
CREATE TABLE t_liteflow_chain (
    chain_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    chain_code VARCHAR(100) NOT NULL,
    chain_name VARCHAR(200) NOT NULL,
    chain_type SMALLINT NOT NULL DEFAULT 1,
    chain_data TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    remark TEXT,
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uk_chain_code_tenant UNIQUE (chain_code, tenant_id)
);

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow workflow chain definitions';
COMMENT ON COLUMN t_liteflow_chain.chain_code IS 'Unique chain identifier';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS 'Human-readable chain name';
COMMENT ON COLUMN t_liteflow_chain.chain_type IS '1=Normal serial flow, 2=Async flow';
COMMENT ON COLUMN t_liteflow_chain.chain_data IS 'LiteFlow EL expression (e.g., THEN(node1, node2))';
COMMENT ON COLUMN t_liteflow_chain.status IS '1=Enabled (active), 0=Disabled (inactive)';

DROP INDEX IF EXISTS idx_liteflow_chain_tenant;
DROP INDEX IF EXISTS idx_liteflow_chain_status;
CREATE INDEX idx_liteflow_chain_tenant ON t_liteflow_chain(tenant_id);
CREATE INDEX idx_liteflow_chain_status ON t_liteflow_chain(status);

-- ============================================================================
-- Table: t_liteflow_script
-- Description: LiteFlow script node definitions (QLExpress/Groovy/JS)
-- ============================================================================
CREATE TABLE t_liteflow_script (
    script_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    script_code VARCHAR(100) NOT NULL,
    script_name VARCHAR(200) NOT NULL,
    script_type VARCHAR(20) NOT NULL DEFAULT 'qlexpress',
    script_data TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    remark TEXT,
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uk_script_code_tenant UNIQUE (script_code, tenant_id)
);

COMMENT ON TABLE t_liteflow_script IS 'LiteFlow script node definitions';
COMMENT ON COLUMN t_liteflow_script.script_code IS 'Unique script identifier';
COMMENT ON COLUMN t_liteflow_script.script_name IS 'Human-readable script name';
COMMENT ON COLUMN t_liteflow_script.script_type IS 'Script language: qlexpress/groovy/js/python';
COMMENT ON COLUMN t_liteflow_script.script_data IS 'Script source code';
COMMENT ON COLUMN t_liteflow_script.status IS '1=Enabled (active), 0=Disabled (inactive)';

DROP INDEX IF EXISTS idx_liteflow_script_tenant;
DROP INDEX IF EXISTS idx_liteflow_script_type;
DROP INDEX IF EXISTS idx_liteflow_script_status;
CREATE INDEX idx_liteflow_script_tenant ON t_liteflow_script(tenant_id);
CREATE INDEX idx_liteflow_script_type ON t_liteflow_script(script_type);
CREATE INDEX idx_liteflow_script_status ON t_liteflow_script(status);

-- ============================================================================
-- RLS Policies for Multi-Tenant Isolation
-- ============================================================================

-- Enable RLS
ALTER TABLE t_liteflow_chain ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_liteflow_script ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS tenant_isolation_liteflow_chain ON t_liteflow_chain;
DROP POLICY IF EXISTS tenant_isolation_liteflow_script ON t_liteflow_script;

-- Create RLS policies
CREATE POLICY tenant_isolation_liteflow_chain ON t_liteflow_chain
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::BIGINT);

CREATE POLICY tenant_isolation_liteflow_script ON t_liteflow_script
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::BIGINT);

-- ============================================================================
-- Initial Data: Turnover Calculation Main Chain
-- ============================================================================

-- Insert Turnover Calculation Chain (if not exists)
INSERT INTO t_liteflow_chain (
    tenant_id, chain_code, chain_name, chain_type, chain_data, status, remark, created_by
) VALUES (
    1,
    'turnover_calculation_main',
    '流水計算主流程',
    1,
    'THEN(riskFilterNode, statusFactorNode, gameWeightNode, turnoverAggregateNode)',
    1,
    'Turnover calculation chain for bet settlement - 3-layer verification (Layer 1: Risk Filter → Layer 2: Status Factor → Layer 3: Game Weight → Aggregate)',
    0
) ON CONFLICT (chain_code, tenant_id) DO NOTHING;

COMMENT ON CONSTRAINT uk_chain_code_tenant ON t_liteflow_chain IS 'Ensure unique chain code per tenant';
COMMENT ON CONSTRAINT uk_script_code_tenant ON t_liteflow_script IS 'Ensure unique script code per tenant';
