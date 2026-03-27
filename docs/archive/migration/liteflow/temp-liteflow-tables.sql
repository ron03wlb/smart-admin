-- ============================================================================
-- Temporary LiteFlow Tables Creation for v3 → v4 Migration
-- ============================================================================
-- These tables are required by V2__add_tenant_id.sql migration but don't
-- exist in the v3 schema. This script creates them WITHOUT tenant_id column,
-- so that V2 migration can add it properly.
--
-- After running this script, re-enable Flyway and run migrations.
-- ============================================================================

-- t_liteflow_chain
CREATE TABLE IF NOT EXISTS t_liteflow_chain (
  chain_id BIGSERIAL PRIMARY KEY,
  chain_name VARCHAR(100) NOT NULL,
  chain_code VARCHAR(50) NOT NULL UNIQUE,
  chain_type INTEGER DEFAULT 1,
  chain_data TEXT,
  version INTEGER DEFAULT 1,
  status INTEGER DEFAULT 1,
  deleted_flag INTEGER DEFAULT 0,
  remark VARCHAR(500),
  create_user_id BIGINT,
  create_user_name VARCHAR(50),
  update_user_id BIGINT,
  update_user_name VARCHAR(50),
  create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow 流程定義表';
COMMENT ON COLUMN t_liteflow_chain.chain_id IS '流程ID（主鍵）';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS '流程名稱';
COMMENT ON COLUMN t_liteflow_chain.chain_code IS '流程編碼（唯一）';
COMMENT ON COLUMN t_liteflow_chain.chain_type IS '流程類型：1-普通 2-條件 3-循環';
COMMENT ON COLUMN t_liteflow_chain.chain_data IS 'EL 表達式定義，如: THEN(a, b, c)';
COMMENT ON COLUMN t_liteflow_chain.version IS '版本號（更新時自動遞增）';
COMMENT ON COLUMN t_liteflow_chain.status IS '狀態：0-禁用 1-啟用';
COMMENT ON COLUMN t_liteflow_chain.deleted_flag IS '刪除標記：0-未刪除 1-已刪除';

CREATE INDEX IF NOT EXISTS idx_liteflow_chain_code ON t_liteflow_chain(chain_code);
CREATE INDEX IF NOT EXISTS idx_liteflow_chain_status ON t_liteflow_chain(status);

-- t_liteflow_script
CREATE TABLE IF NOT EXISTS t_liteflow_script (
  script_id BIGSERIAL PRIMARY KEY,
  script_name VARCHAR(100) NOT NULL,
  script_code VARCHAR(50) NOT NULL UNIQUE,
  script_type VARCHAR(20) DEFAULT 'qlexpress',
  script_data TEXT,
  version INTEGER DEFAULT 1,
  status INTEGER DEFAULT 1,
  deleted_flag INTEGER DEFAULT 0,
  remark VARCHAR(500),
  create_user_id BIGINT,
  create_user_name VARCHAR(50),
  update_user_id BIGINT,
  update_user_name VARCHAR(50),
  create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_liteflow_script IS 'LiteFlow 腳本節點定義表';
COMMENT ON COLUMN t_liteflow_script.script_id IS '腳本ID（主鍵）';
COMMENT ON COLUMN t_liteflow_script.script_name IS '腳本名稱';
COMMENT ON COLUMN t_liteflow_script.script_code IS '腳本編碼（唯一）';
COMMENT ON COLUMN t_liteflow_script.script_type IS '腳本類型：qlexpress/groovy/javascript';
COMMENT ON COLUMN t_liteflow_script.script_data IS '腳本內容（代碼）';
COMMENT ON COLUMN t_liteflow_script.version IS '版本號（更新時自動遞增）';
COMMENT ON COLUMN t_liteflow_script.status IS '狀態：0-禁用 1-啟用';

CREATE INDEX IF NOT EXISTS idx_liteflow_script_code ON t_liteflow_script(script_code);
CREATE INDEX IF NOT EXISTS idx_liteflow_script_status ON t_liteflow_script(status);

-- t_liteflow_execution_metrics
CREATE TABLE IF NOT EXISTS t_liteflow_execution_metrics (
  metric_id BIGSERIAL PRIMARY KEY,
  chain_code VARCHAR(50) NOT NULL,
  metric_date DATE NOT NULL,
  total_count INTEGER DEFAULT 0,
  success_count INTEGER DEFAULT 0,
  failure_count INTEGER DEFAULT 0,
  avg_execution_time INTEGER,
  max_execution_time INTEGER,
  min_execution_time INTEGER,
  create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_liteflow_execution_metrics IS 'LiteFlow 執行指標聚合表（每日統計）';
COMMENT ON COLUMN t_liteflow_execution_metrics.metric_id IS '指標ID（主鍵）';
COMMENT ON COLUMN t_liteflow_execution_metrics.chain_code IS '流程編碼';
COMMENT ON COLUMN t_liteflow_execution_metrics.metric_date IS '統計日期';
COMMENT ON COLUMN t_liteflow_execution_metrics.total_count IS '執行總數';
COMMENT ON COLUMN t_liteflow_execution_metrics.success_count IS '成功數';
COMMENT ON COLUMN t_liteflow_execution_metrics.failure_count IS '失敗數';
COMMENT ON COLUMN t_liteflow_execution_metrics.avg_execution_time IS '平均執行時間（毫秒）';
COMMENT ON COLUMN t_liteflow_execution_metrics.max_execution_time IS '最大執行時間（毫秒）';
COMMENT ON COLUMN t_liteflow_execution_metrics.min_execution_time IS '最小執行時間（毫秒）';

CREATE INDEX IF NOT EXISTS idx_liteflow_metrics_chain_date ON t_liteflow_execution_metrics(chain_code, metric_date);

-- t_liteflow_execution_log
CREATE TABLE IF NOT EXISTS t_liteflow_execution_log (
  log_id BIGSERIAL PRIMARY KEY,
  chain_code VARCHAR(50) NOT NULL,
  request_id VARCHAR(50),
  execution_status INTEGER NOT NULL,
  execution_time INTEGER,
  input_params TEXT,
  output_result TEXT,
  error_message VARCHAR(500),
  error_stack TEXT,
  create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_liteflow_execution_log IS 'LiteFlow 執行日誌表';
COMMENT ON COLUMN t_liteflow_execution_log.log_id IS '日誌ID（主鍵）';
COMMENT ON COLUMN t_liteflow_execution_log.chain_code IS '執行的流程編碼';
COMMENT ON COLUMN t_liteflow_execution_log.request_id IS '請求追蹤ID（UUID）';
COMMENT ON COLUMN t_liteflow_execution_log.execution_status IS '執行狀態：0-失敗 1-成功';
COMMENT ON COLUMN t_liteflow_execution_log.execution_time IS '執行時長（毫秒）';
COMMENT ON COLUMN t_liteflow_execution_log.input_params IS '輸入參數（JSON格式）';
COMMENT ON COLUMN t_liteflow_execution_log.output_result IS '輸出結果（JSON格式）';
COMMENT ON COLUMN t_liteflow_execution_log.error_message IS '錯誤信息';
COMMENT ON COLUMN t_liteflow_execution_log.error_stack IS '錯誤堆棧（完整異常信息）';

CREATE INDEX IF NOT EXISTS idx_liteflow_log_chain ON t_liteflow_execution_log(chain_code);
CREATE INDEX IF NOT EXISTS idx_liteflow_log_request ON t_liteflow_execution_log(request_id);
CREATE INDEX IF NOT EXISTS idx_liteflow_log_status ON t_liteflow_execution_log(execution_status);
CREATE INDEX IF NOT EXISTS idx_liteflow_log_create_time ON t_liteflow_execution_log(create_time);
