-- =====================================================
-- Add application_name column to LiteFlow tables
-- Required by LiteFlow SQL rule source for multi-application support
-- =====================================================

-- Add application_name to t_liteflow_chain
ALTER TABLE t_liteflow_chain
    ADD COLUMN IF NOT EXISTS application_name VARCHAR(100);

COMMENT ON COLUMN t_liteflow_chain.application_name IS 'Application name for multi-application support (LiteFlow SQL parser requirement)';

-- Add application_name to t_liteflow_script
ALTER TABLE t_liteflow_script
    ADD COLUMN IF NOT EXISTS application_name VARCHAR(100);

COMMENT ON COLUMN t_liteflow_script.application_name IS 'Application name for multi-application support (LiteFlow SQL parser requirement)';

-- Update existing records with default application name
UPDATE t_liteflow_chain
SET application_name = 'smartadmin-igaming-integration-test'
WHERE application_name IS NULL;

UPDATE t_liteflow_script
SET application_name = 'smartadmin-igaming-integration-test'
WHERE application_name IS NULL;
