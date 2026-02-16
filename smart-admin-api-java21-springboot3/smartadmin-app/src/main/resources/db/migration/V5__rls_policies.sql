-- ============================================================================
-- V5__rls_policies.sql - PostgreSQL Row-Level Security for Multi-Tenant (G11)
-- ============================================================================
-- Implements dual-layer tenant protection (D1 decision):
--   Layer 1: MyBatis-Plus TenantLineInnerInterceptor (app layer, G1)
--   Layer 2: PostgreSQL RLS (database layer, this migration)
--
-- Design:
--   - Creates non-superuser `smartadmin_app` role (superusers bypass all RLS)
--   - Enables RLS on 48 tenant-aware tables
--   - Creates tenant_isolation policy using session variable
--   - Policy reads `app.current_tenant_id` set by RlsSessionInterceptor
--
-- Activation: Switch datasource to smartadmin_app + set tenant.rls.enabled=true
--
-- Related:
--   - V2__add_tenant_id.sql (added tenant_id to 45 tables)
--   - V4__unique_constraint_tenant.sql (backfilled 3 missed tables)
--   - RlsSessionInterceptor.java (SET LOCAL per query)
--   - SmartTenantLineHandler.java (app-layer tenant filter)
-- ============================================================================

-- ============================================================================
-- Part A: Create Application Role (Non-Superuser)
-- ============================================================================
-- PostgreSQL superusers ALWAYS bypass RLS, even with FORCE ROW LEVEL SECURITY.
-- We create a dedicated non-superuser role for application DML operations.
-- Flyway continues using the superuser (postgres) for DDL migrations.

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartadmin_app') THEN
    CREATE ROLE smartadmin_app LOGIN PASSWORD 'changeme_in_production';
  END IF;
END $$;

-- ============================================================================
-- Part B: Grant DML Permissions to Application Role
-- ============================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO smartadmin_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO smartadmin_app;

-- Default privileges for future tables/sequences created by superuser (Flyway)
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO smartadmin_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO smartadmin_app;

-- ============================================================================
-- Part C: Enable RLS on 48 Tenant-Aware Tables
-- ============================================================================
-- Scope: 45 tables from V2 + 3 tables from V4 backfill
-- Excluded: t_tenant (tenant management), flyway_schema_history (Flyway internal)

-- System module (8 tables)
ALTER TABLE t_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_department ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_position ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role_data_scope ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_menu ENABLE ROW LEVEL SECURITY;

-- Business module (3 tables)
ALTER TABLE t_goods ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_category ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_brand ENABLE ROW LEVEL SECURITY;

-- OA module (6 tables)
ALTER TABLE t_oa_invoice ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_bank ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_enterprise ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_enterprise_employee ENABLE ROW LEVEL SECURITY;

-- Support BaseEntity tables (24 tables)
ALTER TABLE t_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_dict ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_dict_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_file ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_help_doc_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_help_doc ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_message ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mail_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_operate_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_login_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_login_fail ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_password_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_reload_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_serial_number ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_serial_number_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_table_column ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_change_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_code_generator_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_data_tracer ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_smart_job ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_liteflow_chain ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_liteflow_script ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_liteflow_execution_metrics ENABLE ROW LEVEL SECURITY;

-- Support Non-BaseEntity tables (4 tables)
ALTER TABLE t_reload_result ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_smart_job_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_liteflow_execution_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_heart_beat_record ENABLE ROW LEVEL SECURITY;

-- V4 Backfill tables (3 tables)
ALTER TABLE t_notice_visible_range ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice_view_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_help_doc_view_record ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- Part D: Create Tenant Isolation Policies
-- ============================================================================
-- Policy pattern:
--   USING: Controls which rows are visible (SELECT/UPDATE/DELETE)
--   WITH CHECK: Controls what values can be written (INSERT/UPDATE)
--   current_setting('app.current_tenant_id', true): Returns NULL if not set
--     → NULL cast to BIGINT → no rows match → safe deny-by-default
--   TO smartadmin_app: Only applies to app role; superuser unaffected

-- System module (8 tables)
CREATE POLICY tenant_isolation ON t_employee
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_department
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_position
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_role
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_role_employee
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_role_menu
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_role_data_scope
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_menu
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- Business module (3 tables)
CREATE POLICY tenant_isolation ON t_goods
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_category
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_brand
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- OA module (6 tables)
CREATE POLICY tenant_isolation ON t_oa_invoice
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_notice
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_notice_type
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_oa_bank
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_oa_enterprise
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_oa_enterprise_employee
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- Support BaseEntity tables (24 tables)
CREATE POLICY tenant_isolation ON t_config
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_dict
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_dict_data
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_file
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_feedback
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_help_doc_catalog
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_help_doc
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_message
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_mail_template
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_operate_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_login_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_login_fail
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_password_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_reload_item
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_serial_number
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_serial_number_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_table_column
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_change_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_code_generator_config
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_data_tracer
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_smart_job
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_liteflow_chain
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_liteflow_script
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_liteflow_execution_metrics
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- Support Non-BaseEntity tables (4 tables)
CREATE POLICY tenant_isolation ON t_reload_result
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_smart_job_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_liteflow_execution_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_heart_beat_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- V4 Backfill tables (3 tables)
CREATE POLICY tenant_isolation ON t_notice_visible_range
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_notice_view_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_help_doc_view_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);
