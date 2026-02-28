-- =====================================================================
-- V17: Register iGaming SmartJob scheduled tasks
-- =====================================================================
-- Registers 8 iGaming scheduled tasks into t_smart_job for management
-- via the SmartAdmin admin UI.
-- =====================================================================

INSERT INTO t_smart_job (job_name, job_class, trigger_type, trigger_value,
    param, enabled_flag, sort, deleted_flag, tenant_id, create_time, update_time)
VALUES
-- Activity module
('紅利過期清理', 'net.lab1024.sa.igaming.activity.job.BonusExpirationJob',
 'cron', '0 0/30 * * * ?', NULL, true, 101, false, 1, NOW(), NOW()),

('VIP 等級評估', 'net.lab1024.sa.igaming.activity.job.VipEvaluationJob',
 'cron', '0 0 4 * * ?', NULL, true, 102, false, 1, NOW(), NOW()),

-- Game module
('GP 健康檢查', 'net.lab1024.sa.igaming.game.job.GpHealthCheckJob',
 'fixed_delay', '60000', NULL, true, 201, false, 1, NOW(), NOW()),

('缺失結算輪詢', 'net.lab1024.sa.igaming.game.job.MissingSettlementPollJob',
 'fixed_delay', '300000', NULL, true, 202, false, 1, NOW(), NOW()),

('每日遊戲對帳', 'net.lab1024.sa.igaming.game.job.DailyReconciliationJob',
 'cron', '0 0 2 * * ?', NULL, true, 203, false, 1, NOW(), NOW()),

-- Risk module
('風控提案 SLA 監控', 'net.lab1024.sa.igaming.risk.job.RiskProposalSLAJob',
 'fixed_delay', '180000', NULL, true, 301, false, 1, NOW(), NOW()),

-- Wallet/Payment module
('支付對帳', 'net.lab1024.sa.igaming.wallet.payment.job.PaymentReconciliationJob',
 'cron', '0 0 5 * * ?', NULL, true, 401, false, 1, NOW(), NOW()),

('待處理訂單輪詢', 'net.lab1024.sa.igaming.wallet.payment.job.PendingOrderPollJob',
 'fixed_delay', '120000', NULL, true, 402, false, 1, NOW(), NOW())

ON CONFLICT DO NOTHING;
