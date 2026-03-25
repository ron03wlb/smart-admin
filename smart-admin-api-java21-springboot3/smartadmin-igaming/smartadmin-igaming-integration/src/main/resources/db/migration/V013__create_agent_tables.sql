-- =================================================================
-- Migration: V013__create_agent_tables.sql
-- Description: Create Agent Commission System tables (5 tables)
-- Author: iGaming Team
-- Date: 2026-03-25
-- =================================================================

-- =================================================================
-- Table 1: t_agent_relationship (代理關係表)
-- Purpose: Manage multi-level agent-player relationships (up to 5 levels)
-- =================================================================
CREATE TABLE t_agent_relationship (
  relationship_id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL,                    -- Player ID (FK → t_player)
  agent_id BIGINT NOT NULL,                      -- Agent ID (FK → t_player)
  level SMALLINT NOT NULL CHECK (level BETWEEN 1 AND 5),  -- Agent level (1-5)
  bind_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- Binding time
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, TERMINATED
  tenant_id BIGINT NOT NULL,                     -- Tenant ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_agent_relationship_player FOREIGN KEY (player_id) REFERENCES t_player(player_id),
  CONSTRAINT fk_agent_relationship_agent FOREIGN KEY (agent_id) REFERENCES t_player(player_id),
  CONSTRAINT uk_agent_relationship_player_agent UNIQUE (player_id, agent_id, tenant_id, deleted)
);

-- Indexes for t_agent_relationship
CREATE INDEX idx_agent_relationship_player ON t_agent_relationship(player_id, tenant_id, deleted) WHERE deleted = false;
CREATE INDEX idx_agent_relationship_agent ON t_agent_relationship(agent_id, tenant_id, status, deleted) WHERE deleted = false;
CREATE INDEX idx_agent_relationship_level ON t_agent_relationship(agent_id, level, tenant_id) WHERE deleted = false AND status = 'ACTIVE';

-- Comments for t_agent_relationship
COMMENT ON TABLE t_agent_relationship IS '代理關係表 - 管理玩家與代理的多層級關係樹';
COMMENT ON COLUMN t_agent_relationship.level IS '代理層級：1（一級代理）到 5（五級代理）';
COMMENT ON COLUMN t_agent_relationship.status IS '狀態：ACTIVE（活躍）、SUSPENDED（暫停）、TERMINATED（終止）';

-- =================================================================
-- Table 2: t_agent_commission_config (佣金配置表)
-- Purpose: Configure commission rates for different agent levels and product types
-- =================================================================
CREATE TABLE t_agent_commission_config (
  config_id BIGSERIAL PRIMARY KEY,
  agent_level SMALLINT NOT NULL CHECK (agent_level BETWEEN 1 AND 5),  -- Agent level (1-5)
  product_type VARCHAR(20) NOT NULL,             -- SPORTS, CASINO, LIVE, POKER, LOTTERY
  commission_rate NUMERIC(5,4) NOT NULL CHECK (commission_rate BETWEEN 0 AND 1),  -- Commission rate (0.0000-1.0000)
  min_negative_profit NUMERIC(19,4) DEFAULT 0,   -- Minimum negative profit threshold
  effective_from DATE NOT NULL,                  -- Effective start date
  effective_to DATE,                             -- Effective end date (NULL = permanent)
  status SMALLINT NOT NULL DEFAULT 1,            -- 1=ENABLED, 0=DISABLED
  tenant_id BIGINT NOT NULL,                     -- Tenant ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_agent_commission_config UNIQUE (agent_level, product_type, effective_from, tenant_id, deleted)
);

-- Indexes for t_agent_commission_config
CREATE INDEX idx_agent_commission_config_lookup ON t_agent_commission_config(tenant_id, agent_level, product_type, status, deleted, effective_from, effective_to) WHERE deleted = false;

-- Comments for t_agent_commission_config
COMMENT ON TABLE t_agent_commission_config IS '佣金配置表 - 定義不同代理層級和產品類型的佣金比例';
COMMENT ON COLUMN t_agent_commission_config.commission_rate IS '佣金比例：0.3000 = 30%';
COMMENT ON COLUMN t_agent_commission_config.min_negative_profit IS '最低負盈利要求：低於此值不發放佣金';

-- =================================================================
-- Table 3: t_agent_commission_record (佣金記錄表)
-- Purpose: Record commission calculation results for each agent
-- =================================================================
CREATE TABLE t_agent_commission_record (
  record_id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,                      -- Agent ID (FK → t_player)
  settlement_date DATE NOT NULL,                 -- Settlement date (weekly, e.g., 2026-03-24)
  product_type VARCHAR(20) NOT NULL,             -- SPORTS, CASINO, LIVE, POKER, LOTTERY
  total_valid_turnover NUMERIC(19,4) NOT NULL DEFAULT 0,  -- Total valid turnover
  total_bet_amount NUMERIC(19,4) NOT NULL DEFAULT 0,      -- Total bet amount
  total_payout_amount NUMERIC(19,4) NOT NULL DEFAULT 0,   -- Total payout amount
  total_negative_profit NUMERIC(19,4) NOT NULL DEFAULT 0, -- Total negative profit (bet - payout)
  commission_rate NUMERIC(5,4) NOT NULL,         -- Applied commission rate
  commission_amount NUMERIC(19,4) NOT NULL,      -- Commission amount
  platform_cost NUMERIC(19,4) DEFAULT 0,         -- Platform cost deduction
  net_commission NUMERIC(19,4) NOT NULL,         -- Net commission (commission_amount - platform_cost)
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SETTLED, FROZEN, CANCELLED
  settlement_batch_id BIGINT,                    -- Settlement batch ID (FK → t_agent_commission_settlement)
  frozen_reason TEXT,                            -- Reason for frozen status
  tenant_id BIGINT NOT NULL,                     -- Tenant ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_agent_commission_record_agent FOREIGN KEY (agent_id) REFERENCES t_player(player_id),
  CONSTRAINT uk_agent_commission_record UNIQUE (agent_id, settlement_date, product_type, tenant_id, deleted)
);

-- Indexes for t_agent_commission_record
CREATE INDEX idx_agent_commission_record_agent ON t_agent_commission_record(agent_id, settlement_date, status, tenant_id) WHERE deleted = false;
CREATE INDEX idx_agent_commission_record_batch ON t_agent_commission_record(settlement_batch_id, status) WHERE deleted = false;
CREATE INDEX idx_agent_commission_record_status ON t_agent_commission_record(status, settlement_date, tenant_id) WHERE deleted = false;

-- Comments for t_agent_commission_record
COMMENT ON TABLE t_agent_commission_record IS '佣金記錄表 - 記錄每個代理的佣金計算結果';
COMMENT ON COLUMN t_agent_commission_record.total_negative_profit IS '總負盈利 = 總投注額 - 總派彩額（玩家輸的錢）';
COMMENT ON COLUMN t_agent_commission_record.net_commission IS '淨佣金 = 佣金金額 - 平台成本';
COMMENT ON COLUMN t_agent_commission_record.status IS '狀態：PENDING（待結算）、SETTLED（已結算）、FROZEN（凍結）、CANCELLED（取消）';

-- =================================================================
-- Table 4: t_agent_commission_settlement (結算批次表)
-- Purpose: Manage commission settlement batches, prevent duplicate settlements
-- =================================================================
CREATE TABLE t_agent_commission_settlement (
  batch_id BIGSERIAL PRIMARY KEY,
  settlement_date DATE NOT NULL,                 -- Settlement date (e.g., 2026-03-24, for last week's commission)
  total_agents_count INT NOT NULL DEFAULT 0,    -- Number of agents in settlement
  total_commission_amount NUMERIC(19,4) NOT NULL DEFAULT 0,  -- Total commission amount
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING', -- RUNNING, COMPLETED, FAILED
  started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- Start time
  completed_at TIMESTAMP WITH TIME ZONE,         -- Completion time
  error_message TEXT,                            -- Error message (if status = FAILED)
  tenant_id BIGINT NOT NULL,                     -- Tenant ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_agent_commission_settlement UNIQUE (settlement_date, tenant_id, deleted)
);

-- Indexes for t_agent_commission_settlement
CREATE INDEX idx_agent_commission_settlement_date ON t_agent_commission_settlement(settlement_date, status, tenant_id) WHERE deleted = false;

-- Comments for t_agent_commission_settlement
COMMENT ON TABLE t_agent_commission_settlement IS '結算批次表 - 管理佣金結算批次，防止重複結算';
COMMENT ON COLUMN t_agent_commission_settlement.settlement_date IS '結算日期：例如 2026-03-24（代表 2026-03-18 ~ 2026-03-24 的佣金）';
COMMENT ON COLUMN t_agent_commission_settlement.status IS '狀態：RUNNING（執行中）、COMPLETED（完成）、FAILED（失敗）';

-- =================================================================
-- Table 5: t_agent_performance_snapshot (代理業績快照表)
-- Purpose: Record weekly/monthly performance snapshots for agents (for rankings and reports)
-- =================================================================
CREATE TABLE t_agent_performance_snapshot (
  snapshot_id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,                      -- Agent ID (FK → t_player)
  snapshot_type VARCHAR(10) NOT NULL,            -- WEEKLY, MONTHLY
  snapshot_date DATE NOT NULL,                   -- Snapshot date (last day of week/month)
  total_players_count INT NOT NULL DEFAULT 0,    -- Total player count under agent
  active_players_count INT NOT NULL DEFAULT 0,   -- Active player count (with bets in period)
  new_players_count INT NOT NULL DEFAULT 0,      -- New player count
  total_deposit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,     -- Total deposit amount
  total_withdrawal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,  -- Total withdrawal amount
  total_bet_amount NUMERIC(19,4) NOT NULL DEFAULT 0,         -- Total bet amount
  total_valid_turnover NUMERIC(19,4) NOT NULL DEFAULT 0,     -- Total valid turnover
  total_negative_profit NUMERIC(19,4) NOT NULL DEFAULT 0,    -- Total negative profit
  total_commission_amount NUMERIC(19,4) NOT NULL DEFAULT 0,  -- Total commission amount
  tenant_id BIGINT NOT NULL,                     -- Tenant ID
  deleted BOOLEAN NOT NULL DEFAULT false,
  create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_agent_performance_agent FOREIGN KEY (agent_id) REFERENCES t_player(player_id),
  CONSTRAINT uk_agent_performance UNIQUE (agent_id, snapshot_type, snapshot_date, tenant_id, deleted)
);

-- Indexes for t_agent_performance_snapshot
CREATE INDEX idx_agent_performance_agent ON t_agent_performance_snapshot(agent_id, snapshot_type, snapshot_date, tenant_id) WHERE deleted = false;
CREATE INDEX idx_agent_performance_ranking ON t_agent_performance_snapshot(snapshot_type, snapshot_date, total_commission_amount DESC, tenant_id) WHERE deleted = false;

-- Comments for t_agent_performance_snapshot
COMMENT ON TABLE t_agent_performance_snapshot IS '代理業績快照表 - 記錄代理的月度/週度業績快照';
COMMENT ON COLUMN t_agent_performance_snapshot.snapshot_type IS '快照類型：WEEKLY（週快照）、MONTHLY（月快照）';
COMMENT ON COLUMN t_agent_performance_snapshot.active_players_count IS '活躍玩家數量：本週期有投注的玩家';

-- =================================================================
-- End of Migration V013
-- =================================================================
