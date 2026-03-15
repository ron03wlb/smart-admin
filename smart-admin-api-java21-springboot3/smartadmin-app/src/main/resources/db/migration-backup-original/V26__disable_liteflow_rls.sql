-- V26: Disable RLS on LiteFlow Tables
-- Description: LiteFlow uses its own database connection without tenant_id context,
--              so RLS policies block all queries. LiteFlow configuration is system-level,
--              not tenant-specific, so RLS is not needed.
-- Author: iGaming Team
-- Date: 2026-03-13

-- Disable RLS on LiteFlow tables
ALTER TABLE t_liteflow_chain DISABLE ROW LEVEL SECURITY;
ALTER TABLE t_liteflow_script DISABLE ROW LEVEL SECURITY;

-- Drop RLS policies (cleanup)
DROP POLICY IF EXISTS tenant_isolation_liteflow_chain ON t_liteflow_chain;
DROP POLICY IF EXISTS tenant_isolation_liteflow_script ON t_liteflow_script;

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow workflow chain definitions (system-level, no RLS)';
COMMENT ON TABLE t_liteflow_script IS 'LiteFlow script node definitions (system-level, no RLS)';
