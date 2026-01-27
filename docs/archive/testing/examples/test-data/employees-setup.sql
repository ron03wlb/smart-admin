-- Test Data Setup for Employee Integration Tests
-- Use with @Sql annotation in integration tests
--
-- Example:
-- @Sql(scripts = "/test-data/employees-setup.sql", executionPhase = BEFORE_TEST_METHOD)
-- @Sql(scripts = "/test-data/cleanup.sql", executionPhase = AFTER_TEST_METHOD)

-- ========================================
-- Insert Test Department
-- ========================================

INSERT INTO t_department (department_id, department_name, manager_id, parent_id, sort, deleted_flag, create_time, update_time)
VALUES (999, 'Test Department', NULL, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (department_id) DO NOTHING;

-- ========================================
-- Insert Test Roles
-- ========================================

INSERT INTO t_role (role_id, role_name, role_code, remark, deleted_flag, create_time, update_time)
VALUES (90, 'Test Role 1', 'test_role_1', 'Test role for integration tests', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO t_role (role_id, role_name, role_code, remark, deleted_flag, create_time, update_time)
VALUES (91, 'Test Role 2', 'test_role_2', 'Test role for integration tests', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (role_id) DO NOTHING;

-- ========================================
-- Insert Test Employees
-- ========================================

-- Test Employee 1 (with permissions)
INSERT INTO t_employee (
    employee_id, login_name, login_pwd, actual_name, gender, phone,
    department_id, disabled_flag, deleted_flag, administrator_flag, remark,
    create_time, update_time
)
VALUES (
    1001, 'test_user1', '$2a$10$encrypted_password_here', 'Test User 1', 1, '13800138001',
    999, 0, 0, 0, 'Integration test user 1',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (employee_id) DO NOTHING;

-- Test Employee 2
INSERT INTO t_employee (
    employee_id, login_name, login_pwd, actual_name, gender, phone,
    department_id, disabled_flag, deleted_flag, administrator_flag, remark,
    create_time, update_time
)
VALUES (
    1002, 'test_user2', '$2a$10$encrypted_password_here', 'Test User 2', 2, '13800138002',
    999, 0, 0, 0, 'Integration test user 2',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (employee_id) DO NOTHING;

-- Test Employee 3 (disabled)
INSERT INTO t_employee (
    employee_id, login_name, login_pwd, actual_name, gender, phone,
    department_id, disabled_flag, deleted_flag, administrator_flag, remark,
    create_time, update_time
)
VALUES (
    1003, 'test_user3_disabled', '$2a$10$encrypted_password_here', 'Test User 3 Disabled', 1, '13800138003',
    999, 1, 0, 0, 'Disabled test user',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (employee_id) DO NOTHING;

-- ========================================
-- Insert Test Role Assignments
-- ========================================

INSERT INTO t_role_employee (role_id, employee_id, create_time, update_time)
VALUES (90, 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO t_role_employee (role_id, employee_id, create_time, update_time)
VALUES (91, 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO t_role_employee (role_id, employee_id, create_time, update_time)
VALUES (90, 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ========================================
-- Insert Test Permissions (if needed)
-- ========================================

-- Add menu/permission mappings for test roles
-- This depends on your permission structure
-- Example:
-- INSERT INTO t_role_menu (role_id, menu_id, create_time, update_time)
-- VALUES (90, <menu_id_for_employee_add>, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ========================================
-- Notes
-- ========================================

-- 1. Adjust IDs to avoid conflicts with production data
-- 2. Use high IDs (900+, 1000+) for test data
-- 3. Password hashes should be replaced with valid bcrypt hashes if testing login
-- 4. ON CONFLICT clauses prevent duplicate key errors if run multiple times
-- 5. This script is idempotent - safe to run multiple times
