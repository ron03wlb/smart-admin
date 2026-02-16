-- ============================================================================
-- V4__unique_constraint_tenant.sql - Reform UNIQUE constraints for multi-tenant (G15)
-- ============================================================================
-- Reforms 11 UNIQUE constraints from global uniqueness to per-tenant uniqueness
-- by adding tenant_id as leading column in each composite constraint.
--
-- Also backfills tenant_id on 3 tables missed from V2:
--   - t_notice_visible_range
--   - t_notice_view_record
--   - t_help_doc_view_record
--
-- Why no application code changes:
--   TenantLineInnerInterceptor auto-injects AND tenant_id = ? into all SQL,
--   so application-level uniqueness checks already have tenant scoping.
--   The constraint reform is a database-level safety net.
--
-- Related:
--   - V2__add_tenant_id.sql (added tenant_id to 45 tables)
--   - SmartTenantLineHandler (SQL WHERE tenant_id = ? injection)
-- ============================================================================

-- ============================================================================
-- Part A: Backfill 3 Tables Missed from V2
-- ============================================================================

-- t_notice_visible_range (has UNIQUE constraint uk_notice_data)
ALTER TABLE t_notice_visible_range ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_notice_visible_range SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_notice_visible_range ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_notice_visible_range ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_notice_visible_range_tenant_id ON t_notice_visible_range(tenant_id);
COMMENT ON COLUMN t_notice_visible_range.tenant_id IS '租戶ID';

-- t_notice_view_record (composite PK, no UNIQUE constraint)
ALTER TABLE t_notice_view_record ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_notice_view_record SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_notice_view_record ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_notice_view_record ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_notice_view_record_tenant_id ON t_notice_view_record(tenant_id);
COMMENT ON COLUMN t_notice_view_record.tenant_id IS '租戶ID';

-- t_help_doc_view_record (composite PK, no UNIQUE constraint)
ALTER TABLE t_help_doc_view_record ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_help_doc_view_record SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_help_doc_view_record ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_help_doc_view_record ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_help_doc_view_record_tenant_id ON t_help_doc_view_record(tenant_id);
COMMENT ON COLUMN t_help_doc_view_record.tenant_id IS '租戶ID';

-- ============================================================================
-- Part B: Reform 11 UNIQUE Constraints to (tenant_id, ...)
-- ============================================================================

-- 1. t_change_log: version_unique (update_version) -> (tenant_id, update_version)
ALTER TABLE t_change_log DROP CONSTRAINT IF EXISTS version_unique;
ALTER TABLE t_change_log ADD CONSTRAINT version_unique UNIQUE (tenant_id, update_version);

-- 2. t_dict: unique_code (dict_code) -> (tenant_id, dict_code)
ALTER TABLE t_dict DROP CONSTRAINT IF EXISTS unique_code;
ALTER TABLE t_dict ADD CONSTRAINT unique_code UNIQUE (tenant_id, dict_code);

-- 3. t_employee: employee_uid_index (employee_uid) -> (tenant_id, employee_uid)
ALTER TABLE t_employee DROP CONSTRAINT IF EXISTS employee_uid_index;
ALTER TABLE t_employee ADD CONSTRAINT employee_uid_index UNIQUE (tenant_id, employee_uid);

-- 4. t_file: uk_file_key (file_key) -> (tenant_id, file_key)
ALTER TABLE t_file DROP CONSTRAINT IF EXISTS uk_file_key;
ALTER TABLE t_file ADD CONSTRAINT uk_file_key UNIQUE (tenant_id, file_key);

-- 5. t_login_fail: uid_and_utype (user_id, user_type) -> (tenant_id, user_id, user_type)
ALTER TABLE t_login_fail DROP CONSTRAINT IF EXISTS uid_and_utype;
ALTER TABLE t_login_fail ADD CONSTRAINT uid_and_utype UNIQUE (tenant_id, user_id, user_type);

-- 6. t_notice_visible_range: uk_notice_data (notice_id, data_type, data_id) -> (tenant_id, notice_id, data_type, data_id)
ALTER TABLE t_notice_visible_range DROP CONSTRAINT IF EXISTS uk_notice_data;
ALTER TABLE t_notice_visible_range ADD CONSTRAINT uk_notice_data UNIQUE (tenant_id, notice_id, data_type, data_id);

-- 7. t_oa_enterprise_employee: uk_enterprise_employee (enterprise_id, employee_id) -> (tenant_id, enterprise_id, employee_id)
ALTER TABLE t_oa_enterprise_employee DROP CONSTRAINT IF EXISTS uk_enterprise_employee;
ALTER TABLE t_oa_enterprise_employee ADD CONSTRAINT uk_enterprise_employee UNIQUE (tenant_id, enterprise_id, employee_id);

-- 8. t_role: role_code_uni (role_code) -> (tenant_id, role_code)
ALTER TABLE t_role DROP CONSTRAINT IF EXISTS role_code_uni;
ALTER TABLE t_role ADD CONSTRAINT role_code_uni UNIQUE (tenant_id, role_code);

-- 9. t_role_employee: uk_role_employee (role_id, employee_id) -> (tenant_id, role_id, employee_id)
ALTER TABLE t_role_employee DROP CONSTRAINT IF EXISTS uk_role_employee;
ALTER TABLE t_role_employee ADD CONSTRAINT uk_role_employee UNIQUE (tenant_id, role_id, employee_id);

-- 10. t_serial_number: key_name (business_name) -> (tenant_id, business_name)
ALTER TABLE t_serial_number DROP CONSTRAINT IF EXISTS key_name;
ALTER TABLE t_serial_number ADD CONSTRAINT key_name UNIQUE (tenant_id, business_name);

-- 11. t_table_column: uni_employee_table (user_id, table_id) -> (tenant_id, user_id, table_id)
ALTER TABLE t_table_column DROP CONSTRAINT IF EXISTS uni_employee_table;
ALTER TABLE t_table_column ADD CONSTRAINT uni_employee_table UNIQUE (tenant_id, user_id, table_id);

-- ============================================================================
-- Part C: Drop Redundant Simple tenant_id Indexes
-- ============================================================================
-- The composite UNIQUE (tenant_id, ...) serves as an index with tenant_id
-- as leading column, making these simple indexes redundant.

-- 10 from V2 (tables that had UNIQUE constraints)
DROP INDEX IF EXISTS idx_change_log_tenant_id;
DROP INDEX IF EXISTS idx_dict_tenant_id;
DROP INDEX IF EXISTS idx_employee_tenant_id;
DROP INDEX IF EXISTS idx_file_tenant_id;
DROP INDEX IF EXISTS idx_login_fail_tenant_id;
DROP INDEX IF EXISTS idx_oa_enterprise_employee_tenant_id;
DROP INDEX IF EXISTS idx_role_tenant_id;
DROP INDEX IF EXISTS idx_role_employee_tenant_id;
DROP INDEX IF EXISTS idx_serial_number_tenant_id;
DROP INDEX IF EXISTS idx_table_column_tenant_id;

-- 1 from Part A (t_notice_visible_range got UNIQUE constraint reform)
DROP INDEX IF EXISTS idx_notice_visible_range_tenant_id;

-- ============================================================================
-- Part D: Update Statistics
-- ============================================================================
ANALYZE t_change_log, t_dict, t_employee, t_file, t_login_fail,
        t_notice_visible_range, t_notice_view_record, t_help_doc_view_record,
        t_oa_enterprise_employee, t_role, t_role_employee,
        t_serial_number, t_table_column;
