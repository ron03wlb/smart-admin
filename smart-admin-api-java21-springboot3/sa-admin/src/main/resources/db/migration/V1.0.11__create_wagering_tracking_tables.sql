-- ============================================================================
-- 流水要求追蹤與審計系統 DDL
-- 版本: 1.0.0
-- 創建日期: 2026-01-28
-- 說明: 支持實時累積、取款驗證、回推重算的完整流水追蹤系統
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 表 1: wagering_details（有效投注明細表）
-- 用途: 記錄每筆投注的原始數據和計算結果，支持審計回推
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wagering_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵ID',

    -- 關聯信息
    user_id BIGINT NOT NULL COMMENT '用戶ID',
    promotion_id BIGINT COMMENT '活動ID（NULL表示不參與任何活動）',
    transaction_id VARCHAR(100) NOT NULL UNIQUE COMMENT '交易ID（關聯 t_wallet_transaction）',

    -- 原始數據（不可變，用於回推重算）
    bet_amount DECIMAL(18,2) NOT NULL COMMENT '投注額（原始金額）',
    game_type VARCHAR(50) NOT NULL COMMENT '遊戲類型（SLOT, ROULETTE, BLACKJACK, SPORTS_BETTING 等）',
    game_code VARCHAR(100) COMMENT '具體遊戲代碼（用於細粒度分析）',
    odds DECIMAL(10,2) COMMENT '賠率（體育博彩專用）',
    bet_details JSON COMMENT '詳細下注信息（JSON格式，記錄投注細節）',

    -- 計算結果（可變，規則調整後需重算）
    valid_bet DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '有效投注額（經風控過濾後的金額）',
    game_contribution DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '遊戲貢獻權重（0.0000-1.0000）',
    contributed_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '實際貢獻金額 = valid_bet * game_contribution',

    -- 計算依據（支持審計追溯）
    risk_rules_applied JSON COMMENT '應用的風控規則詳情（JSON格式）',
    calculation_version VARCHAR(20) NOT NULL DEFAULT 'v1.0.0' COMMENT '計算邏輯版本號（用於識別需要重算的記錄）',

    -- 審計字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    recalculated_at DATETIME COMMENT '最後重算時間',
    recalculated_by VARCHAR(100) COMMENT '重算操作人（USER_ID 或 SYSTEM）',
    deleted_flag TINYINT NOT NULL DEFAULT 0 COMMENT '刪除標記（0-未刪除 1-已刪除）',

    -- 索引優化
    INDEX idx_user_promo (user_id, promotion_id) COMMENT '用戶活動查詢索引',
    INDEX idx_transaction (transaction_id) COMMENT '交易ID唯一索引（用於快速查找）',
    INDEX idx_created_at (created_at) COMMENT '時間範圍查詢索引（用於回推重算）',
    INDEX idx_calc_version (calculation_version) COMMENT '版本號索引（用於批量重算）',
    INDEX idx_game_type (game_type) COMMENT '遊戲類型索引（用於統計分析）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='有效投注明細表（支持審計回推）';

-- ----------------------------------------------------------------------------
-- 表 2: wagering_progress（流水要求進度表）
-- 用途: 聚合視圖，快速查詢用戶在各活動中的流水達成進度
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wagering_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵ID',

    -- 關聯信息
    user_id BIGINT NOT NULL COMMENT '用戶ID',
    promotion_id BIGINT NOT NULL COMMENT '活動ID',

    -- 流水要求與進度
    total_requirement DECIMAL(18,2) NOT NULL COMMENT '總流水要求（活動規則設定的目標金額）',
    completed_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已完成金額（累積的有效投注貢獻）',
    remaining_amount DECIMAL(18,2) NOT NULL COMMENT '剩餘要求（total_requirement - completed_amount）',

    -- 狀態管理
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '狀態（ACTIVE-進行中 COMPLETED-已完成 EXPIRED-已過期 FORFEITED-已沒收）',

    -- 時間追蹤
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '開始時間（加入活動時間）',
    completed_at DATETIME COMMENT '完成時間（達標時間）',
    expired_at DATETIME COMMENT '過期時間（活動結束時間）',
    last_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最後更新時間',

    -- 審計字段
    deleted_flag TINYINT NOT NULL DEFAULT 0 COMMENT '刪除標記（0-未刪除 1-已刪除）',

    -- 唯一約束與索引
    UNIQUE KEY uk_user_promo (user_id, promotion_id) COMMENT '用戶活動唯一約束',
    INDEX idx_status (status) COMMENT '狀態查詢索引（用於監控預警）',
    INDEX idx_remaining (remaining_amount) COMMENT '剩餘要求索引（用於查詢未達標用戶）',
    INDEX idx_expired (expired_at) COMMENT '過期時間索引（用於定時任務清理）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流水要求進度表（聚合視圖）';

-- ----------------------------------------------------------------------------
-- 表 3: wagering_recalculation_audit（回推重算審計表）
-- 用途: 記錄每次回推重算操作的詳細信息，用於審計和問題追溯
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wagering_recalculation_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵ID',

    -- 回推範圍
    user_id BIGINT COMMENT '用戶ID（NULL表示全局重算）',
    promotion_id BIGINT COMMENT '活動ID（NULL表示全局重算）',
    start_time DATETIME NOT NULL COMMENT '重算開始時間範圍',
    end_time DATETIME NOT NULL COMMENT '重算結束時間範圍',

    -- 規則版本
    old_rule_version VARCHAR(20) NOT NULL COMMENT '舊規則版本號',
    new_rule_version VARCHAR(20) NOT NULL COMMENT '新規則版本號',

    -- 重算結果統計
    affected_transactions INT NOT NULL DEFAULT 0 COMMENT '影響的交易筆數',
    changed_transactions INT NOT NULL DEFAULT 0 COMMENT '實際變更的交易筆數',
    old_total_contributed DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '重算前的總貢獻金額',
    new_total_contributed DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '重算後的總貢獻金額',
    total_difference DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '差異金額（new - old）',

    -- 操作信息
    recalculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '重算執行時間',
    recalculated_by VARCHAR(100) NOT NULL COMMENT '操作人（USER_ID 或 SYSTEM）',
    reason TEXT COMMENT '重算原因說明',

    -- 審計字段
    deleted_flag TINYINT NOT NULL DEFAULT 0 COMMENT '刪除標記（0-未刪除 1-已刪除）',

    -- 索引
    INDEX idx_user_promo (user_id, promotion_id) COMMENT '用戶活動查詢索引',
    INDEX idx_recalculated_at (recalculated_at) COMMENT '時間查詢索引',
    INDEX idx_rule_version (new_rule_version) COMMENT '規則版本索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回推重算審計表';

-- ----------------------------------------------------------------------------
-- 初始化數據：遊戲貢獻權重配置
-- ----------------------------------------------------------------------------
-- 注意：這裡僅作為示例，實際應該存儲在配置表中
-- INSERT INTO t_game_contribution (game_type, contribution, description) VALUES
-- ('SLOT', 1.0000, '老虎機 - 100% 貢獻'),
-- ('VIDEO_POKER', 1.0000, '視訊撲克 - 100% 貢獻'),
-- ('BLACKJACK', 0.1000, '二十一點 - 10% 貢獻'),
-- ('ROULETTE', 0.1000, '輪盤 - 10% 貢獻'),
-- ('BACCARAT', 0.1000, '百家樂 - 10% 貢獻'),
-- ('SPORTS_BETTING', 0.5000, '體育博彩 - 50% 貢獻'),
-- ('LIVE_DEALER', 0.2000, '真人荷官 - 20% 貢獻');

-- ============================================================================
-- 遷移腳本：為現有活動中的玩家生成歷史記錄
-- ============================================================================
-- 注意：此腳本應在業務確認後執行，需根據實際表結構調整
--
-- INSERT INTO t_wagering_detail (
--     user_id, promotion_id, transaction_id,
--     bet_amount, game_type, valid_bet,
--     game_contribution, contributed_amount,
--     calculation_version, created_at
-- )
-- SELECT
--     wt.user_id,
--     ap.promotion_id,
--     wt.transaction_id,
--     wt.bet_amount,
--     wt.game_type,
--     COALESCE(wt.valid_bet, wt.bet_amount),  -- 如果 valid_bet 為空，使用 bet_amount
--     COALESCE(gc.contribution, 1.0000),       -- 默認 100% 貢獻
--     COALESCE(wt.valid_bet, wt.bet_amount) * COALESCE(gc.contribution, 1.0000),
--     'v1.0.0',
--     wt.created_at
-- FROM t_wallet_transaction wt
-- INNER JOIN t_active_promotion ap ON wt.user_id = ap.user_id
-- LEFT JOIN t_game_contribution gc ON wt.game_type = gc.game_type
-- WHERE ap.status = 'ACTIVE'
--   AND wt.created_at >= ap.started_at
--   AND wt.transaction_type = 'BET'
--   AND wt.deleted_flag = 0;

-- ============================================================================
-- 性能優化建議
-- ============================================================================
-- 1. 定期歸檔歷史數據（建議保留最近 3 個月的熱數據）
-- 2. 考慮按月分表策略（t_wagering_detail_202601, t_wagering_detail_202602...）
-- 3. Redis 緩存熱點數據（wagering:progress:{userId}:{promotionId}）
-- 4. 使用 Read Replica 進行報表查詢
-- 5. 監控慢查詢並優化索引

-- ============================================================================
-- 監控指標建議
-- ============================================================================
-- 1. wagering_progress_rate: 流水達成率分佈
-- 2. wagering_detail_insert_rate: 明細表寫入速率（TPS）
-- 3. wagering_recalculation_duration: 回推重算耗時
-- 4. wagering_verification_latency: 取款驗證延遲
