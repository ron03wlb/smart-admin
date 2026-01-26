-- SmartAdmin AI 系統表
-- 實施 P0-3 安全修復：審計日誌 + AI 執行記錄
-- 創建日期: 2026-01-27

-- ============================================
-- 1. AI 操作審計日誌表（P0-3 安全修復）
-- ============================================
CREATE TABLE IF NOT EXISTS t_ai_operation_audit (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(100) NOT NULL,                    -- Agent 名稱（java-architect, postgres-pro）
    operation VARCHAR(50) NOT NULL,                      -- 操作類型（read, write, delete）
    file_path TEXT NOT NULL,                             -- 文件路徑
    decision VARCHAR(20) NOT NULL,                       -- 決策結果（allow, deny, audit）
    denied_reason TEXT,                                  -- 拒絕原因
    success BOOLEAN DEFAULT FALSE,                       -- 操作是否成功執行
    execution_time_ms INT,                               -- 操作執行時間（毫秒）
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引：按 Agent 查詢
CREATE INDEX idx_ai_operation_audit_agent_name ON t_ai_operation_audit(agent_name);

-- 索引：按決策結果查詢（快速找到被拒絕的操作）
CREATE INDEX idx_ai_operation_audit_decision ON t_ai_operation_audit(decision);

-- 索引：按時間查詢
CREATE INDEX idx_ai_operation_audit_created_at ON t_ai_operation_audit(created_at DESC);

-- 註釋
COMMENT ON TABLE t_ai_operation_audit IS 'AI Agent 操作審計日誌（P0-3 安全修復）';
COMMENT ON COLUMN t_ai_operation_audit.agent_name IS 'Agent 名稱';
COMMENT ON COLUMN t_ai_operation_audit.operation IS '操作類型：read, write, delete';
COMMENT ON COLUMN t_ai_operation_audit.file_path IS '被訪問的文件路徑';
COMMENT ON COLUMN t_ai_operation_audit.decision IS '訪問決策：allow, deny, audit';
COMMENT ON COLUMN t_ai_operation_audit.denied_reason IS '拒絕原因（若 decision=deny）';
COMMENT ON COLUMN t_ai_operation_audit.success IS '操作是否成功執行';

-- ============================================
-- 2. AI 執行日誌表（工作流追蹤）
-- ============================================
CREATE TABLE IF NOT EXISTS t_ai_execution_log (
    id BIGSERIAL PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,                 -- 工作流名稱（analyzer/developer/qa）
    trigger_type VARCHAR(50),                            -- 觸發類型（cron/alert/webhook/manual）
    trigger_data JSONB,                                  -- 觸發詳情（JSON）
    crew_name VARCHAR(100),                              -- Crew 名稱（analyzer_crew/developer_crew）
    start_time TIMESTAMP NOT NULL,                       -- 開始時間
    end_time TIMESTAMP,                                  -- 結束時間
    duration_seconds INT,                                -- 執行時長（秒）
    status VARCHAR(20),                                  -- 狀態（success/failed/cancelled）
    error_message TEXT,                                  -- 錯誤信息
    suggestions_generated INT DEFAULT 0,                 -- 生成的優化建議數量
    changes_applied INT DEFAULT 0,                       -- 實際應用的變更數量
    tests_passed BOOLEAN,                                -- 測試是否通過
    deployment_successful BOOLEAN,                       -- 部署是否成功
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引：按工作流名稱查詢
CREATE INDEX idx_ai_execution_log_workflow ON t_ai_execution_log(workflow_name);

-- 索引：按狀態查詢
CREATE INDEX idx_ai_execution_log_status ON t_ai_execution_log(status);

-- 索引：按時間查詢
CREATE INDEX idx_ai_execution_log_created_at ON t_ai_execution_log(created_at DESC);

-- 註釋
COMMENT ON TABLE t_ai_execution_log IS 'AI 工作流執行日誌';
COMMENT ON COLUMN t_ai_execution_log.workflow_name IS '工作流名稱：analyzer/developer/qa';
COMMENT ON COLUMN t_ai_execution_log.trigger_type IS '觸發類型：cron/alert/webhook/manual';
COMMENT ON COLUMN t_ai_execution_log.crew_name IS 'CrewAI Crew 名稱';
COMMENT ON COLUMN t_ai_execution_log.duration_seconds IS '執行時長（秒）';

-- ============================================
-- 3. Agent 性能追蹤表
-- ============================================
CREATE TABLE IF NOT EXISTS t_agent_performance (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(100) NOT NULL,                    -- Agent 名稱
    execution_id BIGINT REFERENCES t_ai_execution_log(id) ON DELETE CASCADE,  -- 關聯執行記錄
    task_description TEXT,                               -- 任務描述
    execution_time_seconds INT,                          -- 執行時間（秒）
    llm_tokens_used INT,                                 -- Claude API token 使用量
    llm_cost_usd DECIMAL(10, 4),                         -- 成本（美元）
    success BOOLEAN DEFAULT FALSE,                       -- 是否成功
    quality_score DECIMAL(3, 2),                         -- 質量評分（0.00-1.00）
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引：按 Agent 查詢
CREATE INDEX idx_agent_performance_agent_name ON t_agent_performance(agent_name);

-- 索引：按執行記錄查詢
CREATE INDEX idx_agent_performance_execution_id ON t_agent_performance(execution_id);

-- 索引：按時間查詢
CREATE INDEX idx_agent_performance_created_at ON t_agent_performance(created_at DESC);

-- 註釋
COMMENT ON TABLE t_agent_performance IS 'Agent 性能追蹤表';
COMMENT ON COLUMN t_agent_performance.llm_tokens_used IS 'Claude API token 使用量';
COMMENT ON COLUMN t_agent_performance.llm_cost_usd IS 'Claude API 調用成本（美元）';
COMMENT ON COLUMN t_agent_performance.quality_score IS '代碼質量評分（基於 ArchUnit/PMD 結果）';

-- ============================================
-- 4. Skill 使用統計表
-- ============================================
CREATE TABLE IF NOT EXISTS t_skill_usage_stats (
    id BIGSERIAL PRIMARY KEY,
    skill_name VARCHAR(100) NOT NULL,                    -- Skill 名稱
    execution_id BIGINT REFERENCES t_ai_execution_log(id) ON DELETE CASCADE,
    phase VARCHAR(50),                                   -- 執行階段
    success BOOLEAN DEFAULT FALSE,
    execution_time_seconds INT,
    files_modified INT DEFAULT 0,                        -- 修改的文件數量
    tests_added INT DEFAULT 0,                           -- 添加的測試數量
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引：按 Skill 查詢
CREATE INDEX idx_skill_usage_stats_skill_name ON t_skill_usage_stats(skill_name);

-- 索引：按時間查詢
CREATE INDEX idx_skill_usage_stats_created_at ON t_skill_usage_stats(created_at DESC);

-- 註釋
COMMENT ON TABLE t_skill_usage_stats IS 'Skill 使用統計表';
COMMENT ON COLUMN t_skill_usage_stats.skill_name IS 'Skill 名稱（如 smartadmin-crud-generator）';
COMMENT ON COLUMN t_skill_usage_stats.phase IS '執行階段（如 backend-only, frontend-only）';

-- ============================================
-- 5. 成本歸因表（追蹤每次執行的成本）
-- ============================================
CREATE TABLE IF NOT EXISTS t_cost_attribution (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT REFERENCES t_ai_execution_log(id) ON DELETE CASCADE,
    agent_name VARCHAR(100),
    llm_model VARCHAR(50),                               -- claude-sonnet-4.5, claude-haiku-3.5
    tokens_used INT,
    cost_usd DECIMAL(10, 4),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 索引：按執行記錄查詢
CREATE INDEX idx_cost_attribution_execution_id ON t_cost_attribution(execution_id);

-- 索引：按時間查詢
CREATE INDEX idx_cost_attribution_created_at ON t_cost_attribution(created_at DESC);

-- 註釋
COMMENT ON TABLE t_cost_attribution IS '成本歸因表（追蹤每次 LLM 調用成本）';
COMMENT ON COLUMN t_cost_attribution.llm_model IS 'LLM 模型名稱';
COMMENT ON COLUMN t_cost_attribution.tokens_used IS 'Token 使用量';
COMMENT ON COLUMN t_cost_attribution.cost_usd IS '成本（美元）';

-- ============================================
-- 6. 數據保留策略（自動清理舊數據）
-- ============================================

-- 創建清理舊審計日誌的函數（保留 90 天）
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs()
RETURNS void AS $$
BEGIN
    DELETE FROM t_ai_operation_audit WHERE created_at < NOW() - INTERVAL '90 days';
    DELETE FROM t_ai_execution_log WHERE created_at < NOW() - INTERVAL '90 days';
    DELETE FROM t_agent_performance WHERE created_at < NOW() - INTERVAL '90 days';
    DELETE FROM t_skill_usage_stats WHERE created_at < NOW() - INTERVAL '90 days';
    DELETE FROM t_cost_attribution WHERE created_at < NOW() - INTERVAL '90 days';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_audit_logs() IS '清理 90 天前的 AI 審計日誌';

-- ============================================
-- 7. 初始化測試數據（可選）
-- ============================================

-- 插入示例審計日誌（用於測試）
INSERT INTO t_ai_operation_audit (agent_name, operation, file_path, decision, denied_reason, success)
VALUES
    ('java-architect', 'read', 'src/main/java/controller/EmployeeController.java', 'allow', NULL, TRUE),
    ('java-architect', 'write', 'src/main/resources/application-prod.yml', 'deny', '路徑匹配禁止模式', FALSE),
    ('postgres-pro', 'read', 'src/main/resources/db/migration/V001__init.sql', 'allow', NULL, TRUE);

-- 完成提示
SELECT 'AI 系統表創建完成！' AS message;
SELECT
    'Tables: t_ai_operation_audit, t_ai_execution_log, t_agent_performance, t_skill_usage_stats, t_cost_attribution' AS tables_created;
