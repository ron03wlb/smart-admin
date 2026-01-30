-- ============================================================================================================
-- H2 Test Database Schema Initialization
-- ============================================================================================================
-- This file is automatically executed by Spring Boot when running integration tests with H2 in-memory database.
-- H2 runs in PostgreSQL compatibility mode (MODE=PostgreSQL) as configured in test application.yaml
-- ============================================================================================================

-- ============================================================================================================
-- System Module Tables
-- ============================================================================================================

-- Employee Table (t_employee)
CREATE TABLE IF NOT EXISTS t_employee (
    employee_id BIGSERIAL PRIMARY KEY,
    employee_uid VARCHAR(50),
    login_name VARCHAR(50) NOT NULL,
    login_pwd VARCHAR(100),
    actual_name VARCHAR(50),
    avatar VARCHAR(255),
    gender INTEGER,
    phone VARCHAR(20),
    email VARCHAR(100),
    department_id BIGINT,
    position_id BIGINT,
    administrator_flag BOOLEAN DEFAULT FALSE,
    disabled_flag BOOLEAN DEFAULT FALSE,
    deleted_flag BOOLEAN DEFAULT FALSE,
    remark VARCHAR(500),
    update_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_employee_login_name ON t_employee(login_name);
CREATE INDEX IF NOT EXISTS idx_employee_phone ON t_employee(phone);
CREATE INDEX IF NOT EXISTS idx_employee_deleted_flag ON t_employee(deleted_flag);

COMMENT ON TABLE t_employee IS '員工資訊表 (Employee Information)';
COMMENT ON COLUMN t_employee.employee_id IS '員工ID (主鍵, Primary Key)';
COMMENT ON COLUMN t_employee.employee_uid IS '唯一識別碼 (Unique Identifier)';
COMMENT ON COLUMN t_employee.login_name IS '登入帳號 (Login Username)';
COMMENT ON COLUMN t_employee.login_pwd IS '登入密碼 (Encrypted Password)';
COMMENT ON COLUMN t_employee.actual_name IS '真實姓名 (Actual Name)';
COMMENT ON COLUMN t_employee.avatar IS '頭像URL (Avatar URL)';
COMMENT ON COLUMN t_employee.gender IS '性別 (Gender: 1=男性, 2=女性)';
COMMENT ON COLUMN t_employee.phone IS '手機號碼 (Phone Number)';
COMMENT ON COLUMN t_employee.email IS '電子郵件 (Email Address)';
COMMENT ON COLUMN t_employee.department_id IS '部門ID (Department Foreign Key)';
COMMENT ON COLUMN t_employee.position_id IS '職位ID (Position Foreign Key)';
COMMENT ON COLUMN t_employee.administrator_flag IS '超級管理員標誌 (Is Administrator)';
COMMENT ON COLUMN t_employee.disabled_flag IS '禁用標誌 (Is Disabled)';
COMMENT ON COLUMN t_employee.deleted_flag IS '軟刪除標誌 (Soft Delete Flag)';
COMMENT ON COLUMN t_employee.remark IS '備註 (Remark)';
COMMENT ON COLUMN t_employee.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_employee.create_time IS '建立時間 (Create Time)';

-- Department Table (t_department)
CREATE TABLE IF NOT EXISTS t_department (
    department_id BIGSERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    manager_id BIGINT,
    parent_id BIGINT DEFAULT 0,
    sort INTEGER DEFAULT 0,
    deleted_flag BOOLEAN DEFAULT FALSE,
    update_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_department_parent_id ON t_department(parent_id);
CREATE INDEX IF NOT EXISTS idx_department_deleted_flag ON t_department(deleted_flag);

COMMENT ON TABLE t_department IS '部門資訊表 (Department Information)';
COMMENT ON COLUMN t_department.department_id IS '部門ID (主鍵, Primary Key)';
COMMENT ON COLUMN t_department.department_name IS '部門名稱 (Department Name)';
COMMENT ON COLUMN t_department.manager_id IS '部門主管ID (Manager Employee ID)';
COMMENT ON COLUMN t_department.parent_id IS '上級部門ID (Parent Department ID, 0=頂級)';
COMMENT ON COLUMN t_department.sort IS '排序順序 (Display Sort Order)';
COMMENT ON COLUMN t_department.deleted_flag IS '軟刪除標誌 (Soft Delete Flag)';
COMMENT ON COLUMN t_department.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_department.create_time IS '建立時間 (Create Time)';

-- Role Table (t_role)
CREATE TABLE IF NOT EXISTS t_role (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50),
    remark VARCHAR(500),
    deleted_flag BOOLEAN DEFAULT FALSE,
    update_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_role_deleted_flag ON t_role(deleted_flag);

COMMENT ON TABLE t_role IS '角色資訊表 (Role Information)';
COMMENT ON COLUMN t_role.role_id IS '角色ID (主鍵, Primary Key)';
COMMENT ON COLUMN t_role.role_name IS '角色名稱 (Role Name)';
COMMENT ON COLUMN t_role.role_code IS '角色代碼 (Role Code)';
COMMENT ON COLUMN t_role.remark IS '備註 (Remark)';
COMMENT ON COLUMN t_role.deleted_flag IS '軟刪除標誌 (Soft Delete Flag)';
COMMENT ON COLUMN t_role.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_role.create_time IS '建立時間 (Create Time)';

-- Role-Employee Association Table (t_role_employee)
CREATE TABLE IF NOT EXISTS t_role_employee (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    update_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_role_employee_role_id ON t_role_employee(role_id);
CREATE INDEX IF NOT EXISTS idx_role_employee_employee_id ON t_role_employee(employee_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_employee ON t_role_employee(role_id, employee_id);

COMMENT ON TABLE t_role_employee IS '角色員工關聯表 (Role-Employee Association)';
COMMENT ON COLUMN t_role_employee.id IS '主鍵ID (Primary Key)';
COMMENT ON COLUMN t_role_employee.role_id IS '角色ID (Role Foreign Key)';
COMMENT ON COLUMN t_role_employee.employee_id IS '員工ID (Employee Foreign Key)';
COMMENT ON COLUMN t_role_employee.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_role_employee.create_time IS '建立時間 (Create Time)';

-- ============================================================================================================
-- Business Module Tables
-- ============================================================================================================

-- Brand Table (t_brand)
CREATE TABLE IF NOT EXISTS t_brand (
    brand_id BIGSERIAL PRIMARY KEY,
    brand_name VARCHAR(100) NOT NULL,
    brand_logo VARCHAR(255),
    description VARCHAR(500),
    sort INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    deleted_flag BOOLEAN DEFAULT FALSE,
    update_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Note: H2 does not support partial indexes with WHERE clauses
-- For H2 compatibility, we create a unique index without the WHERE clause
-- In production PostgreSQL, use: CREATE UNIQUE INDEX uk_brand_name ON t_brand(brand_name) WHERE deleted_flag = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_name ON t_brand(brand_name);
CREATE INDEX IF NOT EXISTS idx_brand_status ON t_brand(status);
CREATE INDEX IF NOT EXISTS idx_brand_deleted_flag ON t_brand(deleted_flag);
CREATE INDEX IF NOT EXISTS idx_brand_sort ON t_brand(sort);

COMMENT ON TABLE t_brand IS '品牌資訊表 (Brand Information)';
COMMENT ON COLUMN t_brand.brand_id IS '品牌ID (主鍵, Primary Key, Auto-increment)';
COMMENT ON COLUMN t_brand.brand_name IS '品牌名稱 (Brand Name, Unique when not deleted)';
COMMENT ON COLUMN t_brand.brand_logo IS '品牌標誌URL (Brand Logo URL)';
COMMENT ON COLUMN t_brand.description IS '品牌描述 (Brand Description)';
COMMENT ON COLUMN t_brand.sort IS '顯示排序 (Display Sort Order, 數值越小越靠前)';
COMMENT ON COLUMN t_brand.status IS '品牌狀態 (Status: 1=啟用 Enabled, 0=禁用 Disabled)';
COMMENT ON COLUMN t_brand.deleted_flag IS '軟刪除標誌 (Soft Delete Flag: true=已刪除, false=未刪除)';
COMMENT ON COLUMN t_brand.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_brand.create_time IS '建立時間 (Create Time)';

-- ============================================================================================================
-- Support Module Tables
-- ============================================================================================================

-- Config Table (t_config)
CREATE TABLE IF NOT EXISTS t_config (
    config_id BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(255) NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT NOT NULL,
    remark VARCHAR(255),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_config_key ON t_config(config_key);

COMMENT ON TABLE t_config IS '系統配置表 (System Configuration)';
COMMENT ON COLUMN t_config.config_id IS '配置ID (主鍵, Primary Key)';
COMMENT ON COLUMN t_config.config_name IS '參數名稱 (Configuration Name)';
COMMENT ON COLUMN t_config.config_key IS '參數Key (Configuration Key, for lookup)';
COMMENT ON COLUMN t_config.config_value IS '參數值 (Configuration Value)';
COMMENT ON COLUMN t_config.remark IS '備註 (Remark)';
COMMENT ON COLUMN t_config.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_config.create_time IS '建立時間 (Create Time)';

-- ============================================================================================================
-- Initial Data for Support Module
-- ============================================================================================================

-- Config Initial Data
INSERT INTO t_config (config_id, config_name, config_key, config_value, remark, create_time, update_time)
VALUES 
  (1, '萬能密碼', 'super_password', '1024ok', 'For testing only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '三級等保', 'level3_protect_config', 
   '{"fileDetectFlag":true,"loginActiveTimeoutMinutes":30,"loginFailLockMinutes":30,"loginFailMaxTimes":3,"maxUploadFileSizeMb":30,"passwordComplexityEnabled":true,"regularChangePasswordMonths":3,"regularChangePasswordNotAllowRepeatTimes":3,"twoFactorLoginEnabled":false}', 
   'Level 3 security protection configuration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reload Item Table (t_reload_item)
CREATE TABLE IF NOT EXISTS t_reload_item (
    tag VARCHAR(255) PRIMARY KEY,
    args VARCHAR(255),
    identification VARCHAR(255) NOT NULL,
    update_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_reload_item IS 'Reload 項目表 (Reload Item Configuration)';
COMMENT ON COLUMN t_reload_item.tag IS '項目名稱 (Item Tag, Primary Key)';
COMMENT ON COLUMN t_reload_item.args IS '參數 (Arguments, Optional)';
COMMENT ON COLUMN t_reload_item.identification IS '運行標識 (Runtime Identification)';
COMMENT ON COLUMN t_reload_item.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_reload_item.create_time IS '建立時間 (Create Time)';

-- Serial Number Table (t_serial_number)
CREATE TABLE IF NOT EXISTS t_serial_number (
    serial_number_id INTEGER PRIMARY KEY,
    business_name VARCHAR(50) NOT NULL,
    format VARCHAR(50),
    rule_type VARCHAR(20) NOT NULL,
    init_number INTEGER NOT NULL,
    step_random_range INTEGER NOT NULL,
    remark VARCHAR(255),
    last_number BIGINT,
    last_time TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_name ON t_serial_number(business_name);

COMMENT ON TABLE t_serial_number IS '序列號生成器表 (Serial Number Generator)';
COMMENT ON COLUMN t_serial_number.serial_number_id IS '序列號ID (Primary Key)';
COMMENT ON COLUMN t_serial_number.business_name IS '業務名稱 (Business Name, Unique)';
COMMENT ON COLUMN t_serial_number.format IS '格式 ([yyyy]=年,[mm]=月,[dd]=日,[nnn]=三位數字)';
COMMENT ON COLUMN t_serial_number.rule_type IS '規則類型 (none=無周期, year=年, month=月, day=日)';
COMMENT ON COLUMN t_serial_number.init_number IS '初始值 (Initial Number)';
COMMENT ON COLUMN t_serial_number.step_random_range IS '步長隨機數 (Step Random Range)';
COMMENT ON COLUMN t_serial_number.remark IS '備註 (Remark)';
COMMENT ON COLUMN t_serial_number.last_number IS '上次產生的單號 (Last Generated Number)';
COMMENT ON COLUMN t_serial_number.last_time IS '上次產生時間 (Last Generated Time)';
COMMENT ON COLUMN t_serial_number.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_serial_number.create_time IS '建立時間 (Create Time)';

-- Serial Number Record Table (t_serial_number_record)
CREATE TABLE IF NOT EXISTS t_serial_number_record (
    serial_number_id INTEGER NOT NULL,
    record_date DATE NOT NULL,
    last_number BIGINT NOT NULL DEFAULT 0,
    last_time TIMESTAMP NOT NULL,
    count BIGINT NOT NULL DEFAULT 0,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS uk_generator ON t_serial_number_record(serial_number_id, record_date);

COMMENT ON TABLE t_serial_number_record IS '序列號記錄表 (Serial Number Records)';
COMMENT ON COLUMN t_serial_number_record.serial_number_id IS '序列號ID (Foreign Key to t_serial_number)';
COMMENT ON COLUMN t_serial_number_record.record_date IS '記錄日期 (Record Date)';
COMMENT ON COLUMN t_serial_number_record.last_number IS '最後更新值 (Last Updated Number)';
COMMENT ON COLUMN t_serial_number_record.last_time IS '最後更新時間 (Last Updated Time)';
COMMENT ON COLUMN t_serial_number_record.count IS '更新次數 (Update Count)';
COMMENT ON COLUMN t_serial_number_record.update_time IS '更新時間 (Last Update Time)';
COMMENT ON COLUMN t_serial_number_record.create_time IS '建立時間 (Create Time)';

-- Reload Item Initial Data
INSERT INTO t_reload_item (tag, args, identification, create_time, update_time)
VALUES ('system_config', '4', '234', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
