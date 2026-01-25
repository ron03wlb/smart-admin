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
