-- ====================================================================
-- SmartAdmin 命名標準統一：t_goods → t_good
-- Version: V1.2
-- Date: 2026-02-02
-- Description: 修正 goods 表命名為單數形式，遵循 SmartAdmin 官方標準
-- Reference: ADR-001-Naming-Convention-Singular-Standard.md
-- ====================================================================

-- ====================================================================
-- Step 1: Rename table
-- ====================================================================
ALTER TABLE t_goods RENAME TO t_good;

-- ====================================================================
-- Step 2: Update sequence (if using PostgreSQL)
-- ====================================================================
-- Note: Adjust sequence name according to your actual database schema
-- For PostgreSQL:
-- ALTER SEQUENCE IF EXISTS t_goods_goods_id_seq RENAME TO t_good_good_id_seq;

-- For MySQL (AUTO_INCREMENT is automatically handled by ALTER TABLE)
-- No additional action needed

-- ====================================================================
-- Step 3: Rename indexes
-- ====================================================================
-- Note: Adjust index names according to your actual schema
-- Example:
-- ALTER INDEX IF EXISTS idx_goods_category_id RENAME TO idx_good_category_id;
-- ALTER INDEX IF EXISTS idx_goods_status RENAME TO idx_good_status;
-- ALTER INDEX IF EXISTS idx_goods_deleted RENAME TO idx_good_deleted;

-- ====================================================================
-- Step 4: Rename foreign key constraints (if any)
-- ====================================================================
-- Note: Update foreign keys in related tables if they reference t_goods
-- Example:
-- ALTER TABLE t_order_item
--     DROP CONSTRAINT IF EXISTS fk_order_item_goods,
--     ADD CONSTRAINT fk_order_item_good
--         FOREIGN KEY (good_id) REFERENCES t_good(good_id);

-- ====================================================================
-- Step 5: Update table comments
-- ====================================================================
-- For MySQL:
ALTER TABLE t_good COMMENT = '商品表（修正為單數形式，遵循 SmartAdmin 標準）';

-- For PostgreSQL:
-- COMMENT ON TABLE t_good IS '商品表（修正為單數形式，遵循 SmartAdmin 標準）';

-- ====================================================================
-- Verification Queries (for manual testing)
-- ====================================================================
-- 1. Check table exists:
-- SHOW TABLES LIKE 't_good';  -- MySQL
-- SELECT * FROM information_schema.tables WHERE table_name = 't_good';  -- PostgreSQL

-- 2. Verify data integrity:
-- SELECT COUNT(*) FROM t_good;

-- 3. Check indexes:
-- SHOW INDEX FROM t_good;  -- MySQL
-- SELECT * FROM pg_indexes WHERE tablename = 't_good';  -- PostgreSQL

-- ====================================================================
-- Rollback script (for emergency use only)
-- ====================================================================
-- IMPORTANT: Keep this commented out. Only execute in emergency rollback scenario.
--
-- -- Step 1: Rename table back
-- ALTER TABLE t_good RENAME TO t_goods;
--
-- -- Step 2: Rename sequence back (PostgreSQL)
-- -- ALTER SEQUENCE IF EXISTS t_good_good_id_seq RENAME TO t_goods_goods_id_seq;
--
-- -- Step 3: Rename indexes back
-- -- ALTER INDEX IF EXISTS idx_good_category_id RENAME TO idx_goods_category_id;
-- -- ALTER INDEX IF EXISTS idx_good_status RENAME TO idx_goods_status;
--
-- -- Step 4: Restore table comments
-- -- ALTER TABLE t_goods COMMENT = '商品表';
-- -- COMMENT ON TABLE t_goods IS '商品表';  -- PostgreSQL

-- ====================================================================
-- Migration Notes
-- ====================================================================
-- 1. This migration aligns with SmartAdmin naming standard (singular form)
-- 2. Entity class GoodsEntity is updated to @TableName("t_good")
-- 3. Mapper XML files are updated to reference t_good
-- 4. Exception: t_liteflow_execution_metrics remains plural (metrics is uncountable)
-- 5. Test in development environment before production deployment
-- 6. Backup production database before applying this migration

-- ====================================================================
-- End of migration script
-- ====================================================================
