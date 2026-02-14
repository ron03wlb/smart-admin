-- SmartAdmin PostgreSQL 初始化脚本
-- 创建扩展、函数和基础配置

-- ============================================
-- 1. 创建常用扩展
-- ============================================

-- UUID 生成扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 模糊搜索扩展（用于 LIKE '%keyword%' 优化）
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- GIN 索引增强（支持更多数据类型）
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- 加密函数扩展
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- 2. 创建全局函数
-- ============================================

-- 自动更新 updated_at 字段的触发器函数
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_modified_column() IS '自动更新 updated_at 字段为当前时间';

-- 软删除触发器函数
CREATE OR REPLACE FUNCTION soft_delete_trigger()
RETURNS TRIGGER AS $$
BEGIN
    NEW.deleted_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION soft_delete_trigger() IS '软删除时自动设置 deleted_at 字段';

-- ============================================
-- 3. 设置默认配置
-- ============================================

-- 设置时区为 Asia/Shanghai
SET timezone = 'Asia/Shanghai';

-- 设置默认编码
SET client_encoding = 'UTF8';

-- ============================================
-- 4. 创建自定义类型（可选）
-- ============================================

-- 审计日志类型
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'audit_operation') THEN
        CREATE TYPE audit_operation AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'READ');
    END IF;
END
$$;

-- ============================================
-- 5. 性能优化配置
-- ============================================

-- 针对当前数据库的配置
ALTER DATABASE smart_admin_v3 SET random_page_cost = 1.1;
ALTER DATABASE smart_admin_v3 SET effective_cache_size = '4GB';
ALTER DATABASE smart_admin_v3 SET shared_buffers = '1GB';
ALTER DATABASE smart_admin_v3 SET work_mem = '16MB';
ALTER DATABASE smart_admin_v3 SET maintenance_work_mem = '256MB';

-- ============================================
-- 6. 创建示例表（可选，供参考）
-- ============================================

-- 示例：用户表
CREATE TABLE IF NOT EXISTS t_user_example (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash TEXT NOT NULL,

    -- 扩展字段（JSONB）
    metadata JSONB DEFAULT '{}'::jsonb,

    -- 审计字段
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_username UNIQUE (username)
);

-- 创建自动更新触发器
CREATE TRIGGER update_user_modtime
    BEFORE UPDATE ON t_user_example
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- 创建索引
CREATE INDEX idx_user_email ON t_user_example(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_created ON t_user_example(created_at DESC);
CREATE INDEX idx_user_metadata ON t_user_example USING GIN(metadata);

COMMENT ON TABLE t_user_example IS '用户表示例（仅供参考）';

-- ============================================
-- 7. 初始化完成日志
-- ============================================

DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'SmartAdmin PostgreSQL 初始化完成';
    RAISE NOTICE '数据库: smart_admin_v3';
    RAISE NOTICE '时区: Asia/Shanghai';
    RAISE NOTICE '扩展: uuid-ossp, pg_trgm, btree_gin, pgcrypto';
    RAISE NOTICE '============================================';
END
$$;
