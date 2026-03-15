-- ============================================================================
-- V6__timestamptz_migration.sql - Migrate TIMESTAMP to TIMESTAMPTZ (G2)
-- ============================================================================
-- Converts all TIMESTAMP (WITHOUT TIME ZONE) columns to TIMESTAMPTZ
-- (WITH TIME ZONE) across 48 tables (~100 columns).
--
-- PostgreSQL handles implicit conversion: existing TIMESTAMP values are
-- interpreted using the session timezone (UTC by default). No data loss.
--
-- DEFAULT CURRENT_TIMESTAMP still works — it returns TIMESTAMPTZ when
-- the column type is TIMESTAMPTZ. No DEFAULT changes needed.
--
-- Related:
--   - SmartAdminBaseEntity: already uses OffsetDateTime (G0)
--   - MybatisPlusFillHandler: already uses OffsetDateTime.now(ZoneOffset.UTC)
--   - TenantTimezoneSerializer: converts UTC → tenant timezone on output
-- ============================================================================

-- Ensure session timezone is UTC for consistent conversion
SET LOCAL timezone = 'UTC';

-- ============================================================================
-- Part A: Standard Tables (create_time + update_time only) — 37 tables
-- ============================================================================

ALTER TABLE t_category
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_change_log
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_code_generator_config
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_config
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_data_tracer
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_department
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_dict
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_dict_data
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_employee
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_feedback
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_file
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_goods
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_help_doc
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_help_doc_catalog
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_help_doc_relation
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_help_doc_view_record
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_login_log
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_mail_template
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_menu
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_notice_type
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_notice_view_record
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_oa_bank
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_oa_enterprise
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_oa_enterprise_employee
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_oa_invoice
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_operate_log
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_password_log
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_position
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_reload_item
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_role
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_role_data_scope
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_role_employee
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_role_menu
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

ALTER TABLE t_table_column
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

-- LiteFlow tables (from V1.1__liteflow.sql)

ALTER TABLE t_liteflow_chain
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_liteflow_script
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

ALTER TABLE t_liteflow_execution_metrics
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

-- ============================================================================
-- Part B: Tables with Additional Timestamp Columns — 7 tables
-- ============================================================================

-- t_heart_beat_record: only process_start_time + heart_beat_time (no create/update)
ALTER TABLE t_heart_beat_record
  ALTER COLUMN process_start_time TYPE TIMESTAMPTZ,
  ALTER COLUMN heart_beat_time TYPE TIMESTAMPTZ;

-- t_login_fail: login_lock_begin_time + standard
ALTER TABLE t_login_fail
  ALTER COLUMN login_lock_begin_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

-- t_message: read_time + standard
ALTER TABLE t_message
  ALTER COLUMN read_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

-- t_notice: publish_time + standard
ALTER TABLE t_notice
  ALTER COLUMN publish_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- t_serial_number: last_time + standard
ALTER TABLE t_serial_number
  ALTER COLUMN last_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- t_serial_number_record: last_time + standard
ALTER TABLE t_serial_number_record
  ALTER COLUMN last_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- t_smart_job: last_execute_time + standard
ALTER TABLE t_smart_job
  ALTER COLUMN last_execute_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ,
  ALTER COLUMN update_time TYPE TIMESTAMPTZ;

-- ============================================================================
-- Part C: Tables with Only create_time (no update_time) — 4 tables
-- ============================================================================

-- t_notice_visible_range: create_time only
ALTER TABLE t_notice_visible_range
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- t_reload_result: create_time only
ALTER TABLE t_reload_result
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- t_smart_job_log: execute_start_time + execute_end_time + create_time
ALTER TABLE t_smart_job_log
  ALTER COLUMN execute_start_time TYPE TIMESTAMPTZ,
  ALTER COLUMN execute_end_time TYPE TIMESTAMPTZ,
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- t_liteflow_execution_log: create_time only
ALTER TABLE t_liteflow_execution_log
  ALTER COLUMN create_time TYPE TIMESTAMPTZ;

-- ============================================================================
-- Part D: Update Statistics
-- ============================================================================
ANALYZE t_category, t_change_log, t_code_generator_config, t_config,
        t_data_tracer, t_department, t_dict, t_dict_data, t_employee,
        t_feedback, t_file, t_goods, t_heart_beat_record,
        t_help_doc, t_help_doc_catalog, t_help_doc_relation,
        t_help_doc_view_record, t_login_fail, t_login_log,
        t_mail_template, t_menu, t_message, t_notice, t_notice_type,
        t_notice_view_record, t_notice_visible_range,
        t_oa_bank, t_oa_enterprise, t_oa_enterprise_employee, t_oa_invoice,
        t_operate_log, t_password_log, t_position,
        t_reload_item, t_reload_result, t_role, t_role_data_scope,
        t_role_employee, t_role_menu, t_serial_number, t_serial_number_record,
        t_smart_job, t_smart_job_log, t_table_column,
        t_liteflow_chain, t_liteflow_script,
        t_liteflow_execution_log, t_liteflow_execution_metrics;
