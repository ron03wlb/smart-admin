-- Test Data Cleanup Script
-- Removes test data inserted by employees-setup.sql
--
-- Example:
-- @Sql(scripts = "/test-data/cleanup.sql", executionPhase = AFTER_TEST_METHOD)

-- ========================================
-- Delete Test Role Assignments
-- ========================================

DELETE FROM t_role_employee
WHERE employee_id IN (1001, 1002, 1003);

DELETE FROM t_role_employee
WHERE role_id IN (90, 91);

-- ========================================
-- Delete Test Employees
-- ========================================

DELETE FROM t_employee
WHERE employee_id >= 1001 AND employee_id <= 1099;

-- Or delete by login name pattern
DELETE FROM t_employee
WHERE login_name LIKE 'test_user%';

-- ========================================
-- Delete Test Roles
-- ========================================

DELETE FROM t_role
WHERE role_id IN (90, 91);

-- ========================================
-- Delete Test Department
-- ========================================

DELETE FROM t_department
WHERE department_id = 999;

-- ========================================
-- Reset Sequences (Optional)
-- ========================================

-- If using PostgreSQL sequences, you may want to reset them
-- Note: Only do this if needed, as it can cause ID conflicts

-- SELECT setval('t_employee_employee_id_seq',
--     COALESCE((SELECT MAX(employee_id) FROM t_employee WHERE employee_id < 1000), 1),
--     true);

-- ========================================
-- Notes
-- ========================================

-- 1. Delete in reverse order of foreign key dependencies
-- 2. Use ID ranges or patterns to avoid deleting production data
-- 3. This script should mirror the setup script
-- 4. Consider using @Transactional on test class for automatic rollback instead
-- 5. Manual cleanup is useful when @Transactional is not used
