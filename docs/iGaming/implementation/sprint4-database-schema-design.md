# Sprint 4 Database Schema Design

**專案**: SmartAdmin iGaming Integration - P1 Features
**日期**: 2026-03-25
**版本**: 1.0.0
**設計者**: iGaming Team

---

## 設計總覽

### 新增表數量

| 功能模塊 | 表數量 | Migration 版本 |
|---------|--------|---------------|
| **代理佣金系統** | 5 張 | V013 |
| **VIP 等級系統** | 3 張 | V014 |
| **自我排除系統** | 2 張 | V015 |
| **總計** | **10 張** | V013-V015 |

---

## 1. 代理佣金系統（Agent Commission System）

### 1.1 業務需求

- 支持多層級代理結構（最多 5 層）
- 佣金基於負盈利計算（玩家輸的錢 - 平台成本）
- 防止重複結算（分佈式環境）
- 支持佣金凍結/解凍（風險管理）
- 週度結算批次

### 1.2 表設計

#### 表 1: t_agent_relationship（代理關係表）

**用途**: 管理玩家與代理的多層級關係樹

```sql
CREATE TABLE t_agent_relationship (
  relationship_id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL,                    -- 玩家 ID（FK → t_player）
  agent_id BIGINT NOT NULL,                      -- 代理 ID（FK → t_player）
  level SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 5),  -- 代理層級（1-5）
  bind_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 綁定時間
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, TERMINATED
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_agent_relationship_player FOREIGN KEY (player_id) REFERENCES t_player(player_id),
  CONSTRAINT fk_agent_relationship_agent FOREIGN KEY (agent_id) REFERENCES t_player(player_id),
  CONSTRAINT uk_agent_relationship_player_agent UNIQUE (player_id, agent_id, tenant_id, deleted)
);

CREATE INDEX idx_agent_relationship_player ON t_agent_relationship(player_id, tenant_id, deleted) WHERE deleted = false;
CREATE INDEX idx_agent_relationship_agent ON t_agent_relationship(agent_id, tenant_id, status, deleted) WHERE deleted = false;
CREATE INDEX idx_agent_relationship_level ON t_agent_relationship(agent_id, level, tenant_id) WHERE deleted = false AND status = 'ACTIVE';

COMMENT ON TABLE t_agent_relationship IS '代理關係表 - 管理玩家與代理的多層級關係樹';
COMMENT ON COLUMN t_agent_relationship.level IS '代理層級：1（一級代理）到 5（五級代理）';
COMMENT ON COLUMN t_agent_relationship.status IS '狀態：ACTIVE（活躍）、SUSPENDED（暫停）、TERMINATED（終止）';
```

---

#### 表 2: t_agent_commission_config（佣金配置表）

**用途**: 配置不同代理層級和產品類型的佣金比例

```sql
CREATE TABLE t_agent_commission_config (
  config_id BIGSERIAL PRIMARY KEY,
  agent_level SMALLINT NOT NULL CHECK (agent_level BETWEEN 1 AND 5),  -- 代理層級（1-5）
  product_type VARCHAR(20) NOT NULL,             -- SPORTS, CASINO, LIVE, POKER, LOTTERY
  commission_rate NUMERIC(5,4) NOT NULL CHECK (commission_rate BETWEEN 0 AND 1),  -- 佣金比例（0.0000-1.0000）
  min_negative_profit NUMERIC(19,4) DEFAULT 0,   -- 最低負盈利要求（低於此值不發放佣金）
  effective_from DATE NOT NULL,                  -- 生效開始日期
  effective_to DATE,                             -- 生效結束日期（NULL 表示永久生效）
  status SMALLINT NOT NULL DEFAULT 1,            -- 1=ENABLED, 0=DISABLED
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_agent_commission_config UNIQUE (agent_level, product_type, effective_from, tenant_id, deleted)
);

CREATE INDEX idx_agent_commission_config_lookup ON t_agent_commission_config(tenant_id, agent_level, product_type, status, deleted, effective_from, effective_to) WHERE deleted = false;

COMMENT ON TABLE t_agent_commission_config IS '佣金配置表 - 定義不同代理層級和產品類型的佣金比例';
COMMENT ON COLUMN t_agent_commission_config.commission_rate IS '佣金比例：0.3000 = 30%';
COMMENT ON COLUMN t_agent_commission_config.min_negative_profit IS '最低負盈利要求：低於此值不發放佣金';
```

---

#### 表 3: t_agent_commission_record（佣金記錄表）

**用途**: 記錄每個代理的佣金計算結果

```sql
CREATE TABLE t_agent_commission_record (
  record_id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,                      -- 代理 ID（FK → t_player）
  settlement_date DATE NOT NULL,                 -- 結算日期（週度結算，例如 2026-03-24）
  product_type VARCHAR(20) NOT NULL,             -- SPORTS, CASINO, LIVE, POKER, LOTTERY
  total_valid_turnover NUMERIC(19,4) NOT NULL DEFAULT 0,  -- 總有效投注額
  total_bet_amount NUMERIC(19,4) NOT NULL DEFAULT 0,      -- 總投注額
  total_payout_amount NUMERIC(19,4) NOT NULL DEFAULT 0,   -- 總派彩額
  total_negative_profit NUMERIC(19,4) NOT NULL DEFAULT 0, -- 總負盈利（投注額 - 派彩額）
  commission_rate NUMERIC(5,4) NOT NULL,         -- 適用的佣金比例
  commission_amount NUMERIC(19,4) NOT NULL,      -- 佣金金額
  platform_cost NUMERIC(19,4) DEFAULT 0,         -- 平台成本扣除
  net_commission NUMERIC(19,4) NOT NULL,         -- 淨佣金（commission_amount - platform_cost）
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SETTLED, FROZEN, CANCELLED
  settlement_batch_id BIGINT,                    -- 結算批次 ID（FK → t_agent_commission_settlement）
  frozen_reason TEXT,                            -- 凍結原因（如果 status = FROZEN）
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_agent_commission_record_agent FOREIGN KEY (agent_id) REFERENCES t_player(player_id),
  CONSTRAINT uk_agent_commission_record UNIQUE (agent_id, settlement_date, product_type, tenant_id, deleted)
);

CREATE INDEX idx_agent_commission_record_agent ON t_agent_commission_record(agent_id, settlement_date, status, tenant_id) WHERE deleted = false;
CREATE INDEX idx_agent_commission_record_batch ON t_agent_commission_record(settlement_batch_id, status) WHERE deleted = false;
CREATE INDEX idx_agent_commission_record_status ON t_agent_commission_record(status, settlement_date, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_agent_commission_record IS '佣金記錄表 - 記錄每個代理的佣金計算結果';
COMMENT ON COLUMN t_agent_commission_record.total_negative_profit IS '總負盈利 = 總投注額 - 總派彩額（玩家輸的錢）';
COMMENT ON COLUMN t_agent_commission_record.net_commission IS '淨佣金 = 佣金金額 - 平台成本';
COMMENT ON COLUMN t_agent_commission_record.status IS '狀態：PENDING（待結算）、SETTLED（已結算）、FROZEN（凍結）、CANCELLED（取消）';
```

---

#### 表 4: t_agent_commission_settlement（結算批次表）

**用途**: 管理佣金結算批次，防止重複結算

```sql
CREATE TABLE t_agent_commission_settlement (
  batch_id BIGSERIAL PRIMARY KEY,
  settlement_date DATE NOT NULL,                 -- 結算日期（例如 2026-03-24，代表上週的佣金）
  total_agents_count INT NOT NULL DEFAULT 0,    -- 參與結算的代理數量
  total_commission_amount NUMERIC(19,4) NOT NULL DEFAULT 0,  -- 總佣金金額
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING', -- RUNNING, COMPLETED, FAILED
  started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 開始時間
  completed_at TIMESTAMP WITH TIME ZONE,         -- 完成時間
  error_message TEXT,                            -- 錯誤訊息（如果 status = FAILED）
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_agent_commission_settlement UNIQUE (settlement_date, tenant_id, deleted)
);

CREATE INDEX idx_agent_commission_settlement_date ON t_agent_commission_settlement(settlement_date, status, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_agent_commission_settlement IS '結算批次表 - 管理佣金結算批次，防止重複結算';
COMMENT ON COLUMN t_agent_commission_settlement.settlement_date IS '結算日期：例如 2026-03-24（代表 2026-03-18 ~ 2026-03-24 的佣金）';
COMMENT ON COLUMN t_agent_commission_settlement.status IS '狀態：RUNNING（執行中）、COMPLETED（完成）、FAILED（失敗）';
```

---

#### 表 5: t_agent_performance_snapshot（代理業績快照表）

**用途**: 記錄代理的月度/週度業績快照（用於排行榜和報表）

```sql
CREATE TABLE t_agent_performance_snapshot (
  snapshot_id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,                      -- 代理 ID（FK → t_player）
  snapshot_type VARCHAR(10) NOT NULL,            -- WEEKLY, MONTHLY
  snapshot_date DATE NOT NULL,                   -- 快照日期（週/月的最後一天）
  total_players_count INT NOT NULL DEFAULT 0,    -- 下級玩家總數
  active_players_count INT NOT NULL DEFAULT 0,   -- 活躍玩家數量（本週期有投注）
  new_players_count INT NOT NULL DEFAULT 0,      -- 新增玩家數量
  total_deposit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,     -- 總存款金額
  total_withdrawal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,  -- 總提款金額
  total_bet_amount NUMERIC(19,4) NOT NULL DEFAULT 0,         -- 總投注額
  total_valid_turnover NUMERIC(19,4) NOT NULL DEFAULT 0,     -- 總有效投注額
  total_negative_profit NUMERIC(19,4) NOT NULL DEFAULT 0,    -- 總負盈利
  total_commission_amount NUMERIC(19,4) NOT NULL DEFAULT 0,  -- 總佣金金額
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_agent_performance_agent FOREIGN KEY (agent_id) REFERENCES t_player(player_id),
  CONSTRAINT uk_agent_performance UNIQUE (agent_id, snapshot_type, snapshot_date, tenant_id, deleted)
);

CREATE INDEX idx_agent_performance_agent ON t_agent_performance_snapshot(agent_id, snapshot_type, snapshot_date, tenant_id) WHERE deleted = false;
CREATE INDEX idx_agent_performance_ranking ON t_agent_performance_snapshot(snapshot_type, snapshot_date, total_commission_amount DESC, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_agent_performance_snapshot IS '代理業績快照表 - 記錄代理的月度/週度業績快照';
COMMENT ON COLUMN t_agent_performance_snapshot.snapshot_type IS '快照類型：WEEKLY（週快照）、MONTHLY（月快照）';
COMMENT ON COLUMN t_agent_performance_snapshot.active_players_count IS '活躍玩家數量：本週期有投注的玩家';
```

---

## 2. VIP 等級系統（VIP Tier System）

### 2.1 業務需求

- 10 個 VIP 等級（Bronze → Silver → Gold → Platinum → Diamond → ...）
- 升級條件：累計投注額、累計存款、活躍天數
- 自動升級任務（每日執行）
- 升級獎勵自動發放（獎金、返水比例提升）
- 升級通知（Email + SMS + 站內信）

### 2.2 表設計

#### 表 6: t_vip_level_config（VIP 等級配置表）

**用途**: 配置 VIP 等級的升級條件和權益

```sql
CREATE TABLE t_vip_level_config (
  level_id BIGSERIAL PRIMARY KEY,
  level SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 10),  -- VIP 等級（1-10）
  level_name VARCHAR(50) NOT NULL,               -- Bronze, Silver, Gold, Platinum, Diamond, ...
  min_cumulative_turnover NUMERIC(19,4) NOT NULL DEFAULT 0,     -- 最低累計投注額
  min_cumulative_deposit NUMERIC(19,4) NOT NULL DEFAULT 0,      -- 最低累計存款
  min_active_days INT NOT NULL DEFAULT 0,        -- 最低活躍天數
  upgrade_bonus NUMERIC(19,4) NOT NULL DEFAULT 0,              -- 升級獎金
  cashback_rate NUMERIC(5,4) NOT NULL DEFAULT 0, -- 返水比例（0.0100 = 1%）
  birthday_bonus NUMERIC(19,4) NOT NULL DEFAULT 0,             -- 生日禮金
  monthly_bonus NUMERIC(19,4) NOT NULL DEFAULT 0,              -- 月度獎金
  withdrawal_daily_limit NUMERIC(19,4),          -- 每日提款限額（NULL 表示無限制）
  priority_support BOOLEAN NOT NULL DEFAULT false,  -- 是否優先客服支持
  dedicated_account_manager BOOLEAN NOT NULL DEFAULT false,  -- 是否專屬客戶經理
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_vip_level_config_level UNIQUE (level, tenant_id, deleted),
  CONSTRAINT uk_vip_level_config_name UNIQUE (level_name, tenant_id, deleted)
);

CREATE INDEX idx_vip_level_config_lookup ON t_vip_level_config(level, tenant_id, deleted) WHERE deleted = false;

COMMENT ON TABLE t_vip_level_config IS 'VIP 等級配置表 - 定義 VIP 等級的升級條件和權益';
COMMENT ON COLUMN t_vip_level_config.level IS 'VIP 等級：1（Bronze）到 10（Diamond）';
COMMENT ON COLUMN t_vip_level_config.cashback_rate IS '返水比例：0.0100 = 1%';
COMMENT ON COLUMN t_vip_level_config.withdrawal_daily_limit IS '每日提款限額：NULL 表示無限制';
```

---

#### 表 7: t_player_vip_history（玩家 VIP 升級歷史表）

**用途**: 記錄玩家的 VIP 升級歷史

```sql
CREATE TABLE t_player_vip_history (
  history_id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL,                     -- 玩家 ID（FK → t_player）
  old_level SMALLINT,                            -- 舊等級（首次註冊為 NULL）
  new_level SMALLINT NOT NULL,                   -- 新等級
  upgrade_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 升級時間
  upgrade_reason VARCHAR(50) NOT NULL,           -- AUTO（自動升級）、MANUAL（手動升級）、PROMOTION（促銷活動）
  reward_amount NUMERIC(19,4) DEFAULT 0,         -- 升級獎勵金額
  cumulative_turnover_at_upgrade NUMERIC(19,4),  -- 升級時的累計投注額
  cumulative_deposit_at_upgrade NUMERIC(19,4),   -- 升級時的累計存款
  active_days_at_upgrade INT,                    -- 升級時的活躍天數
  operator_id BIGINT,                            -- 操作人員 ID（手動升級時）
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_player_vip_history_player FOREIGN KEY (player_id) REFERENCES t_player(player_id)
);

CREATE INDEX idx_player_vip_history_player ON t_player_vip_history(player_id, upgrade_time DESC, tenant_id) WHERE deleted = false;
CREATE INDEX idx_player_vip_history_time ON t_player_vip_history(upgrade_time, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_player_vip_history IS '玩家 VIP 升級歷史表 - 記錄玩家的 VIP 升級歷史';
COMMENT ON COLUMN t_player_vip_history.upgrade_reason IS '升級原因：AUTO（自動升級）、MANUAL（手動升級）、PROMOTION（促銷活動）';
```

---

#### 表 8: t_vip_reward_record（VIP 獎勵記錄表）

**用途**: 記錄 VIP 玩家的各類獎勵發放記錄

```sql
CREATE TABLE t_vip_reward_record (
  reward_id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL,                     -- 玩家 ID（FK → t_player）
  vip_level SMALLINT NOT NULL,                   -- VIP 等級
  reward_type VARCHAR(20) NOT NULL,              -- UPGRADE_BONUS, BIRTHDAY, MONTHLY, CASHBACK
  reward_amount NUMERIC(19,4) NOT NULL,          -- 獎勵金額
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ISSUED, CANCELLED
  issued_at TIMESTAMP WITH TIME ZONE,            -- 發放時間
  cancelled_reason TEXT,                         -- 取消原因（如果 status = CANCELLED）
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_vip_reward_record_player FOREIGN KEY (player_id) REFERENCES t_player(player_id)
);

CREATE INDEX idx_vip_reward_record_player ON t_vip_reward_record(player_id, reward_type, status, tenant_id) WHERE deleted = false;
CREATE INDEX idx_vip_reward_record_status ON t_vip_reward_record(status, create_time, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_vip_reward_record IS 'VIP 獎勵記錄表 - 記錄 VIP 玩家的各類獎勵發放記錄';
COMMENT ON COLUMN t_vip_reward_record.reward_type IS '獎勵類型：UPGRADE_BONUS（升級獎金）、BIRTHDAY（生日禮金）、MONTHLY（月度獎金）、CASHBACK（返水）';
COMMENT ON COLUMN t_vip_reward_record.status IS '狀態：PENDING（待發放）、ISSUED（已發放）、CANCELLED（已取消）';
```

---

## 3. 自我排除系統（Self-Exclusion System）

### 3.1 業務需求

- 限制類型：存款限制、投注限制、登入限制、完全封鎖
- 冷靜期：24小時、7天、30天、永久
- 解除審核流程（合規團隊審核）
- 監管合規性（符合博彩監管要求）
- 審計記錄（所有操作可追溯）

### 3.2 表設計

#### 表 9: t_self_exclusion_request（自我排除請求表）

**用途**: 記錄玩家的自我排除請求

```sql
CREATE TABLE t_self_exclusion_request (
  request_id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL,                     -- 玩家 ID（FK → t_player）
  exclusion_type VARCHAR(20) NOT NULL,           -- DEPOSIT, BETTING, LOGIN, FULL_BLOCK
  cooling_period VARCHAR(20) NOT NULL,           -- HOURS_24, DAYS_7, DAYS_30, PERMANENT
  cooling_period_end_time TIMESTAMP WITH TIME ZONE,  -- 冷靜期結束時間（PERMANENT 為 NULL）
  reason TEXT,                                   -- 玩家申請原因
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, COOLING_PERIOD_EXPIRED, REVIEW_PENDING, RELEASED
  released_at TIMESTAMP WITH TIME ZONE,          -- 解除時間（如果 status = RELEASED）
  reviewer_id BIGINT,                            -- 審核人員 ID（FK → t_employee）
  review_comment TEXT,                           -- 審核意見
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_self_exclusion_request_player FOREIGN KEY (player_id) REFERENCES t_player(player_id)
);

CREATE INDEX idx_self_exclusion_request_player ON t_self_exclusion_request(player_id, status, tenant_id, deleted) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_request_status ON t_self_exclusion_request(status, cooling_period_end_time, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_self_exclusion_request IS '自我排除請求表 - 記錄玩家的自我排除請求';
COMMENT ON COLUMN t_self_exclusion_request.exclusion_type IS '限制類型：DEPOSIT（存款限制）、BETTING（投注限制）、LOGIN（登入限制）、FULL_BLOCK（完全封鎖）';
COMMENT ON COLUMN t_self_exclusion_request.cooling_period IS '冷靜期：HOURS_24（24小時）、DAYS_7（7天）、DAYS_30（30天）、PERMANENT（永久）';
COMMENT ON COLUMN t_self_exclusion_request.status IS '狀態：ACTIVE（生效中）、COOLING_PERIOD_EXPIRED（冷靜期結束）、REVIEW_PENDING（審核中）、RELEASED（已解除）';
```

---

#### 表 10: t_self_exclusion_history（自我排除歷史表）

**用途**: 記錄自我排除請求的所有操作歷史（完整審計記錄）

```sql
CREATE TABLE t_self_exclusion_history (
  history_id BIGSERIAL PRIMARY KEY,
  request_id BIGINT NOT NULL,                    -- 請求 ID（FK → t_self_exclusion_request）
  player_id BIGINT NOT NULL,                     -- 玩家 ID（FK → t_player）
  operation_type VARCHAR(20) NOT NULL,           -- CREATE, UPDATE, RELEASE, REJECT
  old_status VARCHAR(30),                        -- 舊狀態
  new_status VARCHAR(30),                        -- 新狀態
  reviewer_id BIGINT,                            -- 審核人員 ID（FK → t_employee）
  review_comment TEXT,                           -- 審核意見
  operated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 操作時間
  audit_log JSONB,                               -- 完整審計記錄（JSON 格式）
  tenant_id BIGINT NOT NULL,                     -- 租戶 ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_self_exclusion_history_request FOREIGN KEY (request_id) REFERENCES t_self_exclusion_request(request_id),
  CONSTRAINT fk_self_exclusion_history_player FOREIGN KEY (player_id) REFERENCES t_player(player_id)
);

CREATE INDEX idx_self_exclusion_history_request ON t_self_exclusion_history(request_id, operated_at DESC, tenant_id) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_history_player ON t_self_exclusion_history(player_id, operated_at DESC, tenant_id) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_history_operation ON t_self_exclusion_history(operation_type, operated_at, tenant_id) WHERE deleted = false;

COMMENT ON TABLE t_self_exclusion_history IS '自我排除歷史表 - 記錄自我排除請求的所有操作歷史';
COMMENT ON COLUMN t_self_exclusion_history.operation_type IS '操作類型：CREATE（創建）、UPDATE（更新）、RELEASE（解除）、REJECT（拒絕）';
COMMENT ON COLUMN t_self_exclusion_history.audit_log IS '完整審計記錄（JSON）：包含操作前後的完整數據快照';
```

---

## 4. 索引策略

### 4.1 查詢優化索引

所有表都包含以下標準索引：

1. **主鍵索引**（自動創建）
2. **外鍵索引**（提升 JOIN 性能）
3. **查詢索引**（複合索引，覆蓋常用查詢條件）
4. **Partial Index**（WHERE deleted = false）減少索引大小

### 4.2 索引命名規範

```
idx_{table_name}_{column1}_{column2}_{usage}
```

範例：
- `idx_agent_commission_record_agent` - 按代理查詢佣金記錄
- `idx_vip_reward_record_status` - 按狀態查詢獎勵記錄
- `idx_self_exclusion_request_player` - 按玩家查詢排除請求

---

## 5. 數據完整性約束

### 5.1 外鍵約束

所有表都包含適當的外鍵約束：

| 表 | 外鍵 | 參考表 |
|----|------|--------|
| t_agent_relationship | player_id, agent_id | t_player |
| t_agent_commission_record | agent_id | t_player |
| t_agent_performance_snapshot | agent_id | t_player |
| t_player_vip_history | player_id | t_player |
| t_vip_reward_record | player_id | t_player |
| t_self_exclusion_request | player_id | t_player |
| t_self_exclusion_history | request_id, player_id | t_self_exclusion_request, t_player |

### 5.2 CHECK 約束

- **代理層級**：`CHECK (level BETWEEN 1 AND 5)`
- **VIP 等級**：`CHECK (level BETWEEN 1 AND 10)`
- **佣金比例**：`CHECK (commission_rate BETWEEN 0 AND 1)`
- **金額字段**：所有金額字段使用 `NUMERIC(19,4)` 精度

### 5.3 唯一約束

- **代理關係**：`UNIQUE (player_id, agent_id, tenant_id, deleted)`
- **佣金配置**：`UNIQUE (agent_level, product_type, effective_from, tenant_id, deleted)`
- **結算批次**：`UNIQUE (settlement_date, tenant_id, deleted)`
- **VIP 等級**：`UNIQUE (level, tenant_id, deleted)`

---

## 6. 性能考慮

### 6.1 查詢優化

1. **代理樹查詢**: 使用 PostgreSQL Recursive CTE（原生支持，性能優異）
2. **複合索引**: 覆蓋常用查詢條件（tenant_id, status, deleted）
3. **Partial Index**: 僅索引 deleted = false 的記錄

### 6.2 預期性能指標

| 操作 | 目標響應時間 |
|------|-------------|
| 代理樹查詢（5 層，1000 個代理）| < 50ms |
| 佣金記錄查詢（單個代理）| < 10ms |
| VIP 升級條件檢查（100,000 玩家）| < 10 秒 |
| 自我排除狀態查詢 | < 1ms（Redis 緩存命中）|

---

## 7. 審計與合規

### 7.1 審計字段

所有表包含標準審計字段：

- `tenant_id`: 多租戶隔離
- `deleted`: 軟刪除標記（保留歷史數據）
- `create_time`: 創建時間
- `update_time`: 更新時間（代理業績快照表除外）

### 7.2 JSONB 審計日誌

- **t_self_exclusion_history.audit_log**: 記錄操作前後的完整數據快照
- **格式**: `{"before": {...}, "after": {...}, "changes": {...}}`

---

## 8. Migration 版本規劃

| Migration | 描述 | 預計執行時間 |
|-----------|------|-------------|
| **V013** | 創建代理佣金系統表（5 張表）| < 5 秒 |
| **V014** | 創建 VIP 等級系統表（3 張表）| < 3 秒 |
| **V015** | 創建自我排除系統表（2 張表）| < 2 秒 |

---

## 9. 下一步行動

1. ✅ 完成 Database Schema 設計文檔
2. [ ] 創建 Flyway migration scripts (V013-V015)
3. [ ] 添加種子數據（VIP 等級配置、佣金配置範例）
4. [ ] 運行 FlywayMigrationIntegrationTest 驗證
5. [ ] 進入 Phase 1: 代理佣金系統實現

---

**文檔版本**: 1.0.0
**最後更新**: 2026-03-25 17:15
**審核狀態**: ✅ 設計完成，待實施
