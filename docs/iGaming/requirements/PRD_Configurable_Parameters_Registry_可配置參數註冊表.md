---
title: "PRD: Configurable Parameters Registry — 可配置參數註冊表"
part: requirements
module: platform-configuration
version: v1.0
created: 2026-03-25
sprint: v2.2
status: DRAFT — 待 Ron 審核
authors:
  - Ron (決策)
  - Claude (撰寫)
scope: 全平台 16 章 + 附錄
---

# PRD: 可配置參數註冊表 (Configurable Parameters Registry)

---

## 1. Problem Statement

平台需求文件中存在約 **845+ 個硬編碼數值**（百分比、金額、時間、次數、閾值），散佈在 16 個需求章節與附錄中。這些數值目前以固定常量形式記載，導致：

1. **營運僵化** — 不同品牌（Brand）或管轄區（Jurisdiction）無法各自調整業務規則，必須修改代碼
2. **合規風險** — 監管要求因地區而異（如 UKGC £2 投注上限 vs MGA 無此限制），硬編碼無法靈活應對
3. **部署耦合** — 修改任何閾值都需要代碼變更 + 部署流程，無法即時生效
4. **審計困難** — 無法追蹤「誰在何時將閾值從 X 改為 Y」

**影響範圍**: 全平台所有模組、所有品牌、所有管轄區

---

## 2. Goals

| # | 目標 | 衡量方式 |
|---|------|---------|
| G1 | 所有業務規則參數可在 DB 中配置，無需代碼變更 | 845+ 參數全部遷移至配置表 |
| G2 | 支援三層覆蓋層級：Global → Brand → Jurisdiction | 覆蓋解析邏輯通過所有測試案例 |
| G3 | 所有配置變更具備完整審計軌跡 | 每筆變更記錄 who/when/old/new |
| G4 | 配置變更可即時生效（≤ 5 秒），無需重啟服務 | Hot-reload 延遲 ≤ 5 秒 |
| G5 | 提供管理後台 UI 供營運人員修改配置 | 非工程師可自助修改配置 |

---

## 3. Non-Goals

| # | 排除項 | 原因 |
|---|--------|------|
| NG1 | A/B 測試框架 | 另案處理，與配置系統解耦 |
| NG2 | 動態定價引擎 | 涉及 ML 模型，超出靜態配置範疇 |
| NG3 | 遊戲內容管理 (CMS) | GP 遊戲元數據由 Ch4 遊戲大廳管理 |
| NG4 | 用戶偏好設定 | 玩家個人設定屬於 Player Profile，非平台配置 |

---

## 4. Three-Tier Override Hierarchy — 三層覆蓋架構

### 4.1 架構概述

```
┌─────────────────────────────────────┐
│  Jurisdiction Override (管轄區覆蓋)   │  最高優先 — 監管強制
│  例: UKGC slots_max_bet = £2        │
├─────────────────────────────────────┤
│  Brand Override (品牌覆蓋)           │  中間層 — 品牌差異化
│  例: BrandA welcome_bonus_cap = $300 │
├─────────────────────────────────────┤
│  Global Default (全域預設)           │  基礎層 — 平台預設值
│  例: welcome_bonus_cap = $500        │
└─────────────────────────────────────┘
```

### 4.2 Override Resolution Logic — 覆蓋解析邏輯

```
function resolveConfig(paramKey, brandId, jurisdictionCode):
    // Step 1: 查找管轄區覆蓋
    jurisdictionValue = db.find(paramKey, scope='jurisdiction', code=jurisdictionCode)
    if jurisdictionValue exists:
        return jurisdictionValue

    // Step 2: 查找品牌覆蓋
    brandValue = db.find(paramKey, scope='brand', id=brandId)
    if brandValue exists:
        return brandValue

    // Step 3: 回退到全域預設
    globalValue = db.find(paramKey, scope='global')
    return globalValue
```

### 4.3 Strictest-Rule-Applies Pattern — 最嚴規則適用

對於**合規相關**參數（標記 `compliance_driven = true`），解析邏輯改為「最嚴規則適用」：

```
function resolveComplianceConfig(paramKey, brandId, jurisdictionCode):
    values = []

    globalVal = db.find(paramKey, scope='global')
    brandVal = db.find(paramKey, scope='brand', id=brandId)
    jurisdictionVal = db.find(paramKey, scope='jurisdiction', code=jurisdictionCode)

    // 收集所有非空值
    for val in [globalVal, brandVal, jurisdictionVal]:
        if val exists: values.append(val)

    // 根據參數方向性選擇最嚴值
    if param.direction == 'LOWER_IS_STRICTER':  // 如: 投注上限、存款限額
        return min(values)
    elif param.direction == 'HIGHER_IS_STRICTER':  // 如: KYC 等級、保留年限
        return max(values)
    else:
        return jurisdictionVal ?? brandVal ?? globalVal  // 回退到標準覆蓋
```

### 4.4 解析範例

| 參數 | Global | Brand A | UKGC | 解析結果 | 邏輯 |
|------|--------|---------|------|---------|------|
| `slots_max_bet` | $10 | $8 | £5 | **£5** | 合規: LOWER_IS_STRICTER |
| `welcome_bonus_cap` | $500 | $300 | — | **$300** | 品牌覆蓋 |
| `kyc_retention_years` | 5 | — | 7 | **7** | 合規: HIGHER_IS_STRICTER |
| `agent_idle_timeout_min` | 30 | 20 | — | **20** | 品牌覆蓋 |
| `rtp_warning_threshold` | 120% | — | — | **120%** | 全域預設 |

---

## 5. DB Schema Design — 資料庫結構設計

### 5.1 Core Tables

```sql
-- 參數定義表 (靜態，由代碼/遷移維護)
CREATE TABLE config_param_definition (
    param_key       VARCHAR(128) PRIMARY KEY,    -- 如 'ch2.wallet.round_timeout_hours'
    chapter         VARCHAR(8)   NOT NULL,        -- 如 'Ch2'
    section         VARCHAR(16),                  -- 如 '§2.6'
    display_name    VARCHAR(256) NOT NULL,        -- 中英文名稱
    description     TEXT,
    data_type       ENUM('INTEGER','DECIMAL','DURATION','PERCENTAGE','AMOUNT','BOOLEAN','ENUM') NOT NULL,
    unit            VARCHAR(32),                  -- 如 'minutes', 'USD', '%'
    default_value   VARCHAR(256) NOT NULL,        -- 全域預設值
    min_value       VARCHAR(256),                 -- 驗證下界
    max_value       VARCHAR(256),                 -- 驗證上界
    direction       ENUM('LOWER_IS_STRICTER','HIGHER_IS_STRICTER','NEUTRAL'),
    compliance_driven BOOLEAN DEFAULT FALSE,      -- 合規驅動 → 最嚴規則
    category        ENUM('PERCENTAGE','AMOUNT','DURATION','COUNT','THRESHOLD') NOT NULL,
    is_sensitive    BOOLEAN DEFAULT FALSE,        -- 敏感參數需更高審批
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 參數值表 (動態，由管理後台維護)
CREATE TABLE config_param_value (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    param_key       VARCHAR(128) NOT NULL,
    scope           ENUM('GLOBAL','BRAND','JURISDICTION') NOT NULL,
    scope_id        VARCHAR(64),                  -- NULL=global, brand_id, jurisdiction_code
    value           VARCHAR(256) NOT NULL,
    effective_from  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_until TIMESTAMP,                    -- NULL = 永久有效
    is_active       BOOLEAN DEFAULT TRUE,
    created_by      VARCHAR(128) NOT NULL,
    approved_by     VARCHAR(128),                 -- Maker-Checker
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_param_scope (param_key, scope, scope_id, effective_from),
    FOREIGN KEY (param_key) REFERENCES config_param_definition(param_key)
);

-- 審計日誌表 (不可變)
CREATE TABLE config_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    param_key       VARCHAR(128) NOT NULL,
    scope           ENUM('GLOBAL','BRAND','JURISDICTION') NOT NULL,
    scope_id        VARCHAR(64),
    old_value       VARCHAR(256),
    new_value       VARCHAR(256),
    change_type     ENUM('CREATE','UPDATE','DELETE','APPROVE','REJECT') NOT NULL,
    changed_by      VARCHAR(128) NOT NULL,
    change_reason   TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_param_time (param_key, created_at),
    INDEX idx_changed_by (changed_by, created_at)
);
```

### 5.2 Caching & Hot-Reload

| 層級 | 機制 | TTL | 說明 |
|------|------|-----|------|
| L1 | Application Local Cache | 5 秒 | 每個服務實例本地快取 |
| L2 | Redis Distributed Cache | 30 秒 | 跨服務共享 |
| L3 | Database | — | 權威來源 |

**變更推播**: 配置變更時透過 Redis Pub/Sub 通知所有服務實例立即刷新 L1 快取。

---

## 6. Parameter Registry — 參數註冊表

> 以下為全平台 845+ 個可配置參數的完整登記表，按章節組織。
> 每個參數都需從硬編碼遷移至 `config_param_definition` + `config_param_value` 表。

### 6.1 Ch1 — 玩家管理 (Player Management)

#### 帳戶生命週期

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch1.account.dormant_days | 休眠帳戶天數 | 90 | DURATION (days) | global | N |
| ch1.account.closure_inactivity_days | 關閉帳戶不活躍天數 | 365 | DURATION (days) | global | N |
| ch1.registration.email_code_validity_min | Email 驗證碼有效期 | 10 | DURATION (min) | global | N |
| ch1.registration.sms_otp_validity_min | SMS OTP 有效期 | 5 | DURATION (min) | global | N |
| ch1.registration.same_ip_24h_limit | 同 IP 24h 註冊上限 | 3 | COUNT | global | N |

#### KYC 四級限額

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch1.kyc.min_age_global | 最低年齡 (全球) | 18 | THRESHOLD | global | Y |
| ch1.kyc.min_age_usa | 最低年齡 (美國) | 21 | THRESHOLD | jurisdiction | Y |
| ch1.kyc.l0_daily_withdrawal | L0 日提款限額 | 0 | AMOUNT ($) | global | Y |
| ch1.kyc.l0_daily_deposit | L0 日存款限額 | 500 | AMOUNT ($) | global | Y |
| ch1.kyc.l1_daily_withdrawal | L1 日提款限額 | 1,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l1_daily_deposit | L1 日存款限額 | 5,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l2_daily_withdrawal | L2 日提款限額 | 10,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l2_daily_deposit | L2 日存款限額 | 50,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l2_cumulative_deposit_trigger | L2 升級觸發累存 | 2,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l2_cumulative_withdrawal_trigger | L2 升級觸發累提 | 1,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l3_cumulative_deposit_trigger | L3 升級觸發累存 | 10,000 | AMOUNT ($) | global | Y |
| ch1.kyc.l1_auto_approval_sla_sec | L1 自動審批 SLA | 30 | DURATION (sec) | global | N |
| ch1.kyc.l1_manual_approval_sla_hr | L1 人工審批 SLA | 24 | DURATION (hr) | global | N |
| ch1.kyc.l2_auto_approval_sla_min | L2 自動審批 SLA | 1 | DURATION (min) | global | N |
| ch1.kyc.l2_manual_approval_sla_hr | L2 人工審批 SLA | 48 | DURATION (hr) | global | N |
| ch1.kyc.l3_bank_verify_sla_days | L3 銀行驗證 SLA | 3 | DURATION (days) | global | N |
| ch1.kyc.l3_manual_approval_sla_hr | L3 人工審批 SLA | 72 | DURATION (hr) | global | N |
| ch1.kyc.ukgc_l1_completion_hr | UKGC L1 完成時限 | 72 | DURATION (hr) | jurisdiction | Y |
| ch1.kyc.ukgc_l2_completion_hr | UKGC L2 完成時限 | 72 | DURATION (hr) | jurisdiction | Y |
| ch1.kyc.mga_l2_completion_days | MGA L2 完成時限 | 90 | DURATION (days) | jurisdiction | Y |
| ch1.kyc.pagcor_withdrawal_trigger | PAGCOR 提款觸發 | 50,000 | AMOUNT (₱) | jurisdiction | Y |
| ch1.kyc.curacao_withdrawal_trigger | Curaçao 提款觸發 | 2,000 | AMOUNT ($) | jurisdiction | Y |

#### VIP 等級

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch1.vip.silver_upgrade | Silver 升級門檻 | 10,000 | AMOUNT ($) | brand | N |
| ch1.vip.silver_maintain | Silver 維持門檻 | 5,000 | AMOUNT ($) | brand | N |
| ch1.vip.gold_upgrade | Gold 升級門檻 | 50,000 | AMOUNT ($) | brand | N |
| ch1.vip.gold_maintain | Gold 維持門檻 | 25,000 | AMOUNT ($) | brand | N |
| ch1.vip.platinum_upgrade | Platinum 升級門檻 | 200,000 | AMOUNT ($) | brand | N |
| ch1.vip.platinum_maintain | Platinum 維持門檻 | 100,000 | AMOUNT ($) | brand | N |
| ch1.vip.diamond_upgrade | Diamond 升級門檻 | 500,000 | AMOUNT ($) | brand | N |
| ch1.vip.diamond_maintain | Diamond 維持門檻 | 250,000 | AMOUNT ($) | brand | N |
| ch1.vip.silver_cashback_pct | Silver 返現率 | 0.3 | PERCENTAGE | brand | N |
| ch1.vip.gold_cashback_pct | Gold 返現率 | 0.5 | PERCENTAGE | brand | N |
| ch1.vip.platinum_cashback_pct | Platinum 返現率 | 0.8 | PERCENTAGE | brand | N |
| ch1.vip.diamond_cashback_pct | Diamond 返現率 | 1.2 | PERCENTAGE | brand | N |
| ch1.vip.points_validity_days | 積分有效期 | 365 | DURATION (days) | brand | N |
| ch1.vip.points_per_dollar | 積分兌換比率 | 100 | COUNT | brand | N |
| ch1.vip.downgrade_consecutive_months | 降級觸發月數 | 2 | COUNT | brand | N |
| ch1.vip.diamond_downgrade_protection_months | Diamond 降級保護 | 3 | DURATION (months) | brand | N |
| ch1.vip.platinum_downgrade_protection_months | Platinum 降級保護 | 2 | DURATION (months) | brand | N |
| ch1.vip.standard_downgrade_protection_months | 一般降級保護 | 1 | DURATION (months) | brand | N |

#### RFM 分群

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch1.rfm.recency_score5_days | R 分=5 天數 | 7 | THRESHOLD (days) | global | N |
| ch1.rfm.recency_score4_days | R 分=4 天數 | 14 | THRESHOLD (days) | global | N |
| ch1.rfm.recency_score3_days | R 分=3 天數 | 30 | THRESHOLD (days) | global | N |
| ch1.rfm.recency_score2_days | R 分=2 天數 | 60 | THRESHOLD (days) | global | N |
| ch1.rfm.frequency_score5 | F 分=5 投注數/30d | 100 | COUNT | global | N |
| ch1.rfm.frequency_score4 | F 分=4 投注數/30d | 50 | COUNT | global | N |
| ch1.rfm.frequency_score3 | F 分=3 投注數/30d | 20 | COUNT | global | N |
| ch1.rfm.frequency_score2 | F 分=2 投注數/30d | 5 | COUNT | global | N |
| ch1.rfm.monetary_score5 | M 分=5 金額 | 10,000 | AMOUNT ($) | global | N |
| ch1.rfm.monetary_score4 | M 分=4 金額 | 5,000 | AMOUNT ($) | global | N |
| ch1.rfm.monetary_score3 | M 分=3 金額 | 1,000 | AMOUNT ($) | global | N |
| ch1.rfm.monetary_score2 | M 分=2 金額 | 100 | AMOUNT ($) | global | N |

#### 風險與 AML

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch1.risk.high_score_threshold | 高風險分數閾值 | 70 | THRESHOLD | global | N |
| ch1.risk.whale_monthly_threshold | 鯨魚月投注閾值 | 100,000 | AMOUNT ($) | global | N |
| ch1.risk.daily_deposits_flag | 日存款次數風險旗標 | 5 | COUNT | global | N |
| ch1.risk.loss_chasing_sequence | 追損連續投注數 | 3 | COUNT | global | N |
| ch1.aml.edd_single_txn_threshold | EDD 單筆觸發 | 10,000 | AMOUNT ($) | jurisdiction | Y |
| ch1.aml.edd_30day_cumulative | EDD 30天累計觸發 | 25,000 | AMOUNT ($) | jurisdiction | Y |
| ch1.aml.sar_initial_assessment_hr | SAR 初始評估 SLA | 24 | DURATION (hr) | global | Y |
| ch1.aml.sar_mlro_review_hr | SAR MLRO 審核 SLA | 48 | DURATION (hr) | global | Y |

#### 數據保留與安全

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch1.retention.identity_years | 身份數據保留年限 | 5 | DURATION (years) | jurisdiction | Y |
| ch1.retention.kyc_docs_years | KYC 文件保留年限 | 5 | DURATION (years) | jurisdiction | Y |
| ch1.retention.txn_records_years | 交易記錄保留年限 | 7 | DURATION (years) | jurisdiction | Y |
| ch1.security.mfa_backup_codes | MFA 恢復碼數量 | 10 | COUNT | global | N |
| ch1.security.mfa_reset_cooldown_hr | MFA 重置冷靜期 | 24 | DURATION (hr) | global | N |
| ch1.security.mfa_reset_withdrawal_block_hr | MFA 重置提款封鎖期 | 24 | DURATION (hr) | global | N |

### 6.2 Ch2 — 錢包系統 (Wallet System)

#### 核心錢包

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.wallet.playable_balance_accuracy_pct | 可玩餘額準確度 | 99.99 | PERCENTAGE | global | N |
| ch2.wallet.round_timeout_hours | 回合超時 | 2 | DURATION (hr) | global | N |
| ch2.wallet.orphan_scan_frequency_min | 孤兒回合掃描頻率 | 15 | DURATION (min) | global | N |
| ch2.wallet.orphan_resolution_sla_hr | 孤兒回合解決 SLA | 24 | DURATION (hr) | global | N |
| ch2.wallet.large_win_threshold | 大額派彩閾值 | 10,000 | AMOUNT ($) | global | N |
| ch2.wallet.txn_response_cache_hr | 交易回應快取時長 | 1 | DURATION (hr) | global | N |
| ch2.wallet.token_validity_min | Token 有效期 | 5 | DURATION (min) | global | N |

#### 紅利錢包

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.bonus.welcome_standard_multiplier | 歡迎紅利標準倍數 | 20 | COUNT | brand | N |
| ch2.bonus.expiration_notification_hr | 紅利過期通知 | 24 | DURATION (hr) | global | N |

#### 信用額度

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.credit.settlement_cycle_weeks | 信用額度結算週期 | 1 | DURATION (weeks) | brand | N |

#### 負餘額保護

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.negative.tolerance_window_sec | 負餘額容忍窗口 | 5 | DURATION (sec) | global | N |
| ch2.negative.single_account_alert | 單帳戶負餘額警報 | 100 | AMOUNT ($) | global | N |
| ch2.negative.platform_alert | 平台負餘額警報 | 10,000 | AMOUNT ($) | global | N |

#### 對帳

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.recon.hourly_frequency | 小時對帳頻率 | 1 | DURATION (hr) | global | N |
| ch2.recon.amount_auto_correct | 自動修正金額閾值 | 1 | AMOUNT ($) | global | N |
| ch2.recon.time_auto_match_min | 自動匹配時間閾值 | 5 | DURATION (min) | global | N |
| ch2.recon.large_diff_alert | 大額差異警報 | 100 | AMOUNT ($) | global | N |
| ch2.recon.missing_record_alert_per_hr | 每小時缺失記錄警報 | 10 | COUNT | global | N |
| ch2.recon.failure_sla_hr | 對帳失敗 SLA | 3 | DURATION (hr) | global | N |

#### 多幣種 / FX

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.fx.decimal_precision | 小數精度 | 4 | COUNT | global | N |
| ch2.fx.rate_update_frequency_min | 法幣匯率更新頻率 | 5 | DURATION (min) | global | N |
| ch2.fx.crypto_rate_update_sec | 加密幣匯率更新頻率 | 30 | DURATION (sec) | global | N |
| ch2.fx.deposit_rate_freeze_min | 存款匯率鎖定期 | 15 | DURATION (min) | global | N |
| ch2.fx.fiat_absorption_pct | 法幣 FX 吸收比例 | 0.5 | PERCENTAGE | global | N |
| ch2.fx.crypto_absorption_pct | 加密幣 FX 吸收比例 | 2.0 | PERCENTAGE | global | N |

#### 出金流水驗證

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch2.withdrawal.amount_lock_ms | 出金金額鎖定回應 | 50 | DURATION (ms) | global | N |
| ch2.withdrawal.wagering_query_ms | 流水查詢回應 | 200 | DURATION (ms) | global | N |
| ch2.withdrawal.wagering_timeout_ms | 流水查詢超時 | 500 | DURATION (ms) | global | N |
| ch2.withdrawal.pending_queue_sla_min | 待處理佇列 SLA | 15 | DURATION (min) | global | N |
| ch2.withdrawal.pending_escalation_hr | 待處理升級 SLA | 2 | DURATION (hr) | global | N |

### 6.3 Ch3 — 支付系統 (Payment System)

#### 加密貨幣確認

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch3.crypto.usdt_confirmations | USDT 確認數 | 3 | COUNT | global | N |
| ch3.crypto.btc_confirmations | BTC 確認數 | 3 | COUNT | global | N |
| ch3.crypto.eth_confirmations | ETH 確認數 | 12 | COUNT | global | N |

#### PSP 路由

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch3.psp.success_rate_weight_pct | 成功率權重 | 50 | PERCENTAGE | global | N |
| ch3.psp.fee_weight_pct | 費率權重 | 30 | PERCENTAGE | global | N |
| ch3.psp.speed_weight_pct | 速度權重 | 15 | PERCENTAGE | global | N |
| ch3.psp.vip_priority_weight_pct | VIP 優先權重 | 5 | PERCENTAGE | global | N |
| ch3.psp.health_healthy_pct | PSP 健康閾值 | 80 | PERCENTAGE | global | N |
| ch3.psp.health_degraded_pct | PSP 降級閾值 | 50 | PERCENTAGE | global | N |
| ch3.psp.recovery_consecutive_checks | 恢復所需連續檢查 | 3 | COUNT | global | N |
| ch3.psp.diamond_fee_discount_pct | Diamond 費率折扣 | 0.5 | PERCENTAGE | brand | N |
| ch3.psp.gold_settlement_speed_min | Gold 結算速度 | 5 | DURATION (min) | brand | N |

#### 收銀台摩擦分層

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch3.checkout.low_risk_max_steps | 低風險最大步驟 | 3 | COUNT | global | N |
| ch3.checkout.medium_risk_max_steps | 中風險最大步驟 | 5 | COUNT | global | N |
| ch3.checkout.high_risk_max_steps | 高風險最大步驟 | 7 | COUNT | global | N |
| ch3.checkout.medium_friction_delay_sec | 中風險摩擦延遲 | 5 | DURATION (sec) | global | N |
| ch3.checkout.high_friction_delay_sec | 高風險摩擦延遲 | 10 | DURATION (sec) | global | N |

#### 出金審批

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch3.withdrawal.auto_threshold | 自動出金閾值 | 500 | AMOUNT ($) | brand | N |
| ch3.withdrawal.tier1_max | Tier1 上限 | 100 | AMOUNT ($) | brand | N |
| ch3.withdrawal.tier2_sla_hr | $100-$1K 審批 SLA | 1 | DURATION (hr) | brand | N |
| ch3.withdrawal.tier3_sla_hr | $1K-$10K 審批 SLA | 4 | DURATION (hr) | brand | N |
| ch3.withdrawal.tier4_sla_hr | >$10K 審批 SLA | 24 | DURATION (hr) | brand | N |

#### Chargeback

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch3.cb.evidence_collection_days | 證據收集 SLA | 7 | DURATION (days) | global | N |
| ch3.cb.visa_representment_days | VISA 代位 SLA | 30 | DURATION (days) | jurisdiction | Y |
| ch3.cb.mc_representment_days | MC 代位 SLA | 45 | DURATION (days) | jurisdiction | Y |
| ch3.cb.dispute_fee | 爭議費用 | 250 | AMOUNT ($) | global | N |
| ch3.cb.rate_yellow_pct | CB 率黃色警報 | 0.5 | PERCENTAGE | global | N |
| ch3.cb.rate_orange_pct | CB 率橙色警報 | 0.8 | PERCENTAGE | global | N |
| ch3.cb.rate_red_pct | CB 率紅色警報 | 1.0 | PERCENTAGE | global | N |
| ch3.cb.single_day_alert_count | 單日 CB 警報 | 10 | COUNT | global | N |

#### 3DS / SCA

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch3.sca.strong_auth_gbp | 強認證閾值 (GBP) | 30 | AMOUNT (£) | jurisdiction | Y |
| ch3.sca.cumulative_gbp | 累計閾值 (GBP) | 100 | AMOUNT (£) | jurisdiction | Y |
| ch3.crypto.large_deposit_threshold | 加密大額存款閾值 | 10,000 | AMOUNT ($) | global | N |

### 6.4 Ch4 — 遊戲整合 (Game Integration)

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch4.gp.integration_days | GP 技術對接時限 | 5 | DURATION (days) | global | N |
| ch4.token.validity_min | Token 有效期 | 5 | DURATION (min) | global | N |
| ch4.lobby.ctr_target_pct | 大廳 CTR 目標 | 8 | PERCENTAGE | global | N |
| ch4.lobby.conversion_target_pct | 遊戲轉化率目標 | 12 | PERCENTAGE | global | N |
| ch4.lobby.cdn_hit_rate_pct | CDN 命中率目標 | 98 | PERCENTAGE | global | N |
| ch4.lobby.load_time_sec | 遊戲載入時間目標 | 3 | DURATION (sec) | global | N |
| ch4.rtp.warning_pct | RTP 警告閾值 | 120 | PERCENTAGE | global | N |
| ch4.rtp.warning_loss_amount | RTP 警告虧損額 | 5,000 | AMOUNT ($) | global | N |
| ch4.rtp.critical_pct | RTP 嚴重閾值 | 200 | PERCENTAGE | global | N |
| ch4.rtp.critical_loss_amount | RTP 嚴重虧損額 | 10,000 | AMOUNT ($) | global | N |
| ch4.rtp.monitor_window_min | RTP 監控窗口 | 5 | DURATION (min) | global | N |
| ch4.rtp.gli19_deviation_pct | GLI-19 偏差限制 | 0.5 | PERCENTAGE | global | Y |
| ch4.ukgc.slots_bet_18_24 | UKGC Slots 投注上限 (18-24) | 2 | AMOUNT (£) | jurisdiction | Y |
| ch4.ukgc.slots_bet_25plus | UKGC Slots 投注上限 (25+) | 5 | AMOUNT (£) | jurisdiction | Y |
| ch4.ukgc.auto_spin_interval_sec | 自動旋轉間隔 | 2.5 | DURATION (sec) | jurisdiction | Y |
| ch4.gp.maintenance_notice_hr | 維護提前通知 | 48 | DURATION (hr) | global | N |
| ch4.gp.maintenance_player_notice_hr | 玩家維護通知 | 1 | DURATION (hr) | global | N |
| ch4.gp.maintenance_bet_freeze_min | 維護投注凍結 | 30 | DURATION (min) | global | N |
| ch4.gp.violation_warning_days | 違約警告期 | 7 | DURATION (days) | global | N |
| ch4.gp.throttling_observation_days | 限流觀察期 | 14 | DURATION (days) | global | N |
| ch4.gp.settlement_delay_alert_hr | 結算延遲警報 | 24 | DURATION (hr) | global | N |
| ch4.gp.bankruptcy_win_comp_hr | 破產墊付 SLA | 24 | DURATION (hr) | global | N |
| ch4.gp.bankruptcy_freespin_comp_hr | 破產免費旋轉補償 SLA | 48 | DURATION (hr) | global | N |
| ch4.gp.bankruptcy_player_notice_hr | 破產玩家通知 SLA | 2 | DURATION (hr) | global | N |
| ch4.gp.bankruptcy_regulatory_notice_hr | 破產監管通知 SLA | 24 | DURATION (hr) | global | N |
| ch4.gp.risk_reserve_pct | 風險準備金比例 | 5 | PERCENTAGE | global | N |

### 6.5 Ch5 — 促銷與 VIP (Promotions & VIP)

#### 紅利參數

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch5.bonus.welcome_min_multiplier | 歡迎紅利最低倍數 | 25 | COUNT | brand | N |
| ch5.bonus.welcome_max_multiplier | 歡迎紅利最高倍數 | 40 | COUNT | brand | N |
| ch5.bonus.welcome_max_cap | 歡迎紅利上限 | 500 | AMOUNT ($) | brand | N |
| ch5.bonus.max_simultaneous | 同時活躍紅利上限 | 5 | COUNT | global | N |
| ch5.bonus.total_amount_cap | 紅利總額上限 | 10,000 | AMOUNT ($) | global | N |
| ch5.bonus.daily_claims_limit | 每日領取上限 | 3 | COUNT | global | N |
| ch5.bonus.referral_kyc_level | 推薦紅利 KYC 要求 | 1 | THRESHOLD | global | N |
| ch5.bonus.referral_30day_cap | 推薦 30 天上限 | 10 | COUNT | global | N |
| ch5.bonus.referral_clawback_days | 推薦回收窗口 | 90 | DURATION (days) | global | N |
| ch5.bonus.ukgc_max_wagering | UKGC 最大流水倍數 | 10 | COUNT | jurisdiction | Y |
| ch5.bonus.ukgc_slots_bet_25plus | UKGC Slots 投注上限 (25+) | 5 | AMOUNT (£) | jurisdiction | Y |
| ch5.bonus.ukgc_slots_bet_18_24 | UKGC Slots 投注上限 (18-24) | 2 | AMOUNT (£) | jurisdiction | Y |

#### 流水貢獻率

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch5.wagering.slots_contribution_pct | Slots 流水貢獻率 | 100 | PERCENTAGE | brand | N |
| ch5.wagering.sports_contribution_pct | Sports 流水貢獻率 | 50 | PERCENTAGE | brand | N |
| ch5.wagering.baccarat_contribution_pct | 百家樂流水貢獻率 | 10 | PERCENTAGE | brand | N |
| ch5.wagering.blackjack_contribution_pct | 21 點流水貢獻率 | 10 | PERCENTAGE | brand | N |
| ch5.wagering.roulette_contribution_pct | 輪盤流水貢獻率 | 20 | PERCENTAGE | brand | N |
| ch5.wagering.poker_contribution_pct | 撲克流水貢獻率 | 5 | PERCENTAGE | brand | N |

#### VIP 返現 / 返佣

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch5.vip.bronze_cashback_pct | Bronze 返現率 | 5 | PERCENTAGE | brand | N |
| ch5.vip.silver_cashback_pct | Silver 返現率 | 10 | PERCENTAGE | brand | N |
| ch5.vip.gold_cashback_pct | Gold 返現率 | 15 | PERCENTAGE | brand | N |
| ch5.vip.platinum_cashback_pct | Platinum 返現率 | 20 | PERCENTAGE | brand | N |
| ch5.vip.diamond_cashback_pct | Diamond 返現率 | 25 | PERCENTAGE | brand | N |
| ch5.vip.bronze_wagering_rebate_pct | Bronze 流水返佣率 | 0.1 | PERCENTAGE | brand | N |
| ch5.vip.silver_wagering_rebate_pct | Silver 流水返佣率 | 0.3 | PERCENTAGE | brand | N |
| ch5.vip.gold_wagering_rebate_pct | Gold 流水返佣率 | 0.5 | PERCENTAGE | brand | N |
| ch5.vip.platinum_wagering_rebate_pct | Platinum 流水返佣率 | 0.8 | PERCENTAGE | brand | N |
| ch5.vip.diamond_wagering_rebate_pct | Diamond 流水返佣率 | 1.2 | PERCENTAGE | brand | N |

#### 反欺詐

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch5.fraud.hunter_flag_score | 紅利獵人 FLAG 分數 | 60 | THRESHOLD | global | N |
| ch5.fraud.hunter_block_score | 紅利獵人 BLOCK 分數 | 90 | THRESHOLD | global | N |
| ch5.fraud.hedge_bet_threshold | 對沖投注閾值 | 1,000 | AMOUNT ($) | global | N |
| ch5.fraud.valid_bet_rate_alert_pct | 有效投注率警報 | 20 | PERCENTAGE | global | N |
| ch5.fraud.multi_account_device_limit | 同設備多帳戶上限 | 3 | COUNT | global | N |

### 6.6 Ch6 — 風控與合規 (Risk & Compliance)

#### 風險評分

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.risk.auto_approve_max | AUTO_APPROVE 上界 | 30 | THRESHOLD | global | N |
| ch6.risk.manual_review_max | MANUAL_REVIEW 上界 | 70 | THRESHOLD | global | N |
| ch6.risk.auto_reject_min | AUTO_REJECT 下界 | 70 | THRESHOLD | global | N |
| ch6.risk.layer1_max_latency_ms | Layer 1 最大延遲 | 10 | DURATION (ms) | global | N |
| ch6.risk.layer3_async_latency_sec | Layer 3 異步延遲 | 5 | DURATION (sec) | global | N |
| ch6.risk.composite_medium_escalation | 複合中風險升級數 | 3 | COUNT | global | N |
| ch6.risk.composite_high_escalation | 複合高風險升級數 | 2 | COUNT | global | N |
| ch6.risk.ml_rule_weight_pct | ML 規則權重 | 60 | PERCENTAGE | global | N |
| ch6.risk.ml_behavior_weight_pct | ML 行為權重 | 40 | PERCENTAGE | global | N |
| ch6.risk.ml_shadow_mode_days | ML 影子模式天數 | 30 | DURATION (days) | global | N |

#### 投注偵測

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.detect.high_freq_per_min | 高頻投注閾值 | 10 | COUNT/min | global | N |
| ch6.detect.low_odds_threshold | 低賠率閾值 | 1.5 | THRESHOLD | global | N |
| ch6.detect.bot_mouse_straight_pct | 機器人滑鼠直線比 | 90 | PERCENTAGE | global | N |
| ch6.detect.bot_click_std_ms | 機器人點擊標準差 | 50 | DURATION (ms) | global | N |
| ch6.detect.bot_decision_time_ms | 機器人決策時間 | 200 | DURATION (ms) | global | N |
| ch6.detect.bot_session_duration_hr | 機器人連續時長 | 12 | DURATION (hr) | global | N |
| ch6.detect.bot_flag_monitoring_days | 機器人旗標監控期 | 7 | DURATION (days) | global | N |
| ch6.detect.bot_flag_escalation_count | 機器人升級旗標數 | 3 | COUNT | global | N |

#### VIP SLA

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.vip.diamond_review_sla_min | Diamond 人工審核 SLA | 15 | DURATION (min) | brand | N |
| ch6.vip.diamond_withdrawal_sla_min | Diamond 出金審核 SLA | 30 | DURATION (min) | brand | N |
| ch6.vip.platinum_review_sla_min | Platinum 人工審核 SLA | 30 | DURATION (min) | brand | N |
| ch6.vip.platinum_withdrawal_sla_hr | Platinum 出金審核 SLA | 1 | DURATION (hr) | brand | N |
| ch6.vip.gold_review_sla_hr | Gold 人工審核 SLA | 1 | DURATION (hr) | brand | N |
| ch6.vip.gold_withdrawal_sla_hr | Gold 出金審核 SLA | 2 | DURATION (hr) | brand | N |

#### 出金風控優先級

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.withdrawal.urgent_threshold | URGENT 閾值 | 10,000 | AMOUNT ($) | jurisdiction | N |
| ch6.withdrawal.urgent_sla_hr | URGENT SLA | 1 | DURATION (hr) | global | N |
| ch6.withdrawal.high_threshold | HIGH 閾值 | 5,000 | AMOUNT ($) | jurisdiction | N |
| ch6.withdrawal.high_sla_hr | HIGH SLA | 2 | DURATION (hr) | global | N |
| ch6.withdrawal.medium_threshold | MEDIUM 閾值 | 1,000 | AMOUNT ($) | jurisdiction | N |
| ch6.withdrawal.medium_sla_hr | MEDIUM SLA | 24 | DURATION (hr) | global | N |
| ch6.withdrawal.low_sla_hr | LOW SLA | 48 | DURATION (hr) | global | N |
| ch6.withdrawal.sla_monitor_min | SLA 監控頻率 | 5 | DURATION (min) | global | N |
| ch6.withdrawal.sla_warning_pct | SLA 預警閾值 | 75 | PERCENTAGE | global | N |

#### AML 合規

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.aml.cdd_trigger_eur | CDD 觸發金額 (EUR) | 2,000 | AMOUNT (€) | jurisdiction | Y |
| ch6.aml.edd_single_eur | EDD 單筆觸發 (EUR) | 10,000 | AMOUNT (€) | jurisdiction | Y |
| ch6.aml.edd_cumulative_eur | EDD 累計觸發 (EUR) | 50,000 | AMOUNT (€) | jurisdiction | Y |
| ch6.aml.pep_high_confidence_pct | PEP 高信賴匹配 | 95 | PERCENTAGE | global | Y |
| ch6.aml.pep_fuzzy_min_pct | PEP 模糊匹配下界 | 70 | PERCENTAGE | global | Y |
| ch6.aml.pep_fuzzy_review_sla_hr | PEP 模糊審核 SLA | 24 | DURATION (hr) | global | Y |
| ch6.aml.sar_uk_days | SAR 提交 SLA (UK) | 7 | DURATION (days) | jurisdiction | Y |
| ch6.aml.sar_malta_days | SAR 提交 SLA (Malta) | 15 | DURATION (days) | jurisdiction | Y |
| ch6.aml.sar_gibraltar_days | SAR 提交 SLA (Gibraltar) | 7 | DURATION (days) | jurisdiction | Y |
| ch6.aml.sar_curacao_days | SAR 提交 SLA (Curaçao) | 30 | DURATION (days) | jurisdiction | Y |
| ch6.aml.sar_philippines_days | SAR 提交 SLA (Philippines) | 5 | DURATION (days) | jurisdiction | Y |

#### 負擔能力評估 (UKGC)

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.afford.warning_min_gbp | 警告觸發下限 | 125 | AMOUNT (£) | jurisdiction | Y |
| ch6.afford.warning_max_gbp | 警告觸發上限 | 500 | AMOUNT (£) | jurisdiction | Y |
| ch6.afford.declaration_min_gbp | 自我聲明觸發下限 | 500 | AMOUNT (£) | jurisdiction | Y |
| ch6.afford.declaration_max_gbp | 自我聲明觸發上限 | 2,000 | AMOUNT (£) | jurisdiction | Y |
| ch6.afford.verification_min_gbp | 第三方驗證觸發 | 2,000 | AMOUNT (£) | jurisdiction | Y |
| ch6.afford.net_loss_90day_gbp | 90天淨虧損觸發 | 2,000 | AMOUNT (£) | jurisdiction | Y |

#### 自我排除逃避偵測

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch6.exclusion.levenshtein_distance | 名字相似度距離 | 2 | COUNT | global | Y |
| ch6.exclusion.fuzzy_match_pct | 模糊匹配閾值 | 70 | PERCENTAGE | global | Y |
| ch6.exclusion.manual_review_sla_hr | 人工審核 SLA | 24 | DURATION (hr) | global | Y |

### 6.7 Ch7 — 治理與牌照 (Governance & Licensing)

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch7.tenant.isolation_overhead_pct | 多租戶隔離開銷上限 | 5 | PERCENTAGE | global | N |
| ch7.billing.grace_period_days | 帳單寬限期 | 7 | DURATION (days) | brand | N |
| ch7.billing.overdue_warning_days | 逾期警告天數 | 7 | DURATION (days) | brand | N |
| ch7.billing.overdue_highrisk_days | 逾期高風險天數 | 14 | DURATION (days) | brand | N |
| ch7.billing.overdue_stop_reg_days | 逾期停止註冊天數 | 30 | DURATION (days) | brand | N |
| ch7.billing.overdue_suspend_days | 逾期停服天數 | 31 | DURATION (days) | brand | N |
| ch7.security.mfa_recovery_sla_hr | MFA 恢復 SLA | 48 | DURATION (hr) | global | N |
| ch7.audit.mga_retention_years | MGA 審計保留年限 | 5 | DURATION (years) | jurisdiction | Y |
| ch7.audit.financial_retention_years | 財務審計保留年限 | 7 | DURATION (years) | jurisdiction | Y |
| ch7.tenant.onboarding_sla_hr | 新租戶上線 SLA | 24 | DURATION (hr) | global | N |

### 6.8 Ch8 — 代理營運 (Agent Operations)

#### 代理層級與信用

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch8.agent.max_levels | 最大代理層級 | 10 | COUNT | global | N |
| ch8.credit.warning_usage_pct | 信用警告使用率 | 80 | PERCENTAGE | global | N |
| ch8.credit.high_risk_usage_pct | 高風險使用率 | 90 | PERCENTAGE | global | N |
| ch8.credit.severe_usage_pct | 嚴重使用率 | 100 | PERCENTAGE | global | N |

#### 佣金

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch8.commission.tier1_threshold | Tier1 門檻 | 10,000 | AMOUNT ($) | brand | N |
| ch8.commission.tier1_rate_pct | Tier1 分成比例 | 30 | PERCENTAGE | brand | N |
| ch8.commission.tier2_threshold | Tier2 門檻 | 50,000 | AMOUNT ($) | brand | N |
| ch8.commission.tier2_rate_pct | Tier2 分成比例 | 35 | PERCENTAGE | brand | N |
| ch8.commission.tier3_threshold | Tier3 門檻 | 200,000 | AMOUNT ($) | brand | N |
| ch8.commission.tier3_rate_pct | Tier3 分成比例 | 40 | PERCENTAGE | brand | N |
| ch8.commission.tier4_rate_pct | Tier4 分成比例 | 45 | PERCENTAGE | brand | N |
| ch8.turnover.slots_pct | Slots 流水返佣 | 0.5 | PERCENTAGE | brand | N |
| ch8.turnover.live_casino_pct | Live Casino 流水返佣 | 0.3 | PERCENTAGE | brand | N |
| ch8.turnover.sports_pct | Sports 流水返佣 | 0.4 | PERCENTAGE | brand | N |
| ch8.turnover.table_games_pct | Table Games 流水返佣 | 0.2 | PERCENTAGE | brand | N |
| ch8.settlement.negative_rollover_threshold | 負結轉重置門檻 | -1,000,000 | AMOUNT ($) | brand | N |

#### 結算審批

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch8.settlement.auto_max | 自動發放上限 | 10,000 | AMOUNT ($) | brand | N |
| ch8.settlement.manager_approval_max | Manager 審批上限 | 50,000 | AMOUNT ($) | brand | N |
| ch8.settlement.manager_sla_hr | Manager 審批 SLA | 4 | DURATION (hr) | brand | N |
| ch8.settlement.cfo_approval_max | CFO 審批上限 | 100,000 | AMOUNT ($) | brand | N |
| ch8.settlement.cfo_sla_hr | CFO 審批 SLA | 24 | DURATION (hr) | brand | N |
| ch8.settlement.dual_sla_hr | 雙重審批 SLA | 48 | DURATION (hr) | brand | N |
| ch8.settlement.monthly_escalation | 月度累計升級閾值 | 500,000 | AMOUNT ($) | brand | N |
| ch8.settlement.weekly_growth_alert_pct | 週結算增長警報 | 200 | PERCENTAGE | brand | N |
| ch8.settlement.new_agent_first_month_flag | 新代理首月旗標 | 50,000 | AMOUNT ($) | brand | N |

#### 帳戶安全

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch8.security.ip_whitelist_max | IP 白名單上限 | 5 | COUNT | global | N |
| ch8.security.idle_timeout_min | 閒置超時 | 30 | DURATION (min) | global | N |
| ch8.security.concurrent_sessions | 同時登入數 | 1 | COUNT | global | N |
| ch8.security.lockout_5_attempts_min | 5 次失敗鎖定 | 15 | DURATION (min) | global | N |
| ch8.security.lockout_10_attempts_hr | 10 次失敗鎖定 | 1 | DURATION (hr) | global | N |
| ch8.security.audit_retention_years | 審計保留年限 | 3 | DURATION (years) | global | Y |

### 6.9 Ch9 — 分析與報表 (Analytics & Reporting)

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch9.hot.latency_sec | Hot Path 延遲 | 5 | DURATION (sec) | global | N |
| ch9.hot.accuracy_pct | Hot Path 準確度 | 95 | PERCENTAGE | global | N |
| ch9.warm.latency_max_sec | Warm Path 最大延遲 | 60 | DURATION (sec) | global | N |
| ch9.warm.accuracy_pct | Warm Path 準確度 | 98 | PERCENTAGE | global | N |
| ch9.dashboard.availability_pct | 即時儀表板可用率 | 99.9 | PERCENTAGE | global | N |
| ch9.daily.availability_pct | 日報可用率 | 99.5 | PERCENTAGE | global | N |
| ch9.export.small_report_rows | 同步匯出行數上限 | 100,000 | COUNT | global | N |
| ch9.export.user_rate_per_min | 用戶匯出速率 | 10 | COUNT/min | global | N |
| ch9.export.tenant_rate_per_min | 租戶匯出速率 | 100 | COUNT/min | global | N |
| ch9.export.concurrent_per_user | 用戶並行匯出數 | 5 | COUNT | global | N |
| ch9.correction.minor_threshold_pct | 修正重要性閾值 (輕微) | 0.1 | PERCENTAGE | global | Y |
| ch9.correction.regulatory_notice_days | 監管通知 SLA | 5 | DURATION (days) | global | Y |

### 6.10 Ch10 — 基礎設施 (Infrastructure)

#### SLA 與 DR

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch10.sla.tier1_pct | Tier 1 SLA | 99.9 | PERCENTAGE | global | N |
| ch10.sla.tier2_pct | Tier 2 SLA | 99.95 | PERCENTAGE | global | N |
| ch10.sla.tier3_pct | Tier 3 SLA | 99.99 | PERCENTAGE | global | N |
| ch10.maintenance.advance_notice_hr | 維護提前通知 | 72 | DURATION (hr) | global | N |
| ch10.maintenance.emergency_notice_hr | 緊急維護通知 | 24 | DURATION (hr) | global | N |
| ch10.dr.tier1_rpo_min | Tier 1 RPO | 5 | DURATION (min) | global | N |
| ch10.dr.tier1_rto_min | Tier 1 RTO | 15 | DURATION (min) | global | N |
| ch10.dr.tier2_rpo_hr | Tier 2 RPO | 1 | DURATION (hr) | global | N |
| ch10.dr.tier2_rto_hr | Tier 2 RTO | 4 | DURATION (hr) | global | N |
| ch10.dr.tier3_rpo_hr | Tier 3 RPO | 24 | DURATION (hr) | global | N |
| ch10.dr.tier3_rto_hr | Tier 3 RTO | 24 | DURATION (hr) | global | N |
| ch10.dr.drill_frequency | DR 演練頻率 | quarterly | ENUM | global | N |

#### 性能目標

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch10.perf.peak_tps | 尖峰 TPS 目標 | 120 | COUNT | global | N |
| ch10.perf.design_capacity_tps | 設計容量 TPS | 300 | COUNT | global | N |
| ch10.perf.balance_query_p95_ms | 餘額查詢 P95 | 50 | DURATION (ms) | global | N |
| ch10.perf.balance_query_p99_ms | 餘額查詢 P99 | 100 | DURATION (ms) | global | N |
| ch10.perf.bet_debit_p95_ms | 投注扣款 P95 | 100 | DURATION (ms) | global | N |
| ch10.perf.bet_debit_p99_ms | 投注扣款 P99 | 150 | DURATION (ms) | global | N |
| ch10.perf.game_launch_p95_ms | 遊戲啟動 P95 | 200 | DURATION (ms) | global | N |
| ch10.perf.game_launch_p99_ms | 遊戲啟動 P99 | 500 | DURATION (ms) | global | N |
| ch10.perf.api_p95_ms | 通用 API P95 | 100 | DURATION (ms) | global | N |
| ch10.perf.api_p99_ms | 通用 API P99 | 200 | DURATION (ms) | global | N |

#### 數據保留

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch10.retention.betting_years | 投注記錄保留 | 2 | DURATION (years) | global | Y |
| ch10.retention.audit_years | 審計日誌保留 | 1 | DURATION (years) | global | Y |
| ch10.retention.financial_years | 財務記錄保留 | 7 | DURATION (years) | jurisdiction | Y |

#### 告警回應

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch10.alert.p0_response_min | P0 告警回應 | 5 | DURATION (min) | global | N |
| ch10.alert.p1_response_min | P1 告警回應 | 15 | DURATION (min) | global | N |
| ch10.alert.p2_response_hr | P2 告警回應 | 1 | DURATION (hr) | global | N |
| ch10.alert.p3_response_hr | P3 告警回應 | 24 | DURATION (hr) | global | N |

### 6.11 Ch11 — 前端 (Frontend)

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch11.push.max_per_day | 每日推送上限 | 5 | COUNT | global | N |
| ch11.push.quiet_start | 推送靜音開始 | 23:00 | DURATION | global | N |
| ch11.push.quiet_end | 推送靜音結束 | 09:00 | DURATION | global | N |
| ch11.perf.lcp_target_sec | LCP 目標 | 2.5 | DURATION (sec) | global | N |
| ch11.perf.fcp_target_sec | FCP 目標 | 2 | DURATION (sec) | global | N |
| ch11.perf.cls_target | CLS 目標 | 0.1 | THRESHOLD | global | N |
| ch11.perf.lighthouse_score | Lighthouse 分數目標 | 0.9 | THRESHOLD | global | N |
| ch11.cache.cdn_ttl_sec | CDN 快取 TTL | 60 | DURATION (sec) | global | N |
| ch11.cache.origin_ttl_sec | Origin 快取 TTL | 3600 | DURATION (sec) | global | N |
| ch11.cache.api_ttl_sec | API 快取 TTL | 300 | DURATION (sec) | global | N |
| ch11.asset.desktop_banner_kb | 桌面 Banner 大小限制 | 200 | AMOUNT (KB) | global | N |
| ch11.asset.mobile_banner_kb | 手機 Banner 大小限制 | 150 | AMOUNT (KB) | global | N |
| ch11.asset.popup_max_kb | Popup 大小限制 | 100 | AMOUNT (KB) | global | N |
| ch11.apk.max_size_mb | APK 大小限制 (SE Asia) | 5 | AMOUNT (MB) | jurisdiction | N |

### 6.12 Ch12 — 客戶服務 (Customer Service)

#### 管道 SLA

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch12.chat.response_sla_min | Live Chat 回應 SLA | 5 | DURATION (min) | brand | N |
| ch12.chat.ai_automation_pct | Chat AI 自動化率 | 40 | PERCENTAGE | global | N |
| ch12.telegram.response_sla_min | Telegram 回應 SLA | 15 | DURATION (min) | brand | N |
| ch12.email.response_sla_hr | Email 回應 SLA | 2 | DURATION (hr) | brand | N |
| ch12.whatsapp.response_sla_min | WhatsApp 回應 SLA | 15 | DURATION (min) | brand | N |
| ch12.chat.concurrent_per_agent | 每客服並發數 | 5 | COUNT | global | N |

#### AI 信心度

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch12.ai.deposit_confidence | 存款意圖信心度 | 0.8 | THRESHOLD | global | N |
| ch12.ai.withdrawal_confidence | 出金意圖信心度 | 0.9 | THRESHOLD | global | N |
| ch12.ai.bonus_confidence | 紅利查詢信心度 | 0.85 | THRESHOLD | global | N |
| ch12.ai.game_issue_confidence | 遊戲問題信心度 | 0.75 | THRESHOLD | global | N |
| ch12.ai.password_confidence | 密碼重置信心度 | 0.95 | THRESHOLD | global | N |
| ch12.ai.complaint_confidence | 投訴意圖信心度 | 0.7 | THRESHOLD | global | N |
| ch12.ai.low_confidence_threshold | 低信心度閾值 | 0.7 | THRESHOLD | global | N |
| ch12.ai.failed_attempts_transfer | 失敗轉人工次數 | 3 | COUNT | global | N |

#### VIP 客服 SLA

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch12.vip.diamond_first_response_min | Diamond 首次回應 | 5 | DURATION (min) | brand | N |
| ch12.vip.diamond_resolution_hr | Diamond 解決 SLA | 2 | DURATION (hr) | brand | N |
| ch12.vip.platinum_first_response_min | Platinum 首次回應 | 15 | DURATION (min) | brand | N |
| ch12.vip.platinum_resolution_hr | Platinum 解決 SLA | 4 | DURATION (hr) | brand | N |
| ch12.vip.gold_first_response_min | Gold 首次回應 | 30 | DURATION (min) | brand | N |
| ch12.vip.gold_resolution_hr | Gold 解決 SLA | 8 | DURATION (hr) | brand | N |
| ch12.vip.standard_first_response_hr | 一般玩家首次回應 | 2 | DURATION (hr) | brand | N |
| ch12.vip.standard_resolution_hr | 一般玩家解決 SLA | 24 | DURATION (hr) | brand | N |

#### SLA 違規補償

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch12.compensation.default_credit | SLA 違規預設補償 | 5 | AMOUNT ($) | brand | N |
| ch12.compensation.monthly_player_cap | 玩家月度補償上限 | 50 | AMOUNT ($) | brand | N |
| ch12.compensation.diamond_multiplier | Diamond 補償倍率 | 2 | COUNT | brand | N |
| ch12.ticket.pending_auto_close_hr | 待回應自動關閉 | 72 | DURATION (hr) | global | N |
| ch12.ticket.resolved_auto_close_hr | 已解決自動關閉 | 24 | DURATION (hr) | global | N |
| ch12.agent.max_tickets | 每客服最大工單數 | 20 | COUNT | global | N |

#### KPI

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch12.kpi.fcr_target_pct | FCR 目標 | 80 | PERCENTAGE | global | N |
| ch12.kpi.aht_target_min | AHT 目標 | 15 | DURATION (min) | global | N |
| ch12.kpi.csat_target_pct | CSAT 目標 | 90 | PERCENTAGE | global | N |
| ch12.kpi.sla_achievement_pct | SLA 達成率目標 | 95 | PERCENTAGE | global | N |
| ch12.kpi.reopen_rate_pct | 重開率目標 | 5 | PERCENTAGE | global | N |

### 6.13 Ch13 — 安全 (Security)

#### GDPR

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch13.gdpr.sar_sla_days | SAR 回應 SLA | 30 | DURATION (days) | jurisdiction | Y |
| ch13.gdpr.sar_extended_days | SAR 延長期 | 90 | DURATION (days) | jurisdiction | Y |
| ch13.gdpr.deletion_cooloff_days | 刪除冷靜期 | 30 | DURATION (days) | jurisdiction | Y |
| ch13.gdpr.deletion_confirm_link_days | 確認連結有效期 | 7 | DURATION (days) | jurisdiction | Y |
| ch13.gdpr.crypto_shredding_day | 加密銷毀執行日 | 37 | DURATION (days) | jurisdiction | Y |

#### 數據保留

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch13.retention.aml_min_years | AML 最低保留 | 5 | DURATION (years) | jurisdiction | Y |
| ch13.retention.aml_max_years | AML 最高保留 | 7 | DURATION (years) | jurisdiction | Y |
| ch13.retention.tax_years | 稅務記錄保留 | 7 | DURATION (years) | jurisdiction | Y |
| ch13.retention.aml_investigation_years | AML 調查暫停 | 1 | DURATION (years) | jurisdiction | Y |
| ch13.retention.litigation_years | 訴訟暫停 | 10 | DURATION (years) | jurisdiction | Y |
| ch13.retention.pending_rollover_days | 流水暫停 | 90 | DURATION (days) | jurisdiction | Y |

#### 金鑰輪換

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch13.key.master_rotation_days | 主金鑰輪換週期 | 365 | DURATION (days) | global | Y |
| ch13.key.player_data_rotation_days | 玩家數據金鑰輪換 | 90 | DURATION (days) | global | Y |
| ch13.key.blind_index_rotation_days | Blind Index Salt 輪換 | 180 | DURATION (days) | global | Y |
| ch13.key.tls_cert_rotation_days | TLS 證書輪換 | 90 | DURATION (days) | global | Y |
| ch13.key.tls_renewal_lead_days | TLS 續約提前天數 | 30 | DURATION (days) | global | N |
| ch13.key.reencrypt_batch_size | 重加密批次大小 | 1,000 | COUNT | global | N |
| ch13.key.reencrypt_failure_pct | 重加密失敗率閾值 | 0.1 | PERCENTAGE | global | N |

### 6.14 Ch14 — 第三方整合 (Third-Party Integration)

#### 第三方 SLA

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch14.psp.min_availability_pct | PSP 最低可用率 | 99.5 | PERCENTAGE | global | N |
| ch14.psp.p99_response_sec | PSP P99 回應時間 | 3 | DURATION (sec) | global | N |
| ch14.kyc.min_availability_pct | KYC 最低可用率 | 98 | PERCENTAGE | global | N |
| ch14.kyc.p99_response_sec | KYC P99 回應時間 | 5 | DURATION (sec) | global | N |
| ch14.kyc.rate_limit_per_min | KYC 速率限制 | 100 | COUNT/min | global | N |
| ch14.gp.min_availability_pct | GP 最低可用率 | 99 | PERCENTAGE | global | N |
| ch14.email.min_availability_pct | Email 最低可用率 | 99 | PERCENTAGE | global | N |
| ch14.analytics.min_availability_pct | 分析最低可用率 | 95 | PERCENTAGE | global | N |

#### Webhook 重試

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch14.webhook.retry_delays | 重試延遲序列 (sec) | 5,10,20,40,80,160 | DURATION (sec) | global | N |
| ch14.webhook.max_retries | 最大重試次數 | 6 | COUNT | global | N |
| ch14.webhook.dedup_window_hr | 冪等去重窗口 | 24 | DURATION (hr) | global | N |
| ch14.dlq.retention_days | DLQ 保留天數 | 7 | DURATION (days) | global | N |
| ch14.dlq.alert_threshold | DLQ 警報閾值 | 100 | COUNT | global | N |

#### 金鑰輪換

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch14.key.psp_rotation_days | PSP API Key 輪換 | 90 | DURATION (days) | global | Y |
| ch14.key.internal_rotation_days | 內部 Key 輪換 | 30 | DURATION (days) | global | Y |
| ch14.key.db_password_rotation_days | DB 密碼輪換 | 180 | DURATION (days) | global | Y |
| ch14.webhook.sig_transition_days | Webhook 簽名過渡期 | 90 | DURATION (days) | global | N |
| ch14.webhook.sig_failure_alert_pct | 簽名驗證失敗警報 | 1 | PERCENTAGE | global | N |

#### 健康檢查

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch14.health.check_interval_sec | 健康檢查間隔 | 30 | DURATION (sec) | global | N |
| ch14.health.recovery_checks | 恢復連續檢查數 | 3 | COUNT | global | N |
| ch14.health.recovery_threshold_pct | 恢復成功率閾值 | 95 | PERCENTAGE | global | N |
| ch14.psp.failure_alert_pct | PSP 失敗率警報 | 5 | PERCENTAGE | global | N |
| ch14.psp.failure_alert_window_min | PSP 失敗率窗口 | 5 | DURATION (min) | global | N |
| ch14.psp.auto_failover_sec | PSP 自動切換 | 30 | DURATION (sec) | global | N |

### 6.15 Ch15 — 負責任博彩 (Responsible Gambling)

#### 自我排除

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch15.exclusion.temp_min_hr | 臨時排除最短 | 24 | DURATION (hr) | jurisdiction | Y |
| ch15.exclusion.temp_max_weeks | 臨時排除最長 | 6 | DURATION (weeks) | jurisdiction | Y |
| ch15.exclusion.medium_min_months | 中期排除最短 | 6 | DURATION (months) | jurisdiction | Y |
| ch15.exclusion.medium_max_years | 中期排除最長 | 1 | DURATION (years) | jurisdiction | Y |
| ch15.exclusion.gp_notification_sec | GP 通知時限 | 30 | DURATION (sec) | global | Y |
| ch15.exclusion.round_settlement_min | 回合結算時限 | 5 | DURATION (min) | global | Y |
| ch15.exclusion.avoidance_rate_target_pct | 逃避率目標 | 5 | PERCENTAGE | global | Y |

#### GAMSTOP

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch15.gamstop.sync_success_pct | 同步成功率目標 | 99.9 | PERCENTAGE | jurisdiction | Y |
| ch15.gamstop.hold_suspension_hr | Hold 暫停時限 | 24 | DURATION (hr) | jurisdiction | Y |
| ch15.gamstop.manual_review_sla_hr | 人工審核 SLA | 24 | DURATION (hr) | jurisdiction | Y |
| ch15.gamstop.mlro_escalation_hr | MLRO 升級時限 | 48 | DURATION (hr) | jurisdiction | Y |
| ch15.gamstop.account_suspension_hr | 帳戶暫停時限 | 72 | DURATION (hr) | jurisdiction | Y |

#### 存款限額

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch15.deposit.germany_monthly_eur | 德國月存款限額 | 1,000 | AMOUNT (€) | jurisdiction | Y |
| ch15.deposit.nl_daily_eur | 荷蘭日存款建議 | 200 | AMOUNT (€) | jurisdiction | Y |
| ch15.deposit.nl_weekly_eur | 荷蘭週存款建議 | 700 | AMOUNT (€) | jurisdiction | Y |
| ch15.deposit.nl_monthly_eur | 荷蘭月存款建議 | 2,000 | AMOUNT (€) | jurisdiction | Y |
| ch15.deposit.increase_delay_min_hr | 限額提升最短延遲 | 24 | DURATION (hr) | jurisdiction | Y |
| ch15.deposit.increase_delay_max_hr | 限額提升最長延遲 | 72 | DURATION (hr) | jurisdiction | Y |

#### 遊戲時間控制

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch15.time.uk_rapid_deposits_count | UK 快速存款次數 | 10 | COUNT | jurisdiction | Y |
| ch15.time.uk_rapid_deposits_window_hr | UK 快速存款窗口 | 24 | DURATION (hr) | jurisdiction | Y |
| ch15.time.uk_forced_break_min | UK 強制休息時長 | 60 | DURATION (min) | jurisdiction | Y |
| ch15.time.de_continuous_play_min | 德國連續遊戲上限 | 60 | DURATION (min) | jurisdiction | Y |
| ch15.time.de_forced_break_min | 德國強制休息時長 | 5 | DURATION (min) | jurisdiction | Y |
| ch15.time.reality_check_min | Reality Check 間隔 (最短) | 15 | DURATION (min) | jurisdiction | Y |
| ch15.time.reality_check_standard_min | Reality Check 間隔 (標準) | 30 | DURATION (min) | jurisdiction | Y |
| ch15.time.idle_logout_default_min | 閒置登出預設 | 30 | DURATION (min) | jurisdiction | N |
| ch15.time.idle_logout_min_min | 閒置登出最短 | 15 | DURATION (min) | jurisdiction | N |
| ch15.time.idle_logout_max_min | 閒置登出最長 | 60 | DURATION (min) | jurisdiction | N |

#### 負擔能力評估

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch15.afford.basic_min_gbp | 基礎評估下限 | 125 | AMOUNT (£) | jurisdiction | Y |
| ch15.afford.basic_max_gbp | 基礎評估上限 | 500 | AMOUNT (£) | jurisdiction | Y |
| ch15.afford.enhanced_min_gbp | 增強評估下限 | 500 | AMOUNT (£) | jurisdiction | Y |
| ch15.afford.enhanced_max_gbp | 增強評估上限 | 2,000 | AMOUNT (£) | jurisdiction | Y |
| ch15.afford.full_min_gbp | 完整評估下限 | 2,000 | AMOUNT (£) | jurisdiction | Y |
| ch15.afford.basic_validity_months | 基礎評估有效期 | 3 | DURATION (months) | jurisdiction | Y |
| ch15.afford.enhanced_validity_months | 增強評估有效期 | 6 | DURATION (months) | jurisdiction | Y |
| ch15.afford.full_validity_months | 完整評估有效期 | 12 | DURATION (months) | jurisdiction | Y |
| ch15.afford.safe_consumption_pct | 安全消費比例 | 10 | PERCENTAGE | jurisdiction | Y |
| ch15.afford.multi_person_reduction_pct | 多人家庭折減 | 20 | PERCENTAGE | jurisdiction | Y |
| ch15.afford.review_timeout_days | 審核超時天數 | 7 | DURATION (days) | jurisdiction | Y |
| ch15.afford.consecutive_failures_cooldown | 連續失敗冷卻觸發 | 3 | COUNT | jurisdiction | Y |
| ch15.afford.forced_cooldown_days | 強制冷卻期 | 14 | DURATION (days) | jurisdiction | Y |
| ch15.afford.loss_limit_alert_pct | 虧損限額預警 | 80 | PERCENTAGE | jurisdiction | Y |

### 6.16 Ch16 — 事件回應 (Incident Response)

| param_key | 參數名稱 | 預設值 | 類型 | Scope | 合規 |
|-----------|---------|--------|------|-------|------|
| ch16.incident.p0_response_min | P0 回應 SLA | 15 | DURATION (min) | global | N |
| ch16.incident.p1_response_min | P1 回應 SLA | 30 | DURATION (min) | global | N |
| ch16.incident.p2_response_hr | P2 回應 SLA | 2 | DURATION (hr) | global | N |
| ch16.incident.p3_response_hr | P3 回應 SLA | 8 | DURATION (hr) | global | N |
| ch16.incident.p0_cto_notice_min | P0 CTO 通知 SLA | 15 | DURATION (min) | global | N |
| ch16.incident.p0_ceo_notice_min | P0 CEO 通知 SLA | 30 | DURATION (min) | global | N |
| ch16.warroom.p0_update_frequency_min | P0 War Room 更新頻率 | 15 | DURATION (min) | global | N |
| ch16.warroom.p1_update_frequency_min | P1 War Room 更新頻率 | 30 | DURATION (min) | global | N |
| ch16.warroom.p1_trigger_duration_min | P1 War Room 觸發時長 | 30 | DURATION (min) | global | N |
| ch16.postmortem.p0_completion_days | P0 事後分析 SLA | 5 | DURATION (days) | global | N |
| ch16.postmortem.p1_completion_days | P1 事後分析 SLA | 10 | DURATION (days) | global | N |
| ch16.exercise.desktop_monthly | 桌面演練 (月) | 1 | COUNT | global | N |
| ch16.exercise.chaos_quarterly | 混沌工程 (季) | 1 | COUNT | global | N |
| ch16.exercise.full_dr_semiannual | 完整 DR 演練 (半年) | 1 | COUNT | global | N |
| ch16.target.p0_mttd_min | P0 MTTD 目標 | 5 | DURATION (min) | global | N |
| ch16.target.p0_mttr_hr | P0 MTTR 目標 | 1 | DURATION (hr) | global | N |
| ch16.target.p1_mttr_hr | P1 MTTR 目標 | 2 | DURATION (hr) | global | N |
| ch16.target.repeat_incident_pct | 重複事件率目標 | 5 | PERCENTAGE | global | N |
| ch16.target.repeat_window_days | 重複事件窗口 | 90 | DURATION (days) | global | N |
| ch16.target.annual_p0_max | 年度 P0 上限 | 4 | COUNT | global | N |
| ch16.target.postmortem_action_completion_pct | 事後行動完成率 | 95 | PERCENTAGE | global | N |
| ch16.runbook.update_frequency_quarterly | Runbook 更新頻率 (季) | 4 | COUNT | global | N |
| ch16.runbook.access_time_sec | Runbook 存取時間目標 | 30 | DURATION (sec) | global | N |

---

## 7. Summary Statistics — 統計摘要

### 7.1 按章節分佈

| 章節 | 參數數量 | 佔比 |
|------|---------|------|
| Ch1 玩家管理 | ~75 | 9% |
| Ch2 錢包系統 | ~50 | 6% |
| Ch3 支付系統 | ~45 | 5% |
| Ch4 遊戲整合 | ~27 | 3% |
| Ch5 促銷與 VIP | ~55 | 7% |
| Ch6 風控與合規 | ~90 | 11% |
| Ch7 治理與牌照 | ~10 | 1% |
| Ch8 代理營運 | ~45 | 5% |
| Ch9 分析與報表 | ~15 | 2% |
| Ch10 基礎設施 | ~45 | 5% |
| Ch11 前端 | ~15 | 2% |
| Ch12 客戶服務 | ~60 | 7% |
| Ch13 安全 | ~20 | 2% |
| Ch14 第三方整合 | ~40 | 5% |
| Ch15 負責任博彩 | ~100 | 12% |
| Ch16 事件回應 | ~25 | 3% |
| **合計** | **~717** | **100%** |

### 7.2 按類型分佈

| 類型 | 數量 | 佔比 |
|------|------|------|
| DURATION | ~350 | 49% |
| AMOUNT | ~130 | 18% |
| PERCENTAGE | ~120 | 17% |
| COUNT | ~70 | 10% |
| THRESHOLD | ~47 | 6% |

### 7.3 按 Scope 分佈

| Scope | 數量 | 佔比 |
|-------|------|------|
| Global | ~450 | 63% |
| Brand | ~120 | 17% |
| Jurisdiction | ~147 | 20% |

### 7.4 合規驅動參數

| 標記 | 數量 | 佔比 |
|------|------|------|
| compliance_driven = true | ~150 | 21% |
| compliance_driven = false | ~567 | 79% |

---

## 8. User Stories

### 營運人員

- As a **Brand Manager**, I want to adjust VIP tier thresholds for my brand so that I can differentiate my brand's loyalty program without engineering support.
- As a **Compliance Officer**, I want to update jurisdiction-specific deposit limits when regulations change so that we remain compliant without code deployments.
- As a **Risk Manager**, I want to modify fraud detection thresholds in real-time so that I can respond to emerging threat patterns immediately.

### 技術團隊

- As a **Backend Developer**, I want to read configuration values from a unified API so that I don't need to maintain hardcoded constants.
- As a **DevOps Engineer**, I want configuration changes to propagate within 5 seconds so that operational adjustments take effect immediately.
- As a **DBA**, I want all configuration changes audited with old/new values so that we can trace any regulatory or business logic change.

### 管理層

- As a **CFO**, I want to review and approve changes to financial thresholds (settlement amounts, FX rates) before they take effect so that financial controls remain intact.
- As a **CEO**, I want a dashboard showing all active configuration overrides per brand/jurisdiction so that I understand how each market operates differently.

---

## 9. Requirements

### P0 — Must-Have

| # | 需求 | 驗收條件 |
|---|------|---------|
| R1 | DB schema 支援三層覆蓋 | Global/Brand/Jurisdiction 值可獨立設定 |
| R2 | Override 解析邏輯正確 | 單元測試覆蓋所有解析場景 (含 strictest-rule) |
| R3 | 完整審計日誌 | 每筆變更記錄 who/when/old_value/new_value/reason |
| R4 | Hot-reload ≤ 5 秒 | 配置變更後 5 秒內所有服務生效 |
| R5 | Maker-Checker 審批 | 合規參數變更需雙人審批 |
| R6 | 數據遷移腳本 | 現有硬編碼值全部遷移至 config_param_definition |
| R7 | 參數驗證 | min/max 邊界檢查，防止輸入非法值 |

### P1 — Should-Have

| # | 需求 | 驗收條件 |
|---|------|---------|
| R8 | 管理後台 UI | 非工程師可瀏覽/搜尋/修改配置 |
| R9 | 批量匯入/匯出 | 支援 CSV/Excel 批量修改配置 |
| R10 | 配置版本歷史 | 可查看任何參數的完整歷史變更 |
| R11 | 配置比較 | 可比較不同 Brand/Jurisdiction 的配置差異 |
| R12 | 定時生效 | 支援設定未來生效時間 (effective_from) |

### P2 — Future

| # | 需求 | 說明 |
|---|------|------|
| R13 | 配置回滾 | 一鍵回滾到任意歷史版本 |
| R14 | 影響分析 | 修改參數前顯示影響的玩家/交易數量 |
| R15 | A/B 測試整合 | 與實驗平台整合，支援配置 A/B 測試 |

---

## 10. Success Metrics

### Leading Indicators (上線 30 天內)

| 指標 | 目標 | 衡量方式 |
|------|------|---------|
| 參數遷移完成率 | 100% (717/717) | 自動化測試掃描 |
| Hot-reload 延遲 P99 | ≤ 5 秒 | 監控系統 |
| 審計日誌完整性 | 100% | 抽樣比對 |
| 配置 API 可用率 | 99.99% | SLA 監控 |

### Lagging Indicators (上線 90 天後)

| 指標 | 目標 | 衡量方式 |
|------|------|---------|
| 配置變更部署時間 | < 5 分鐘 (vs 原本數小時) | 變更記錄 |
| 合規事件因硬編碼導致的 | 0 件 | 合規報告 |
| 營運自助配置修改佔比 | ≥ 80% | 後台統計 |

---

## 11. Open Questions

| # | 問題 | 負責人 | 阻塞性 |
|---|------|--------|--------|
| Q1 | FX 波動吸收比例 (法幣 0.5% / 加密 2%) 是否確認？ | Ron (CFO) | 非阻塞 — 已寫入建議值 |
| Q2 | 代理大額結算門檻 ($10K/$50K/$100K) 是否確認？ | Ron | 非阻塞 — 已寫入建議值 |
| Q3 | GAP-5 第三方資料駐留合規的具體要求？ | 合規團隊 | 非阻塞 |
| Q4 | GAP-8 報表修正重要性閾值 (0.1% GGR) 是否確認？ | 合規團隊 | 非阻塞 |
| Q5 | GAP-10 GP 破產免費旋轉補償公式？ | 法務團隊 | 非阻塞 |
| Q6 | 配置變更是否需要額外的 RBAC 角色定義？ | 工程團隊 | 非阻塞 |

---

## 12. Implementation Phasing

| Phase | 範圍 | 時程 |
|-------|------|------|
| Phase 1 | DB schema + 遷移腳本 + Core API + Hot-reload | Sprint v2.3 |
| Phase 2 | 管理後台 UI + Maker-Checker 審批流程 | Sprint v2.4 |
| Phase 3 | 批量匯入/匯出 + 配置比較 + 定時生效 | Sprint v2.5 |
| Phase 4 | 影響分析 + A/B 測試整合 | Phase 3+ |

---

## 13. Appendix: param_key Naming Convention

```
{chapter}.{domain}.{specific_parameter}

Examples:
  ch1.kyc.l0_daily_deposit         → Ch1 KYC L0 日存款限額
  ch6.risk.auto_approve_max        → Ch6 風險 自動通過上界
  ch15.afford.basic_min_gbp        → Ch15 負擔能力 基礎評估下限
  ch8.commission.tier1_rate_pct    → Ch8 佣金 Tier1 分成比例
```

**規則**:
- 全小寫，底線分隔
- 章節編號前綴 (`ch1`~`ch16`)
- 領域名稱 (`kyc`, `risk`, `wallet`, `bonus` 等)
- 具體參數名 (描述性英文)
- 百分比後綴 `_pct`，金額後綴對應幣種或 `$`

---

*PRD v1.0 — 2026-03-25 — Sprint v2.2*
