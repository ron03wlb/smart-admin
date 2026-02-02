-- ============================================================================
-- LiteFlow Workflow Engine Module - Database Migration Script
-- Version: 1.1
-- Description: Create 4 core tables for LiteFlow workflow engine
-- Author: SmartAdmin Team
-- Date: 2026-02-02
-- ============================================================================

-- ============================================================================
-- Table 1: t_liteflow_chain - Workflow Chain Definitions
-- ============================================================================
CREATE TABLE IF NOT EXISTS t_liteflow_chain (
    chain_id BIGSERIAL PRIMARY KEY,
    chain_name VARCHAR(200) NOT NULL,
    chain_code VARCHAR(100) NOT NULL,
    chain_type INTEGER NOT NULL DEFAULT 1,  -- 1-普通, 2-條件, 3-循環
    chain_data TEXT NOT NULL,               -- EL 表達式，如: THEN(a, b, c)
    version INTEGER NOT NULL DEFAULT 1,     -- 版本號（更新時遞增）
    status INTEGER NOT NULL DEFAULT 1,      -- 0-禁用, 1-啟用
    deleted_flag INTEGER NOT NULL DEFAULT 0, -- 0-未刪除, 1-已刪除
    remark VARCHAR(500),
    create_user_id BIGINT NOT NULL,
    create_user_name VARCHAR(50) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id BIGINT,
    update_user_name VARCHAR(50),
    update_time TIMESTAMP
);

-- Indexes for t_liteflow_chain
CREATE UNIQUE INDEX IF NOT EXISTS uk_chain_code
ON t_liteflow_chain(chain_code)
WHERE deleted_flag = 0;

CREATE INDEX IF NOT EXISTS idx_chain_status
ON t_liteflow_chain(status)
WHERE deleted_flag = 0;

-- Comments for t_liteflow_chain
COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow流程定義表';
COMMENT ON COLUMN t_liteflow_chain.chain_id IS '流程ID（主鍵）';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS '流程名稱';
COMMENT ON COLUMN t_liteflow_chain.chain_code IS '流程編碼（唯一）';
COMMENT ON COLUMN t_liteflow_chain.chain_type IS '流程類型：1-普通 2-條件 3-循環';
COMMENT ON COLUMN t_liteflow_chain.chain_data IS 'EL表達式定義，如: THEN(a, b, c)';
COMMENT ON COLUMN t_liteflow_chain.version IS '版本號（更新時自動遞增）';
COMMENT ON COLUMN t_liteflow_chain.status IS '狀態：0-禁用 1-啟用';
COMMENT ON COLUMN t_liteflow_chain.deleted_flag IS '刪除標記：0-未刪除 1-已刪除';

-- ============================================================================
-- Table 2: t_liteflow_script - Script Node Definitions
-- ============================================================================
CREATE TABLE IF NOT EXISTS t_liteflow_script (
    script_id BIGSERIAL PRIMARY KEY,
    script_name VARCHAR(200) NOT NULL,
    script_code VARCHAR(100) NOT NULL,
    script_type VARCHAR(50) NOT NULL DEFAULT 'qlexpress',  -- qlexpress/groovy/javascript
    script_data TEXT NOT NULL,                             -- 腳本內容/代碼
    version INTEGER NOT NULL DEFAULT 1,                    -- 版本號
    status INTEGER NOT NULL DEFAULT 1,                     -- 0-禁用, 1-啟用
    deleted_flag INTEGER NOT NULL DEFAULT 0,               -- 0-未刪除, 1-已刪除
    remark VARCHAR(500),
    create_user_id BIGINT NOT NULL,
    create_user_name VARCHAR(50) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id BIGINT,
    update_user_name VARCHAR(50),
    update_time TIMESTAMP
);

-- Indexes for t_liteflow_script
CREATE UNIQUE INDEX IF NOT EXISTS uk_script_code
ON t_liteflow_script(script_code)
WHERE deleted_flag = 0;

CREATE INDEX IF NOT EXISTS idx_script_type
ON t_liteflow_script(script_type)
WHERE deleted_flag = 0;

-- Comments for t_liteflow_script
COMMENT ON TABLE t_liteflow_script IS 'LiteFlow腳本節點定義表';
COMMENT ON COLUMN t_liteflow_script.script_id IS '腳本ID（主鍵）';
COMMENT ON COLUMN t_liteflow_script.script_name IS '腳本名稱';
COMMENT ON COLUMN t_liteflow_script.script_code IS '腳本編碼（唯一）';
COMMENT ON COLUMN t_liteflow_script.script_type IS '腳本類型：qlexpress/groovy/javascript';
COMMENT ON COLUMN t_liteflow_script.script_data IS '腳本內容（代碼）';
COMMENT ON COLUMN t_liteflow_script.version IS '版本號（更新時自動遞增）';
COMMENT ON COLUMN t_liteflow_script.status IS '狀態：0-禁用 1-啟用';
COMMENT ON COLUMN t_liteflow_script.deleted_flag IS '刪除標記：0-未刪除 1-已刪除';

-- ============================================================================
-- Table 3: t_liteflow_execution_log - Execution Log Records
-- ============================================================================
CREATE TABLE IF NOT EXISTS t_liteflow_execution_log (
    log_id BIGSERIAL PRIMARY KEY,
    chain_code VARCHAR(100) NOT NULL,
    request_id VARCHAR(100),                         -- 請求追蹤ID（用於關聯多個節點執行）
    execution_status INTEGER NOT NULL,               -- 0-失敗, 1-成功
    execution_time INTEGER NOT NULL,                 -- 執行時長（毫秒）
    input_params TEXT,                               -- 輸入參數（JSON格式）
    output_result TEXT,                              -- 輸出結果（JSON格式）
    error_message TEXT,                              -- 錯誤信息
    error_stack TEXT,                                -- 錯誤堆棧
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for t_liteflow_execution_log
CREATE INDEX IF NOT EXISTS idx_log_chain_code
ON t_liteflow_execution_log(chain_code);

CREATE INDEX IF NOT EXISTS idx_log_status
ON t_liteflow_execution_log(execution_status);

CREATE INDEX IF NOT EXISTS idx_log_create_time
ON t_liteflow_execution_log(create_time);

CREATE INDEX IF NOT EXISTS idx_log_request_id
ON t_liteflow_execution_log(request_id);

-- Comments for t_liteflow_execution_log
COMMENT ON TABLE t_liteflow_execution_log IS 'LiteFlow執行日誌表';
COMMENT ON COLUMN t_liteflow_execution_log.log_id IS '日誌ID（主鍵）';
COMMENT ON COLUMN t_liteflow_execution_log.chain_code IS '執行的流程編碼';
COMMENT ON COLUMN t_liteflow_execution_log.request_id IS '請求追蹤ID（UUID）';
COMMENT ON COLUMN t_liteflow_execution_log.execution_status IS '執行狀態：0-失敗 1-成功';
COMMENT ON COLUMN t_liteflow_execution_log.execution_time IS '執行時長（毫秒）';
COMMENT ON COLUMN t_liteflow_execution_log.input_params IS '輸入參數（JSON格式）';
COMMENT ON COLUMN t_liteflow_execution_log.output_result IS '輸出結果（JSON格式）';
COMMENT ON COLUMN t_liteflow_execution_log.error_message IS '錯誤信息';
COMMENT ON COLUMN t_liteflow_execution_log.error_stack IS '錯誤堆棧（完整異常信息）';

-- ============================================================================
-- Table 4: t_liteflow_execution_metrics - Daily Aggregated Metrics
-- ============================================================================
CREATE TABLE IF NOT EXISTS t_liteflow_execution_metrics (
    metric_id BIGSERIAL PRIMARY KEY,
    chain_code VARCHAR(100) NOT NULL,
    metric_date DATE NOT NULL,                       -- 統計日期
    total_count INTEGER NOT NULL DEFAULT 0,          -- 執行總數
    success_count INTEGER NOT NULL DEFAULT 0,        -- 成功數
    failure_count INTEGER NOT NULL DEFAULT 0,        -- 失敗數
    avg_execution_time INTEGER NOT NULL DEFAULT 0,   -- 平均執行時間（毫秒）
    max_execution_time INTEGER NOT NULL DEFAULT 0,   -- 最大執行時間（毫秒）
    min_execution_time INTEGER NOT NULL DEFAULT 0,   -- 最小執行時間（毫秒）
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

-- Indexes for t_liteflow_execution_metrics
CREATE UNIQUE INDEX IF NOT EXISTS uk_metrics_chain_date
ON t_liteflow_execution_metrics(chain_code, metric_date);

-- Comments for t_liteflow_execution_metrics
COMMENT ON TABLE t_liteflow_execution_metrics IS 'LiteFlow執行指標聚合表（每日統計）';
COMMENT ON COLUMN t_liteflow_execution_metrics.metric_id IS '指標ID（主鍵）';
COMMENT ON COLUMN t_liteflow_execution_metrics.chain_code IS '流程編碼';
COMMENT ON COLUMN t_liteflow_execution_metrics.metric_date IS '統計日期';
COMMENT ON COLUMN t_liteflow_execution_metrics.total_count IS '執行總數';
COMMENT ON COLUMN t_liteflow_execution_metrics.success_count IS '成功數';
COMMENT ON COLUMN t_liteflow_execution_metrics.failure_count IS '失敗數';
COMMENT ON COLUMN t_liteflow_execution_metrics.avg_execution_time IS '平均執行時間（毫秒）';
COMMENT ON COLUMN t_liteflow_execution_metrics.max_execution_time IS '最大執行時間（毫秒）';
COMMENT ON COLUMN t_liteflow_execution_metrics.min_execution_time IS '最小執行時間（毫秒）';

-- ============================================================================
-- End of Migration Script
-- ============================================================================
