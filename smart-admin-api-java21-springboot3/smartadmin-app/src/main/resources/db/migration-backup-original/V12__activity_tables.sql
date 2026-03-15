-- =====================================================================
-- V12: Activity Engine Tables — Phase 4 (04_Activity_Engine)
-- =====================================================================
-- Creates 2 tables:
--   - t_promotion_rule: Promotion rule configuration
--   - t_player_bonus_record: Player bonus claim records
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - BIGINT for tenant_id (Multi-Tenant G1)
-- =====================================================================

-- =====================================================================
-- Part A: t_promotion_rule (Promotion Rule Configuration)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_promotion_rule (
    rule_id                 BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    promotion_code          VARCHAR(64)     NOT NULL,
    promotion_name          VARCHAR(128)    NOT NULL,
    promotion_type          SMALLINT        NOT NULL,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    start_time              TIMESTAMPTZ     NOT NULL,
    end_time                TIMESTAMPTZ     NOT NULL,
    min_deposit             DECIMAL(19,4)   NOT NULL DEFAULT 0,
    bonus_rate              DECIMAL(8,4),
    max_bonus               DECIMAL(19,4),
    wagering_multiplier     DECIMAL(8,2)    NOT NULL DEFAULT 1.00,
    max_claims_per_player   INT             NOT NULL DEFAULT 1,
    bonus_expiry_days       INT             NOT NULL DEFAULT 30,
    game_restriction        JSONB,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_promo_type CHECK (promotion_type IN (1, 2, 3, 4, 5)),
    CONSTRAINT ck_promo_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_promotion_rule IS '促銷活動規則配置表';
COMMENT ON COLUMN t_promotion_rule.rule_id IS '規則唯一標識';
COMMENT ON COLUMN t_promotion_rule.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_promotion_rule.promotion_code IS '促銷代碼 (租戶內唯一)';
COMMENT ON COLUMN t_promotion_rule.promotion_name IS '促銷名稱';
COMMENT ON COLUMN t_promotion_rule.promotion_type IS '促銷類型: 1=首存, 2=續存, 3=流水回饋, 4=活動, 5=免費旋轉';
COMMENT ON COLUMN t_promotion_rule.status IS '狀態: 1=有效, 2=暫停, 3=過期';
COMMENT ON COLUMN t_promotion_rule.start_time IS '活動開始時間';
COMMENT ON COLUMN t_promotion_rule.end_time IS '活動結束時間';
COMMENT ON COLUMN t_promotion_rule.min_deposit IS '最低存款金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_promotion_rule.bonus_rate IS '獎金比例 (1.0000=100%)';
COMMENT ON COLUMN t_promotion_rule.max_bonus IS '最大獎金金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_promotion_rule.wagering_multiplier IS '流水倍數 (e.g. 20.00 表示 20 倍流水)';
COMMENT ON COLUMN t_promotion_rule.max_claims_per_player IS '每位玩家最大領取次數';
COMMENT ON COLUMN t_promotion_rule.bonus_expiry_days IS '獎金有效天數';
COMMENT ON COLUMN t_promotion_rule.game_restriction IS '遊戲限制規則 (JSONB)';
COMMENT ON COLUMN t_promotion_rule.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_promotion_rule.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX IF NOT EXISTS uk_promo_code_tenant ON t_promotion_rule (promotion_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_promo_tenant_status ON t_promotion_rule (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_promo_type ON t_promotion_rule (promotion_type);

-- =====================================================================
-- Part B: t_player_bonus_record (Player Bonus Claim Records)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_player_bonus_record (
    record_id               BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    rule_id                 BIGINT          NOT NULL REFERENCES t_promotion_rule(rule_id),
    claim_id                VARCHAR(64)     NOT NULL,
    bonus_amount            DECIMAL(19,4)   NOT NULL,
    wagering_required       DECIMAL(19,4)   NOT NULL DEFAULT 0,
    wagering_completed      DECIMAL(19,4)   NOT NULL DEFAULT 0,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    claimed_at              TIMESTAMPTZ     NOT NULL,
    completed_at            TIMESTAMPTZ,
    expired_at              TIMESTAMPTZ,
    wallet_bonus_ext_id     BIGINT,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_bonus_record_status CHECK (status IN (1, 2, 3, 4, 5))
);

COMMENT ON TABLE t_player_bonus_record IS '玩家獎金領取記錄表';
COMMENT ON COLUMN t_player_bonus_record.record_id IS '記錄唯一標識';
COMMENT ON COLUMN t_player_bonus_record.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_player_bonus_record.player_id IS '玩家 ID';
COMMENT ON COLUMN t_player_bonus_record.rule_id IS '促銷規則 ID (FK→t_promotion_rule)';
COMMENT ON COLUMN t_player_bonus_record.claim_id IS '領取唯一鍵 (冪等性)';
COMMENT ON COLUMN t_player_bonus_record.bonus_amount IS '獎金金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_player_bonus_record.wagering_required IS '需完成流水額 DECIMAL(19,4)';
COMMENT ON COLUMN t_player_bonus_record.wagering_completed IS '已完成流水額 DECIMAL(19,4)';
COMMENT ON COLUMN t_player_bonus_record.status IS '狀態: 1=待啟用, 2=進行中, 3=已完成, 4=已過期, 5=已沒收';
COMMENT ON COLUMN t_player_bonus_record.claimed_at IS '領取時間';
COMMENT ON COLUMN t_player_bonus_record.completed_at IS '完成時間';
COMMENT ON COLUMN t_player_bonus_record.expired_at IS '過期時間';
COMMENT ON COLUMN t_player_bonus_record.wallet_bonus_ext_id IS '關聯 t_wallet_bonus_ext.id';
COMMENT ON COLUMN t_player_bonus_record.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_player_bonus_record.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX IF NOT EXISTS uk_bonus_claim ON t_player_bonus_record (player_id, rule_id, claim_id, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bonus_player ON t_player_bonus_record (player_id);
CREATE INDEX IF NOT EXISTS idx_bonus_tenant_status ON t_player_bonus_record (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_bonus_rule ON t_player_bonus_record (rule_id);
CREATE INDEX IF NOT EXISTS idx_bonus_expired ON t_player_bonus_record (expired_at, status) WHERE status = 2;

-- =====================================================================
-- Part C: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_promotion_rule ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_player_bonus_record ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_promotion_rule
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_player_bonus_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part D: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_promotion_rule TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_player_bonus_record TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_promotion_rule_rule_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_player_bonus_record_record_id_seq TO smartadmin_app;
