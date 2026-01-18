-- =============================================
-- Smart Admin 21 - PostgreSQL 初始化腳本
-- =============================================
-- 此腳本在容器首次啟動時自動執行

-- 創建擴展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 設置時區
SET timezone = 'Asia/Taipei';

-- 創建應用用戶（可選，用於更細粒度的權限控制）
-- CREATE USER app_user WITH PASSWORD 'app_password';
-- GRANT CONNECT ON DATABASE smart_admin TO app_user;

-- 輸出初始化完成信息
DO $$
BEGIN
    RAISE NOTICE '=============================================';
    RAISE NOTICE 'Smart Admin 21 - PostgreSQL 初始化完成';
    RAISE NOTICE '數據庫: smart_admin';
    RAISE NOTICE '時區: Asia/Taipei';
    RAISE NOTICE '=============================================';
END $$;
