-- ============================================================================
-- V2__add_tenant_id.sql - Add tenant_id to all SmartAdmin tables (G1.5)
-- ============================================================================
-- Adds tenant_id column to 45 existing tables for multi-tenant isolation.
--
-- Migration pattern per table:
--   1. ADD COLUMN IF NOT EXISTS tenant_id BIGINT
--   2. UPDATE existing rows SET tenant_id = 1 (default tenant)
--   3. ALTER COLUMN SET NOT NULL
--   4. ALTER COLUMN SET DEFAULT 1
--   5. CREATE INDEX idx_{table}_tenant_id
--   6. COMMENT ON COLUMN
--
-- Excluded tables:
--   - t_tenant (tenant registry, in SmartTenantLineHandler.IGNORE_TABLES)
--   - flyway_schema_history (Flyway internal)
--
-- Related infrastructure:
--   - SmartAdminBaseEntity.tenantId (@TableField(fill = FieldFill.INSERT))
--   - MybatisPlusFillHandler (auto-fill from TenantContext)
--   - SmartTenantLineHandler (SQL WHERE tenant_id = ? injection)
-- ============================================================================

-- ============================================================================
-- System Module (8 tables)
-- ============================================================================

-- t_employee
ALTER TABLE t_employee ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_employee SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_employee ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_employee ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_employee_tenant_id ON t_employee(tenant_id);
COMMENT ON COLUMN t_employee.tenant_id IS '租戶ID';

-- t_department
ALTER TABLE t_department ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_department SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_department ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_department ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_department_tenant_id ON t_department(tenant_id);
COMMENT ON COLUMN t_department.tenant_id IS '租戶ID';

-- t_position
ALTER TABLE t_position ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_position SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_position ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_position ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_position_tenant_id ON t_position(tenant_id);
COMMENT ON COLUMN t_position.tenant_id IS '租戶ID';

-- t_role
ALTER TABLE t_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_role SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_role ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_role ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_role_tenant_id ON t_role(tenant_id);
COMMENT ON COLUMN t_role.tenant_id IS '租戶ID';

-- t_role_employee
ALTER TABLE t_role_employee ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_role_employee SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_role_employee ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_role_employee ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_role_employee_tenant_id ON t_role_employee(tenant_id);
COMMENT ON COLUMN t_role_employee.tenant_id IS '租戶ID';

-- t_role_menu
ALTER TABLE t_role_menu ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_role_menu SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_role_menu ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_role_menu ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_role_menu_tenant_id ON t_role_menu(tenant_id);
COMMENT ON COLUMN t_role_menu.tenant_id IS '租戶ID';

-- t_role_data_scope
ALTER TABLE t_role_data_scope ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_role_data_scope SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_role_data_scope ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_role_data_scope ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_role_data_scope_tenant_id ON t_role_data_scope(tenant_id);
COMMENT ON COLUMN t_role_data_scope.tenant_id IS '租戶ID';

-- t_menu
ALTER TABLE t_menu ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_menu SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_menu ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_menu ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_menu_tenant_id ON t_menu(tenant_id);
COMMENT ON COLUMN t_menu.tenant_id IS '租戶ID';

-- ============================================================================
-- Business Module (3 tables)
-- ============================================================================

-- t_goods
ALTER TABLE t_goods ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_goods SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_goods ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_goods ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_goods_tenant_id ON t_goods(tenant_id);
COMMENT ON COLUMN t_goods.tenant_id IS '租戶ID';

-- t_category
ALTER TABLE t_category ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_category SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_category ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_category ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_category_tenant_id ON t_category(tenant_id);
COMMENT ON COLUMN t_category.tenant_id IS '租戶ID';

-- t_brand
ALTER TABLE t_brand ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_brand SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_brand ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_brand ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_brand_tenant_id ON t_brand(tenant_id);
COMMENT ON COLUMN t_brand.tenant_id IS '租戶ID';

-- ============================================================================
-- OA Module (6 tables)
-- ============================================================================

-- t_oa_invoice
ALTER TABLE t_oa_invoice ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_oa_invoice SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_oa_invoice ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_oa_invoice ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_oa_invoice_tenant_id ON t_oa_invoice(tenant_id);
COMMENT ON COLUMN t_oa_invoice.tenant_id IS '租戶ID';

-- t_notice
ALTER TABLE t_notice ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_notice SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_notice ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_notice ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_notice_tenant_id ON t_notice(tenant_id);
COMMENT ON COLUMN t_notice.tenant_id IS '租戶ID';

-- t_notice_type
ALTER TABLE t_notice_type ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_notice_type SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_notice_type ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_notice_type ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_notice_type_tenant_id ON t_notice_type(tenant_id);
COMMENT ON COLUMN t_notice_type.tenant_id IS '租戶ID';

-- t_oa_bank
ALTER TABLE t_oa_bank ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_oa_bank SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_oa_bank ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_oa_bank ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_oa_bank_tenant_id ON t_oa_bank(tenant_id);
COMMENT ON COLUMN t_oa_bank.tenant_id IS '租戶ID';

-- t_oa_enterprise
ALTER TABLE t_oa_enterprise ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_oa_enterprise SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_oa_enterprise ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_oa_enterprise ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_oa_enterprise_tenant_id ON t_oa_enterprise(tenant_id);
COMMENT ON COLUMN t_oa_enterprise.tenant_id IS '租戶ID';

-- t_oa_enterprise_employee
ALTER TABLE t_oa_enterprise_employee ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_oa_enterprise_employee SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_oa_enterprise_employee ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_oa_enterprise_employee ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_oa_enterprise_employee_tenant_id ON t_oa_enterprise_employee(tenant_id);
COMMENT ON COLUMN t_oa_enterprise_employee.tenant_id IS '租戶ID';

-- ============================================================================
-- Support Module - BaseEntity entities (24 tables)
-- ============================================================================

-- t_config
ALTER TABLE t_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_config SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_config ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_config ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_config_tenant_id ON t_config(tenant_id);
COMMENT ON COLUMN t_config.tenant_id IS '租戶ID';

-- t_dict
ALTER TABLE t_dict ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_dict SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_dict ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_dict ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_dict_tenant_id ON t_dict(tenant_id);
COMMENT ON COLUMN t_dict.tenant_id IS '租戶ID';

-- t_dict_data
ALTER TABLE t_dict_data ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_dict_data SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_dict_data ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_dict_data ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_dict_data_tenant_id ON t_dict_data(tenant_id);
COMMENT ON COLUMN t_dict_data.tenant_id IS '租戶ID';

-- t_file
ALTER TABLE t_file ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_file SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_file ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_file ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_file_tenant_id ON t_file(tenant_id);
COMMENT ON COLUMN t_file.tenant_id IS '租戶ID';

-- t_feedback
ALTER TABLE t_feedback ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_feedback SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_feedback ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_feedback ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_feedback_tenant_id ON t_feedback(tenant_id);
COMMENT ON COLUMN t_feedback.tenant_id IS '租戶ID';

-- t_help_doc_catalog
ALTER TABLE t_help_doc_catalog ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_help_doc_catalog SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_help_doc_catalog ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_help_doc_catalog ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_help_doc_catalog_tenant_id ON t_help_doc_catalog(tenant_id);
COMMENT ON COLUMN t_help_doc_catalog.tenant_id IS '租戶ID';

-- t_help_doc
ALTER TABLE t_help_doc ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_help_doc SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_help_doc ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_help_doc ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_help_doc_tenant_id ON t_help_doc(tenant_id);
COMMENT ON COLUMN t_help_doc.tenant_id IS '租戶ID';

-- t_message
ALTER TABLE t_message ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_message SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_message ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_message ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_message_tenant_id ON t_message(tenant_id);
COMMENT ON COLUMN t_message.tenant_id IS '租戶ID';

-- t_mail_template
ALTER TABLE t_mail_template ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_mail_template SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_mail_template ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_mail_template ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_mail_template_tenant_id ON t_mail_template(tenant_id);
COMMENT ON COLUMN t_mail_template.tenant_id IS '租戶ID';

-- t_operate_log
ALTER TABLE t_operate_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_operate_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_operate_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_operate_log ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_operate_log_tenant_id ON t_operate_log(tenant_id);
COMMENT ON COLUMN t_operate_log.tenant_id IS '租戶ID';

-- t_login_log
ALTER TABLE t_login_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_login_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_login_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_login_log ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_login_log_tenant_id ON t_login_log(tenant_id);
COMMENT ON COLUMN t_login_log.tenant_id IS '租戶ID';

-- t_login_fail
ALTER TABLE t_login_fail ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_login_fail SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_login_fail ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_login_fail ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_login_fail_tenant_id ON t_login_fail(tenant_id);
COMMENT ON COLUMN t_login_fail.tenant_id IS '租戶ID';

-- t_password_log
ALTER TABLE t_password_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_password_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_password_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_password_log ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_password_log_tenant_id ON t_password_log(tenant_id);
COMMENT ON COLUMN t_password_log.tenant_id IS '租戶ID';

-- t_reload_item
ALTER TABLE t_reload_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_reload_item SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_reload_item ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_reload_item ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_reload_item_tenant_id ON t_reload_item(tenant_id);
COMMENT ON COLUMN t_reload_item.tenant_id IS '租戶ID';

-- t_serial_number
ALTER TABLE t_serial_number ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_serial_number SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_serial_number ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_serial_number ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_serial_number_tenant_id ON t_serial_number(tenant_id);
COMMENT ON COLUMN t_serial_number.tenant_id IS '租戶ID';

-- t_serial_number_record
ALTER TABLE t_serial_number_record ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_serial_number_record SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_serial_number_record ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_serial_number_record ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_serial_number_record_tenant_id ON t_serial_number_record(tenant_id);
COMMENT ON COLUMN t_serial_number_record.tenant_id IS '租戶ID';

-- t_table_column
ALTER TABLE t_table_column ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_table_column SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_table_column ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_table_column ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_table_column_tenant_id ON t_table_column(tenant_id);
COMMENT ON COLUMN t_table_column.tenant_id IS '租戶ID';

-- t_change_log
ALTER TABLE t_change_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_change_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_change_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_change_log ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_change_log_tenant_id ON t_change_log(tenant_id);
COMMENT ON COLUMN t_change_log.tenant_id IS '租戶ID';

-- t_code_generator_config
ALTER TABLE t_code_generator_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_code_generator_config SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_code_generator_config ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_code_generator_config ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_code_generator_config_tenant_id ON t_code_generator_config(tenant_id);
COMMENT ON COLUMN t_code_generator_config.tenant_id IS '租戶ID';

-- t_data_tracer
ALTER TABLE t_data_tracer ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_data_tracer SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_data_tracer ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_data_tracer ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_data_tracer_tenant_id ON t_data_tracer(tenant_id);
COMMENT ON COLUMN t_data_tracer.tenant_id IS '租戶ID';

-- t_smart_job
ALTER TABLE t_smart_job ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_smart_job SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_smart_job ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_smart_job ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_smart_job_tenant_id ON t_smart_job(tenant_id);
COMMENT ON COLUMN t_smart_job.tenant_id IS '租戶ID';

-- t_liteflow_chain
ALTER TABLE t_liteflow_chain ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_liteflow_chain SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_liteflow_chain ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_liteflow_chain ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_liteflow_chain_tenant_id ON t_liteflow_chain(tenant_id);
COMMENT ON COLUMN t_liteflow_chain.tenant_id IS '租戶ID';

-- t_liteflow_script
ALTER TABLE t_liteflow_script ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_liteflow_script SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_liteflow_script ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_liteflow_script ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_liteflow_script_tenant_id ON t_liteflow_script(tenant_id);
COMMENT ON COLUMN t_liteflow_script.tenant_id IS '租戶ID';

-- t_liteflow_execution_metrics
ALTER TABLE t_liteflow_execution_metrics ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_liteflow_execution_metrics SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_liteflow_execution_metrics ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_liteflow_execution_metrics ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_liteflow_execution_metrics_tenant_id ON t_liteflow_execution_metrics(tenant_id);
COMMENT ON COLUMN t_liteflow_execution_metrics.tenant_id IS '租戶ID';

-- ============================================================================
-- Support Module - Non-BaseEntity entities (4 tables)
-- These entities have explicit tenantId field, not via SmartAdminBaseEntity
-- ============================================================================

-- t_reload_result
ALTER TABLE t_reload_result ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_reload_result SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_reload_result ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_reload_result ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_reload_result_tenant_id ON t_reload_result(tenant_id);
COMMENT ON COLUMN t_reload_result.tenant_id IS '租戶ID';

-- t_smart_job_log
ALTER TABLE t_smart_job_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_smart_job_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_smart_job_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_smart_job_log ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_smart_job_log_tenant_id ON t_smart_job_log(tenant_id);
COMMENT ON COLUMN t_smart_job_log.tenant_id IS '租戶ID';

-- t_liteflow_execution_log
ALTER TABLE t_liteflow_execution_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_liteflow_execution_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_liteflow_execution_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_liteflow_execution_log ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_liteflow_execution_log_tenant_id ON t_liteflow_execution_log(tenant_id);
COMMENT ON COLUMN t_liteflow_execution_log.tenant_id IS '租戶ID';

-- t_heart_beat_record
ALTER TABLE t_heart_beat_record ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE t_heart_beat_record SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE t_heart_beat_record ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE t_heart_beat_record ALTER COLUMN tenant_id SET DEFAULT 1;
CREATE INDEX IF NOT EXISTS idx_heart_beat_record_tenant_id ON t_heart_beat_record(tenant_id);
COMMENT ON COLUMN t_heart_beat_record.tenant_id IS '租戶ID';
