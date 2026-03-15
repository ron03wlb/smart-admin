-- =====================================================================
-- V1: Core Infrastructure（核心基礎設施）
-- =====================================================================
-- Consolidates: V1 (baseline) + V2 (tenant_id) + V4 (constraints) +
--               V5 (RLS) + V6 (timestamptz) + V7 (idempotent key)
--
-- Key optimizations:
-- - Tables created with tenant_id (no ALTER needed)
-- - Tables created with TIMESTAMPTZ (no type migration needed)
-- - All constraints created atomically
-- - RLS policies applied immediately to 44 base tables
-- - Deleted columns follow SmartAdmin naming (deleted, not deleted_flag)
--
-- Source: Extracted from smart_admin_v3.sql + V2-V7 optimizations
-- Version: 2.0.0 - Complete reconstruction (方案 B)
-- =====================================================================

-- =====================================================================
-- PART A: Base Table Creation (44 tables from smart_admin_v3.sql)
-- =====================================================================
-- All tables created with:
-- - tenant_id BIGINT NOT NULL DEFAULT 1
-- - TIMESTAMPTZ instead of TIMESTAMP
-- - deleted SMALLINT instead of deleted_flag (SmartAdmin naming)
-- - Indexes on tenant_id (will be dropped later if covered by UNIQUE)
-- =====================================================================

-- ----------------------------
-- Table structure for t_category
-- ----------------------------
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
  category_id SERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  category_name varchar(100) NOT NULL,
  category_type SMALLINT NOT NULL,
  parent_id INTEGER NOT NULL,
  sort INTEGER NOT NULL DEFAULT 0,
  disabled SMALLINT NOT NULL DEFAULT 0,
  deleted SMALLINT NOT NULL DEFAULT 0,
  remark varchar(255) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_category_tenant_id ON t_category(tenant_id);
COMMENT ON COLUMN t_category.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_change_log
-- ----------------------------
DROP TABLE IF EXISTS t_change_log;
CREATE TABLE t_change_log (
  change_log_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  update_version varchar(255) NOT NULL,
  type INTEGER NOT NULL,
  publish_author varchar(255) NOT NULL,
  public_date date NOT NULL,
  content text NOT NULL,
  link text NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_change_log_tenant_id ON t_change_log(tenant_id);
COMMENT ON COLUMN t_change_log.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_code_generator_config
-- ----------------------------
DROP TABLE IF EXISTS t_code_generator_config;
CREATE TABLE t_code_generator_config (
  table_name varchar(255) NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  basic text NULL,
  fields text NULL,
  insert_and_update text NULL,
  delete_info text NULL,
  query_fields text NULL,
  table_fields text NULL,
  detail text NULL,
  create_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_code_generator_config_tenant_id ON t_code_generator_config(tenant_id);
COMMENT ON COLUMN t_code_generator_config.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_config
-- ----------------------------
DROP TABLE IF EXISTS t_config;
CREATE TABLE t_config (
  config_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  config_name varchar(255) NOT NULL,
  config_key varchar(255) NOT NULL,
  config_value text NOT NULL,
  remark varchar(255) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_config_tenant_id ON t_config(tenant_id);
COMMENT ON COLUMN t_config.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_data_tracer
-- ----------------------------
DROP TABLE IF EXISTS t_data_tracer;
CREATE TABLE t_data_tracer (
  data_tracer_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  data_id BIGINT NOT NULL,
  type INTEGER NOT NULL,
  content text NULL,
  diff_old text NULL,
  diff_new text NULL,
  extra_data text NULL,
  user_id BIGINT NOT NULL,
  user_type INTEGER NOT NULL,
  user_name varchar(50) NOT NULL,
  ip varchar(50) NULL DEFAULT NULL,
  ip_region varchar(1000) NULL DEFAULT NULL,
  user_agent varchar(2000) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_data_tracer_tenant_id ON t_data_tracer(tenant_id);
COMMENT ON COLUMN t_data_tracer.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_department
-- ----------------------------
DROP TABLE IF EXISTS t_department;
CREATE TABLE t_department (
  department_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  department_name varchar(50) NOT NULL,
  manager_id BIGINT NULL DEFAULT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  sort INTEGER NOT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_department_tenant_id ON t_department(tenant_id);
COMMENT ON COLUMN t_department.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_dict
-- ----------------------------
DROP TABLE IF EXISTS t_dict;
CREATE TABLE t_dict (
  dict_id bigint NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  dict_name varchar(500) NOT NULL,
  dict_code varchar(500) NOT NULL,
  remark varchar(1000) DEFAULT NULL,
  disabled SMALLINT NOT NULL DEFAULT 0,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dict_tenant_id ON t_dict(tenant_id);
COMMENT ON COLUMN t_dict.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_dict_data
-- ----------------------------
DROP TABLE IF EXISTS t_dict_data;
CREATE TABLE t_dict_data (
  dict_data_id bigint NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  dict_id bigint NOT NULL,
  data_value varchar(500) NOT NULL,
  data_label varchar(500) NOT NULL,
  remark varchar(1000) DEFAULT NULL,
  sort_order int NOT NULL,
  disabled SMALLINT NOT NULL DEFAULT 0,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dict_data_tenant_id ON t_dict_data(tenant_id);
COMMENT ON COLUMN t_dict_data.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_employee
-- ----------------------------
DROP TABLE IF EXISTS t_employee;
CREATE TABLE t_employee (
  employee_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  employee_uid varchar(100) NULL DEFAULT NULL,
  login_name varchar(30) NOT NULL,
  login_pwd varchar(100) NOT NULL,
  actual_name varchar(30) NOT NULL,
  avatar varchar(200) NULL DEFAULT NULL,
  gender SMALLINT NOT NULL DEFAULT 0,
  phone varchar(15) NULL DEFAULT NULL,
  department_id BIGINT NOT NULL,
  position_id BIGINT NULL DEFAULT NULL,
  email varchar(100) NULL DEFAULT NULL,
  disabled SMALLINT NOT NULL,
  deleted SMALLINT NOT NULL,
  administrator SMALLINT NOT NULL DEFAULT 0,
  remark varchar(200) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_employee_tenant_id ON t_employee(tenant_id);
COMMENT ON COLUMN t_employee.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_feedback
-- ----------------------------
DROP TABLE IF EXISTS t_feedback;
CREATE TABLE t_feedback (
  feedback_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  feedback_content text NULL,
  feedback_attachment varchar(500) NULL DEFAULT NULL,
  user_id BIGINT NOT NULL,
  user_type INTEGER NOT NULL,
  user_name varchar(50) NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_feedback_tenant_id ON t_feedback(tenant_id);
COMMENT ON COLUMN t_feedback.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_file
-- ----------------------------
DROP TABLE IF EXISTS t_file;
CREATE TABLE t_file (
  file_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  folder_type SMALLINT NOT NULL,
  file_name varchar(100) NULL DEFAULT NULL,
  file_size INTEGER NULL DEFAULT NULL,
  file_key varchar(200) NOT NULL,
  file_type varchar(50) NOT NULL,
  creator_id BIGINT NULL DEFAULT NULL,
  creator_user_type INTEGER NULL DEFAULT NULL,
  creator_name varchar(100) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_file_tenant_id ON t_file(tenant_id);
COMMENT ON COLUMN t_file.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_goods
-- ----------------------------
DROP TABLE IF EXISTS t_goods;
CREATE TABLE t_goods (
  goods_id SERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  goods_status INTEGER NULL DEFAULT NULL,
  category_id INTEGER NOT NULL,
  goods_name varchar(50) NOT NULL,
  place varchar(255) NULL DEFAULT NULL,
  price decimal(10, 2) NOT NULL,
  shelves SMALLINT NOT NULL,
  deleted SMALLINT NOT NULL,
  remark varchar(255) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_goods_tenant_id ON t_goods(tenant_id);
COMMENT ON COLUMN t_goods.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_heart_beat_record
-- ----------------------------
DROP TABLE IF EXISTS t_heart_beat_record;
CREATE TABLE t_heart_beat_record (
  heart_beat_record_id SERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  project_path varchar(100) NOT NULL,
  server_ip varchar(200) NOT NULL,
  process_no INTEGER NOT NULL,
  process_start_time TIMESTAMPTZ NOT NULL,
  heart_beat_time TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_heart_beat_record_tenant_id ON t_heart_beat_record(tenant_id);
COMMENT ON COLUMN t_heart_beat_record.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_help_doc
-- ----------------------------
DROP TABLE IF EXISTS t_help_doc;
CREATE TABLE t_help_doc (
  help_doc_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  help_doc_catalog_id BIGINT NOT NULL,
  title varchar(200) NOT NULL,
  content_text text NOT NULL,
  content_html text NOT NULL,
  attachment varchar(1000) NULL DEFAULT NULL,
  sort INTEGER NOT NULL DEFAULT 0,
  page_view_count INTEGER NOT NULL DEFAULT 0,
  user_view_count INTEGER NOT NULL DEFAULT 0,
  author varchar(1000) NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_help_doc_tenant_id ON t_help_doc(tenant_id);
COMMENT ON COLUMN t_help_doc.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_help_doc_catalog
-- ----------------------------
DROP TABLE IF EXISTS t_help_doc_catalog;
CREATE TABLE t_help_doc_catalog (
  help_doc_catalog_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  name varchar(1000) NOT NULL,
  sort INTEGER NOT NULL DEFAULT 0,
  parent_id BIGINT NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_help_doc_catalog_tenant_id ON t_help_doc_catalog(tenant_id);
COMMENT ON COLUMN t_help_doc_catalog.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_help_doc_relation
-- ----------------------------
DROP TABLE IF EXISTS t_help_doc_relation;
CREATE TABLE t_help_doc_relation (
  relation_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  relation_name varchar(255) NULL DEFAULT NULL,
  help_doc_id BIGINT NOT NULL,
  create_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (relation_id, help_doc_id)
);
CREATE INDEX IF NOT EXISTS idx_help_doc_relation_tenant_id ON t_help_doc_relation(tenant_id);
COMMENT ON COLUMN t_help_doc_relation.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_help_doc_view_record (V4 backfill table)
-- ----------------------------
DROP TABLE IF EXISTS t_help_doc_view_record;
CREATE TABLE t_help_doc_view_record (
  help_doc_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_name varchar(255) NULL DEFAULT NULL,
  page_view_count INTEGER NULL DEFAULT 0,
  first_ip varchar(255) NULL DEFAULT NULL,
  first_user_agent varchar(1000) NULL DEFAULT NULL,
  last_ip varchar(255) NULL DEFAULT NULL,
  last_user_agent varchar(1000) NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (help_doc_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_help_doc_view_record_tenant_id ON t_help_doc_view_record(tenant_id);
COMMENT ON COLUMN t_help_doc_view_record.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_login_fail
-- ----------------------------
DROP TABLE IF EXISTS t_login_fail;
CREATE TABLE t_login_fail (
  login_fail_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  user_type INTEGER NOT NULL,
  login_name varchar(1000) NULL DEFAULT NULL,
  login_fail_count INTEGER NULL DEFAULT NULL,
  locked SMALLINT NULL DEFAULT 0,
  login_lock_begin_time TIMESTAMPTZ NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_login_fail_tenant_id ON t_login_fail(tenant_id);
COMMENT ON COLUMN t_login_fail.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_login_log
-- ----------------------------
DROP TABLE IF EXISTS t_login_log;
CREATE TABLE t_login_log (
  login_log_id bigint NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id int NOT NULL,
  user_type int NOT NULL,
  user_name varchar(1000) NOT NULL,
  login_ip varchar(1000) DEFAULT NULL,
  login_ip_region varchar(1000) DEFAULT NULL,
  user_agent text,
  login_device varchar(1000) DEFAULT NULL,
  login_result int NOT NULL,
  remark varchar(2000) DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_login_log_tenant_id ON t_login_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_login_log_user_id ON t_login_log(user_id);
COMMENT ON COLUMN t_login_log.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_mail_template
-- ----------------------------
DROP TABLE IF EXISTS t_mail_template;
CREATE TABLE t_mail_template (
  template_code varchar(200) NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  template_subject varchar(100) NOT NULL,
  template_content TEXT NOT NULL,
  template_type varchar(50) NOT NULL,
  disabled SMALLINT NOT NULL DEFAULT 0,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mail_template_tenant_id ON t_mail_template(tenant_id);
COMMENT ON COLUMN t_mail_template.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS t_menu;
CREATE TABLE t_menu (
  menu_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  menu_name varchar(200) NOT NULL,
  menu_type SMALLINT NOT NULL,
  parent_id BIGINT NOT NULL,
  sort INTEGER NULL DEFAULT NULL,
  path varchar(1000) NULL DEFAULT NULL,
  component varchar(1000) NULL DEFAULT NULL,
  perms_type SMALLINT NULL DEFAULT NULL,
  api_perms text NULL DEFAULT NULL,
  web_perms varchar(1000) NULL DEFAULT NULL,
  icon varchar(1000) NULL DEFAULT NULL,
  context_menu_id BIGINT NULL DEFAULT NULL,
  frame SMALLINT NOT NULL DEFAULT 0,
  frame_url text NULL DEFAULT NULL,
  cache SMALLINT NOT NULL DEFAULT 0,
  visible SMALLINT NOT NULL DEFAULT 1,
  disabled SMALLINT NOT NULL DEFAULT 0,
  deleted SMALLINT NOT NULL DEFAULT 0,
  create_user_id BIGINT NOT NULL,
  create_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  update_user_id BIGINT NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_menu_tenant_id ON t_menu(tenant_id);
COMMENT ON COLUMN t_menu.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS t_message;
CREATE TABLE t_message (
  message_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  message_type SMALLINT NOT NULL,
  receiver_user_type SMALLINT NOT NULL,
  receiver_user_id BIGINT NOT NULL,
  data_id BIGINT NULL DEFAULT NULL,
  title varchar(200) NULL DEFAULT NULL,
  content text NULL DEFAULT NULL,
  read SMALLINT NOT NULL DEFAULT 0,
  read_time TIMESTAMPTZ NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_message_tenant_id ON t_message(tenant_id);
COMMENT ON COLUMN t_message.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_notice
-- ----------------------------
DROP TABLE IF EXISTS t_notice;
CREATE TABLE t_notice (
  notice_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  notice_type_id BIGINT NOT NULL,
  title varchar(200) NULL DEFAULT NULL,
  content_text text NULL DEFAULT NULL,
  content_html text NULL DEFAULT NULL,
  attachment varchar(1000) NULL DEFAULT NULL,
  all_visible SMALLINT NOT NULL DEFAULT 0,
  scheduled_publish SMALLINT NOT NULL DEFAULT 0,
  publish_time TIMESTAMPTZ NULL DEFAULT NULL,
  page_view_count INTEGER NULL DEFAULT NULL,
  user_view_count INTEGER NULL DEFAULT NULL,
  author varchar(1000) NULL DEFAULT NULL,
  source varchar(1000) NULL DEFAULT NULL,
  deleted SMALLINT NOT NULL DEFAULT 0,
  create_user_id BIGINT NOT NULL,
  create_user_name varchar(1000) NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notice_tenant_id ON t_notice(tenant_id);
COMMENT ON COLUMN t_notice.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_notice_type
-- ----------------------------
DROP TABLE IF EXISTS t_notice_type;
CREATE TABLE t_notice_type (
  notice_type_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  notice_type_name varchar(200) NULL DEFAULT NULL,
  sort INTEGER NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notice_type_tenant_id ON t_notice_type(tenant_id);
COMMENT ON COLUMN t_notice_type.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_notice_view_record (V4 backfill table)
-- ----------------------------
DROP TABLE IF EXISTS t_notice_view_record;
CREATE TABLE t_notice_view_record (
  notice_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  page_view_count INTEGER NULL DEFAULT 0,
  first_ip varchar(255) NULL DEFAULT NULL,
  first_user_agent varchar(1000) NULL DEFAULT NULL,
  last_ip varchar(255) NULL DEFAULT NULL,
  last_user_agent varchar(1000) NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notice_id, employee_id)
);
CREATE INDEX IF NOT EXISTS idx_notice_view_record_tenant_id ON t_notice_view_record(tenant_id);
COMMENT ON COLUMN t_notice_view_record.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_notice_visible_range (V4 backfill table)
-- ----------------------------
DROP TABLE IF EXISTS t_notice_visible_range;
CREATE TABLE t_notice_visible_range (
  notice_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  data_type SMALLINT NOT NULL,
  data_id BIGINT NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notice_visible_range_tenant_id ON t_notice_visible_range(tenant_id);
COMMENT ON COLUMN t_notice_visible_range.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_oa_bank
-- ----------------------------
DROP TABLE IF EXISTS t_oa_bank;
CREATE TABLE t_oa_bank (
  bank_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  enterprise_id BIGINT NOT NULL,
  bank_name varchar(1000) NULL DEFAULT NULL,
  account_name varchar(1000) NULL DEFAULT NULL,
  account_number varchar(1000) NULL DEFAULT NULL,
  remark varchar(1000) NULL DEFAULT NULL,
  disabled SMALLINT NOT NULL DEFAULT 0,
  deleted SMALLINT NOT NULL DEFAULT 0,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_oa_bank_tenant_id ON t_oa_bank(tenant_id);
COMMENT ON COLUMN t_oa_bank.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_oa_enterprise
-- ----------------------------
DROP TABLE IF EXISTS t_oa_enterprise;
CREATE TABLE t_oa_enterprise (
  enterprise_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  enterprise_name varchar(1000) NOT NULL,
  enterprise_logo varchar(1000) NULL DEFAULT NULL,
  unified_social_credit_code varchar(1000) NULL DEFAULT NULL,
  type SMALLINT NULL DEFAULT NULL,
  province_name varchar(1000) NULL DEFAULT NULL,
  province_code varchar(1000) NULL DEFAULT NULL,
  city_name varchar(1000) NULL DEFAULT NULL,
  city_code varchar(1000) NULL DEFAULT NULL,
  district_name varchar(1000) NULL DEFAULT NULL,
  district_code varchar(1000) NULL DEFAULT NULL,
  address varchar(1000) NULL DEFAULT NULL,
  contact varchar(1000) NULL DEFAULT NULL,
  contact_phone varchar(1000) NULL DEFAULT NULL,
  email varchar(1000) NULL DEFAULT NULL,
  business_license varchar(1000) NULL DEFAULT NULL,
  disabled SMALLINT NOT NULL DEFAULT 0,
  deleted SMALLINT NOT NULL DEFAULT 0,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_oa_enterprise_tenant_id ON t_oa_enterprise(tenant_id);
COMMENT ON COLUMN t_oa_enterprise.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_oa_enterprise_employee
-- ----------------------------
DROP TABLE IF EXISTS t_oa_enterprise_employee;
CREATE TABLE t_oa_enterprise_employee (
  enterprise_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (enterprise_id, employee_id)
);
CREATE INDEX IF NOT EXISTS idx_oa_enterprise_employee_tenant_id ON t_oa_enterprise_employee(tenant_id);
COMMENT ON COLUMN t_oa_enterprise_employee.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_oa_invoice
-- ----------------------------
DROP TABLE IF EXISTS t_oa_invoice;
CREATE TABLE t_oa_invoice (
  invoice_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  enterprise_id BIGINT NOT NULL,
  invoice_heads varchar(1000) NULL DEFAULT NULL,
  taxpayer_identification_number varchar(1000) NULL DEFAULT NULL,
  bank_name varchar(1000) NULL DEFAULT NULL,
  bank_account varchar(1000) NULL DEFAULT NULL,
  remark varchar(1000) NULL DEFAULT NULL,
  disabled SMALLINT NOT NULL DEFAULT 0,
  deleted SMALLINT NOT NULL DEFAULT 0,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_oa_invoice_tenant_id ON t_oa_invoice(tenant_id);
COMMENT ON COLUMN t_oa_invoice.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_operate_log
-- ----------------------------
DROP TABLE IF EXISTS t_operate_log;
CREATE TABLE t_operate_log (
  operate_log_id bigint NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id bigint NOT NULL,
  user_type int NOT NULL,
  user_name varchar(1000) NOT NULL,
  module varchar(1000) DEFAULT NULL,
  content varchar(1000) DEFAULT NULL,
  url text,
  method varchar(1000) DEFAULT NULL,
  param text,
  fail_reason text,
  operate_ip varchar(1000) DEFAULT NULL,
  operate_ip_region varchar(1000) DEFAULT NULL,
  user_agent text,
  success SMALLINT NOT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_operate_log_tenant_id ON t_operate_log(tenant_id);
COMMENT ON COLUMN t_operate_log.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_password_log
-- ----------------------------
DROP TABLE IF EXISTS t_password_log;
CREATE TABLE t_password_log (
  password_log_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  user_type SMALLINT NOT NULL,
  old_password varchar(100) NOT NULL,
  new_password varchar(100) NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_password_log_tenant_id ON t_password_log(tenant_id);
COMMENT ON COLUMN t_password_log.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_position
-- ----------------------------
DROP TABLE IF EXISTS t_position;
CREATE TABLE t_position (
  position_id bigint NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  position_name varchar(200) NOT NULL,
  level varchar(50) NOT NULL,
  sort int NOT NULL,
  remark varchar(1000) DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_position_tenant_id ON t_position(tenant_id);
COMMENT ON COLUMN t_position.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_reload_item
-- ----------------------------
DROP TABLE IF EXISTS t_reload_item;
CREATE TABLE t_reload_item (
  tag varchar(1000) NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  args varchar(1000) NULL DEFAULT NULL,
  identification varchar(1000) NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reload_item_tenant_id ON t_reload_item(tenant_id);
COMMENT ON COLUMN t_reload_item.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_reload_result
-- ----------------------------
DROP TABLE IF EXISTS t_reload_result;
CREATE TABLE t_reload_result (
  tag varchar(1000) NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  identification varchar(1000) NULL DEFAULT NULL,
  result SMALLINT NOT NULL,
  exception text NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reload_result_tenant_id ON t_reload_result(tenant_id);
COMMENT ON COLUMN t_reload_result.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
  role_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  role_name varchar(20) NOT NULL,
  role_code varchar(1000) NOT NULL,
  remark varchar(255) NULL DEFAULT NULL,
  deleted SMALLINT NOT NULL DEFAULT 0,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_role_tenant_id ON t_role(tenant_id);
COMMENT ON COLUMN t_role.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_role_data_scope
-- ----------------------------
DROP TABLE IF EXISTS t_role_data_scope;
CREATE TABLE t_role_data_scope (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  data_scope_type INTEGER NOT NULL,
  view_type INTEGER NOT NULL,
  role_id BIGINT NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_role_data_scope_tenant_id ON t_role_data_scope(tenant_id);
COMMENT ON COLUMN t_role_data_scope.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_role_employee
-- ----------------------------
DROP TABLE IF EXISTS t_role_employee;
CREATE TABLE t_role_employee (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  role_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_role_employee_tenant_id ON t_role_employee(tenant_id);
COMMENT ON COLUMN t_role_employee.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS t_role_menu;
CREATE TABLE t_role_menu (
  role_menu_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_role_menu_tenant_id ON t_role_menu(tenant_id);
COMMENT ON COLUMN t_role_menu.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_serial_number
-- ----------------------------
DROP TABLE IF EXISTS t_serial_number;
CREATE TABLE t_serial_number (
  serial_number_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  business_name varchar(1000) NOT NULL,
  format varchar(1000) NOT NULL,
  rule_type varchar(1000) NOT NULL,
  init_number INTEGER NOT NULL,
  step_random_range INTEGER NOT NULL,
  remark varchar(1000) NULL DEFAULT NULL,
  last_number BIGINT NOT NULL,
  last_time TIMESTAMPTZ NULL DEFAULT NULL,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_serial_number_tenant_id ON t_serial_number(tenant_id);
COMMENT ON COLUMN t_serial_number.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_serial_number_record
-- ----------------------------
DROP TABLE IF EXISTS t_serial_number_record;
CREATE TABLE t_serial_number_record (
  serial_number_record_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  serial_number_id BIGINT NOT NULL,
  record_date date NOT NULL,
  last_number BIGINT NOT NULL,
  last_time TIMESTAMPTZ NULL DEFAULT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_serial_number_record_tenant_id ON t_serial_number_record(tenant_id);
COMMENT ON COLUMN t_serial_number_record.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_smart_job
-- ----------------------------
DROP TABLE IF EXISTS t_smart_job;
CREATE TABLE t_smart_job (
  job_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  job_name varchar(50) NOT NULL,
  job_class varchar(200) NOT NULL,
  trigger_type varchar(30) NOT NULL,
  trigger_value varchar(200) NOT NULL,
  enabled SMALLINT NOT NULL DEFAULT 0,
  param varchar(1000) NULL DEFAULT NULL,
  last_execute_time TIMESTAMPTZ NULL DEFAULT NULL,
  last_execute_log_id INTEGER NULL DEFAULT NULL,
  sort INTEGER NOT NULL DEFAULT 0,
  remark varchar(255) NULL DEFAULT NULL,
  deleted SMALLINT NOT NULL DEFAULT 0,
  update_name varchar(50) NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_smart_job_tenant_id ON t_smart_job(tenant_id);
COMMENT ON COLUMN t_smart_job.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_smart_job_log
-- ----------------------------
DROP TABLE IF EXISTS t_smart_job_log;
CREATE TABLE t_smart_job_log (
  log_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  job_id INTEGER NOT NULL,
  job_name varchar(100) NOT NULL,
  param varchar(2000) NULL DEFAULT NULL,
  success SMALLINT NOT NULL,
  execute_start_time TIMESTAMPTZ NOT NULL,
  execute_time_millis INTEGER NULL DEFAULT NULL,
  execute_end_time TIMESTAMPTZ NULL DEFAULT NULL,
  execute_result varchar(2000) NULL DEFAULT NULL,
  ip varchar(50) NOT NULL,
  process_id varchar(50) NOT NULL,
  program_path varchar(255) NOT NULL,
  create_name varchar(100) NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_smart_job_log_tenant_id ON t_smart_job_log(tenant_id);
COMMENT ON COLUMN t_smart_job_log.tenant_id IS '租戶ID';

-- ----------------------------
-- Table structure for t_table_column
-- ----------------------------
DROP TABLE IF EXISTS t_table_column;
CREATE TABLE t_table_column (
  table_column_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  user_type INTEGER NOT NULL,
  table_id INTEGER NOT NULL,
  column_json text NOT NULL,
  create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_table_column_tenant_id ON t_table_column(tenant_id);
COMMENT ON COLUMN t_table_column.tenant_id IS '租戶ID';

-- =====================================================================
-- PART B: Unique Constraints (from V4__unique_constraint_tenant.sql)
-- =====================================================================
-- Reforms 11 UNIQUE constraints from global uniqueness to per-tenant
-- uniqueness by adding tenant_id as leading column.
-- =====================================================================

-- 1. t_change_log: version_unique (tenant_id, update_version)
ALTER TABLE t_change_log ADD CONSTRAINT version_unique UNIQUE (tenant_id, update_version);

-- 2. t_dict: unique_code (tenant_id, dict_code)
ALTER TABLE t_dict ADD CONSTRAINT unique_code UNIQUE (tenant_id, dict_code);

-- 3. t_employee: employee_uid_index (tenant_id, employee_uid)
ALTER TABLE t_employee ADD CONSTRAINT employee_uid_index UNIQUE (tenant_id, employee_uid);

-- 4. t_file: uk_file_key (tenant_id, file_key)
ALTER TABLE t_file ADD CONSTRAINT uk_file_key UNIQUE (tenant_id, file_key);

-- 5. t_login_fail: uid_and_utype (tenant_id, user_id, user_type)
ALTER TABLE t_login_fail ADD CONSTRAINT uid_and_utype UNIQUE (tenant_id, user_id, user_type);

-- 6. t_notice_visible_range: uk_notice_data (tenant_id, notice_id, data_type, data_id)
ALTER TABLE t_notice_visible_range ADD CONSTRAINT uk_notice_data UNIQUE (tenant_id, notice_id, data_type, data_id);

-- 7. t_oa_enterprise_employee: uk_enterprise_employee (tenant_id, enterprise_id, employee_id)
ALTER TABLE t_oa_enterprise_employee ADD CONSTRAINT uk_enterprise_employee UNIQUE (tenant_id, enterprise_id, employee_id);

-- 8. t_role: role_code_uni (tenant_id, role_code)
ALTER TABLE t_role ADD CONSTRAINT role_code_uni UNIQUE (tenant_id, role_code);

-- 9. t_role_employee: uk_role_employee (tenant_id, role_id, employee_id)
ALTER TABLE t_role_employee ADD CONSTRAINT uk_role_employee UNIQUE (tenant_id, role_id, employee_id);

-- 10. t_serial_number: key_name (tenant_id, business_name)
ALTER TABLE t_serial_number ADD CONSTRAINT key_name UNIQUE (tenant_id, business_name);

-- 11. t_table_column: uni_employee_table (tenant_id, user_id, table_id)
ALTER TABLE t_table_column ADD CONSTRAINT uni_employee_table UNIQUE (tenant_id, user_id, table_id);

-- =====================================================================
-- PART C: Drop Redundant tenant_id Indexes
-- =====================================================================
-- The composite UNIQUE (tenant_id, ...) serves as an index with
-- tenant_id as leading column, making simple tenant_id indexes redundant.
-- =====================================================================

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
DROP INDEX IF EXISTS idx_notice_visible_range_tenant_id;

-- =====================================================================
-- PART D: Row-Level Security (from V5__rls_policies.sql)
-- =====================================================================
-- Implements dual-layer tenant protection:
--   Layer 1: MyBatis-Plus TenantLineInnerInterceptor (app layer)
--   Layer 2: PostgreSQL RLS (database layer, defense-in-depth)
-- =====================================================================

-- Create Application Role (Non-Superuser)
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartadmin_app') THEN
    CREATE ROLE smartadmin_app LOGIN PASSWORD 'changeme_in_production';
  END IF;
END $$;

-- Grant DML Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO smartadmin_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO smartadmin_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO smartadmin_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO smartadmin_app;

-- Enable RLS on 44 Base Tables
ALTER TABLE t_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_department ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_position ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_role_data_scope ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_goods ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_category ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_invoice ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_bank ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_enterprise ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_oa_enterprise_employee ENABLE ROW LEVEL SECURITY;
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
ALTER TABLE t_reload_result ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_smart_job_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_heart_beat_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice_visible_range ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_notice_view_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_help_doc_view_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_help_doc_relation ENABLE ROW LEVEL SECURITY;

-- Create Tenant Isolation Policies (44 tables)
DROP POLICY IF EXISTS tenant_isolation ON t_employee;
CREATE POLICY tenant_isolation ON t_employee FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_department;
CREATE POLICY tenant_isolation ON t_department FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_position;
CREATE POLICY tenant_isolation ON t_position FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_role;
CREATE POLICY tenant_isolation ON t_role FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_role_employee;
CREATE POLICY tenant_isolation ON t_role_employee FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_role_menu;
CREATE POLICY tenant_isolation ON t_role_menu FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_role_data_scope;
CREATE POLICY tenant_isolation ON t_role_data_scope FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_menu;
CREATE POLICY tenant_isolation ON t_menu FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_goods;
CREATE POLICY tenant_isolation ON t_goods FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_category;
CREATE POLICY tenant_isolation ON t_category FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_oa_invoice;
CREATE POLICY tenant_isolation ON t_oa_invoice FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_notice;
CREATE POLICY tenant_isolation ON t_notice FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_notice_type;
CREATE POLICY tenant_isolation ON t_notice_type FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_oa_bank;
CREATE POLICY tenant_isolation ON t_oa_bank FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_oa_enterprise;
CREATE POLICY tenant_isolation ON t_oa_enterprise FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_oa_enterprise_employee;
CREATE POLICY tenant_isolation ON t_oa_enterprise_employee FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_config;
CREATE POLICY tenant_isolation ON t_config FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_dict;
CREATE POLICY tenant_isolation ON t_dict FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_dict_data;
CREATE POLICY tenant_isolation ON t_dict_data FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_file;
CREATE POLICY tenant_isolation ON t_file FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_feedback;
CREATE POLICY tenant_isolation ON t_feedback FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_help_doc_catalog;
CREATE POLICY tenant_isolation ON t_help_doc_catalog FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_help_doc;
CREATE POLICY tenant_isolation ON t_help_doc FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_message;
CREATE POLICY tenant_isolation ON t_message FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_mail_template;
CREATE POLICY tenant_isolation ON t_mail_template FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_operate_log;
CREATE POLICY tenant_isolation ON t_operate_log FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_login_log;
CREATE POLICY tenant_isolation ON t_login_log FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_login_fail;
CREATE POLICY tenant_isolation ON t_login_fail FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_password_log;
CREATE POLICY tenant_isolation ON t_password_log FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_reload_item;
CREATE POLICY tenant_isolation ON t_reload_item FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_serial_number;
CREATE POLICY tenant_isolation ON t_serial_number FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_serial_number_record;
CREATE POLICY tenant_isolation ON t_serial_number_record FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_table_column;
CREATE POLICY tenant_isolation ON t_table_column FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_change_log;
CREATE POLICY tenant_isolation ON t_change_log FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_code_generator_config;
CREATE POLICY tenant_isolation ON t_code_generator_config FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_data_tracer;
CREATE POLICY tenant_isolation ON t_data_tracer FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_smart_job;
CREATE POLICY tenant_isolation ON t_smart_job FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_reload_result;
CREATE POLICY tenant_isolation ON t_reload_result FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_smart_job_log;
CREATE POLICY tenant_isolation ON t_smart_job_log FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_heart_beat_record;
CREATE POLICY tenant_isolation ON t_heart_beat_record FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_notice_visible_range;
CREATE POLICY tenant_isolation ON t_notice_visible_range FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_notice_view_record;
CREATE POLICY tenant_isolation ON t_notice_view_record FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_help_doc_view_record;
CREATE POLICY tenant_isolation ON t_help_doc_view_record FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_help_doc_relation;
CREATE POLICY tenant_isolation ON t_help_doc_relation FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- PART E: Idempotent Key Table (from V7__idempotent_key_table.sql)
-- =====================================================================
DROP TABLE IF EXISTS t_idempotent_key;
CREATE TABLE t_idempotent_key (
  idempotent_key varchar(200) NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NULL,
  user_type SMALLINT NULL,
  request_time TIMESTAMPTZ NOT NULL,
  expire_time TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_idempotent_key_tenant_id ON t_idempotent_key(tenant_id);
CREATE INDEX IF NOT EXISTS idx_idempotent_key_expire_time ON t_idempotent_key(expire_time);
COMMENT ON TABLE t_idempotent_key IS '事件去重表 - 防止重複提交';
COMMENT ON COLUMN t_idempotent_key.tenant_id IS '租戶ID';

-- Enable RLS on idempotent key table
ALTER TABLE t_idempotent_key ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_idempotent_key;
CREATE POLICY tenant_isolation ON t_idempotent_key FOR ALL TO smartadmin_app
  USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
  WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- PART F: Update Statistics
-- =====================================================================
ANALYZE t_category, t_change_log, t_code_generator_config, t_config,
        t_data_tracer, t_department, t_dict, t_dict_data, t_employee,
        t_feedback, t_file, t_goods, t_heart_beat_record, t_help_doc,
        t_help_doc_catalog, t_help_doc_relation, t_help_doc_view_record,
        t_login_fail, t_login_log, t_mail_template, t_menu, t_message,
        t_notice, t_notice_type, t_notice_view_record, t_notice_visible_range,
        t_oa_bank, t_oa_enterprise, t_oa_enterprise_employee, t_oa_invoice,
        t_operate_log, t_password_log, t_position, t_reload_item, t_reload_result,
        t_role, t_role_data_scope, t_role_employee, t_role_menu,
        t_serial_number, t_serial_number_record, t_smart_job, t_smart_job_log,
        t_table_column, t_idempotent_key;
