# 玩家保護 API 技術規格（Player Protection API）

> **規範來源**: [15-07_Player_Protection_API.md](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md)
> **目標讀者**: Architects, Backend Developers
> **業務需求**: [Player_Protection_Requirements.md](../../requirements/05_Risk_Compliance/08_Player_Protection_Requirements.md)
> **最後同步**: 2026-02-08

---

## 1. 概述（Overview）

本文檔定義了玩家保護 (Player Protection) 模組的 RESTful API 規格，涵蓋玩家端和管理端的負責任博彩工具接口。

---

## 2. API 端點摘要（API Endpoint Summary）

### 2.1 玩家端接口（Player-Facing Endpoints）

| 端點 | 方法 | 描述 |
|----------|--------|-------------|
| `/api/v1/player/protection/settings` | GET | 獲取所有保護設定 |
| `/api/v1/player/protection/settings` | PUT | 更新保護設定 |
| `/api/v1/player/protection/deposit-limits` | GET | 獲取存款限額 |
| `/api/v1/player/protection/deposit-limits` | PUT | 設定存款限額 |
| `/api/v1/player/protection/loss-limits` | GET | 獲取虧損限額 |
| `/api/v1/player/protection/loss-limits` | PUT | 設定虧損限額 |
| `/api/v1/player/protection/self-exclusion` | POST | 申請自我排除 |
| `/api/v1/player/protection/self-exclusion/revoke` | POST | 申請撤銷排除 |
| `/api/v1/player/protection/cooling-off` | POST | 啟動冷靜期 |
| `/api/v1/player/protection/session-limits` | GET | 獲取會話限制 |
| `/api/v1/player/protection/session-limits` | PUT | 設定會話限制 |
| `/api/v1/player/protection/activity-history` | GET | 獲取活動歷史 |
| `/api/v1/player/protection/usage` | GET | 獲取限額使用情況 |

### 2.2 管理端接口（Admin-Facing Endpoints）

| 端點 | 方法 | 描述 |
|----------|--------|-------------|
| `/api/v1/admin/protection/exclusions` | GET | 列出被排除玩家 |
| `/api/v1/admin/protection/exclusions/{playerId}` | GET | 玩家排除詳情 |
| `/api/v1/admin/protection/exclusions/{playerId}` | POST | 操作員發起的排除 |
| `/api/v1/admin/protection/reports` | GET | 合規報告 |
| `/api/v1/admin/protection/reports/export` | POST | 導出報告 |
| `/api/v1/admin/protection/gamstop/sync` | POST | Gamstop 同步 |
| `/api/v1/admin/protection/gamstop/check` | POST | Gamstop 查詢 |

---

## 3. 詳細 API 規格（Detailed API Specifications）

### 3.1 獲取保護設定（Get Protection Settings）

**請求（Request）**
```http
GET /api/v1/player/protection/settings
Authorization: Bearer {token}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "selfExclusion": {
      "active": false,
      "endTime": null
    },
    "coolingOff": {
      "active": false,
      "endTime": null
    },
    "depositLimits": {
      "daily": 1000.00,
      "weekly": 5000.00,
      "monthly": 15000.00,
      "pendingChanges": []
    },
    "lossLimits": {
      "daily": 500.00,
      "weekly": 2000.00,
      "monthly": null,
      "pendingChanges": []
    },
    "sessionLimits": {
      "durationMinutes": 60,
      "idleTimeoutMinutes": 30,
      "realityCheckIntervalMinutes": 30
    },
    "lastUpdated": "2026-02-07T10:30:00Z"
  }
}
```

### 3.2 設定存款限額（Set Deposit Limits）

**請求（Request）**
```http
PUT /api/v1/player/protection/deposit-limits
Authorization: Bearer {token}
Content-Type: application/json

{
  "dailyLimit": 500.00,
  "weeklyLimit": 2000.00,
  "monthlyLimit": 8000.00
}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "currentLimits": {
      "daily": 500.00,
      "weekly": 2000.00,
      "monthly": 8000.00
    },
    "pendingChanges": [
      {
        "type": "MONTHLY",
        "oldValue": 5000.00,
        "newValue": 8000.00,
        "effectiveTime": "2026-02-08T10:30:00Z",
        "status": "PENDING"
      }
    ],
    "message": "日限額和週限額已立即生效,月限額提高將於 24 小時後生效"
  }
}
```

### 3.3 申請自我排除（Request Self-Exclusion）

**請求（Request）**
```http
POST /api/v1/player/protection/self-exclusion
Authorization: Bearer {token}
Content-Type: application/json

{
  "duration": "6M",
  "reason": "需要休息一段時間",
  "coolingOffConfirmed": true
}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "exclusionId": 12345,
    "status": "ACTIVE",
    "startTime": "2026-02-07T10:30:00Z",
    "endTime": "2026-08-07T10:30:00Z",
    "duration": "6M",
    "message": "自我排除已生效,將於 2026-08-07 結束"
  }
}
```

**持續時間選項（Duration Options）**

| 值 | 描述 |
|-------|-------------|
| `24H` | 24 小時 |
| `7D` | 7 天 |
| `30D` | 30 天 |
| `6M` | 6 個月 |
| `1Y` | 1 年 |
| `5Y` | 5 年 |
| `PERMANENT` | 永久 |

### 3.4 啟動冷靜期（Activate Cooling-Off）

**請求（Request）**
```http
POST /api/v1/player/protection/cooling-off
Authorization: Bearer {token}
Content-Type: application/json

{
  "duration": "D7",
  "reason": "短暫休息"
}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "status": "ACTIVE",
    "startTime": "2026-02-07T10:30:00Z",
    "endTime": "2026-02-14T10:30:00Z",
    "message": "冷靜期已啟動,將於 2026-02-14 自動結束"
  }
}
```

### 3.5 獲取限額使用情況（Get Limit Usage）

**請求（Request）**
```http
GET /api/v1/player/protection/usage
Authorization: Bearer {token}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "depositLimits": {
      "daily": {
        "limit": 1000.00,
        "used": 350.00,
        "remaining": 650.00,
        "percentage": 35,
        "resetTime": "2026-02-08T00:00:00Z"
      },
      "weekly": {
        "limit": 5000.00,
        "used": 1200.00,
        "remaining": 3800.00,
        "percentage": 24,
        "resetTime": "2026-02-12T00:00:00Z"
      },
      "monthly": {
        "limit": 15000.00,
        "used": 4500.00,
        "remaining": 10500.00,
        "percentage": 30,
        "resetTime": "2026-03-01T00:00:00Z"
      }
    },
    "lossLimits": {
      "daily": {
        "limit": 500.00,
        "current": 120.00,
        "remaining": 380.00,
        "percentage": 24,
        "resetTime": "2026-02-08T00:00:00Z"
      },
      "weekly": {
        "limit": 2000.00,
        "current": 450.00,
        "remaining": 1550.00,
        "percentage": 22.5,
        "resetTime": "2026-02-12T00:00:00Z"
      },
      "monthly": null
    },
    "session": {
      "currentDuration": 45,
      "limitMinutes": 60,
      "nextRealityCheck": "2026-02-07T11:00:00Z"
    }
  }
}
```

### 3.6 獲取活動歷史（Get Activity History）

**請求（Request）**
```http
GET /api/v1/player/protection/activity-history
Authorization: Bearer {token}
```

**查詢參數（Query Parameters）**

| 參數 | 類型 | 必填 | 描述 |
|-----------|------|----------|-------------|
| `startDate` | String | 否 | 開始日期 (YYYY-MM-DD) |
| `endDate` | String | 否 | 結束日期 (YYYY-MM-DD) |
| `type` | String | 否 | 活動類型: EXCLUSION, LIMIT_CHANGE, COOLING_OFF, REALITY_CHECK |
| `page` | Integer | 否 | 頁碼 (預設: 1) |
| `pageSize` | Integer | 否 | 每頁大小 (預設: 20) |

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 45,
    "page": 1,
    "pageSize": 20,
    "records": [
      {
        "id": 1001,
        "type": "LIMIT_CHANGE",
        "subType": "DEPOSIT_LIMIT",
        "action": "DECREASE",
        "oldValue": "1500.00",
        "newValue": "1000.00",
        "effectiveTime": "2026-02-06T15:30:00Z",
        "createdAt": "2026-02-06T15:30:00Z"
      },
      {
        "id": 1002,
        "type": "REALITY_CHECK",
        "sessionDuration": 60,
        "netResult": -150.00,
        "response": "CONTINUE",
        "createdAt": "2026-02-05T20:00:00Z"
      }
    ]
  }
}
```

---

## 4. 管理端 API 規格（Admin API Specifications）

### 4.1 列出被排除玩家（List Excluded Players）

**請求（Request）**
```http
GET /api/v1/admin/protection/exclusions
Authorization: Bearer {admin_token}
```

**查詢參數（Query Parameters）**

| 參數 | 類型 | 描述 |
|-----------|------|-------------|
| `status` | String | ACTIVE, COMPLETED, PENDING_REVOCATION |
| `type` | String | SELF, OPERATOR, GAMSTOP |
| `page` | Integer | 頁碼 |
| `pageSize` | Integer | 每頁大小 |

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 128,
    "page": 1,
    "pageSize": 20,
    "records": [
      {
        "id": 5001,
        "playerId": 10001,
        "playerName": "player***",
        "email": "p***@email.com",
        "exclusionType": "SELF",
        "duration": "6M",
        "startTime": "2026-01-15T10:00:00Z",
        "endTime": "2026-07-15T10:00:00Z",
        "status": "ACTIVE",
        "gamstopSynced": true,
        "createdAt": "2026-01-15T10:00:00Z"
      }
    ]
  }
}
```

### 4.2 操作員發起的排除（Operator-Initiated Exclusion）

**請求（Request）**
```http
POST /api/v1/admin/protection/exclusions/{playerId}
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "duration": "1Y",
  "reason": "疑似問題博彩行為",
  "notifyPlayer": true
}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "exclusionId": 5002,
    "playerId": 10002,
    "status": "ACTIVE",
    "message": "玩家已被排除,並已發送通知"
  }
}
```

### 4.3 合規報告（Compliance Reports）

**請求（Request）**
```http
GET /api/v1/admin/protection/reports
Authorization: Bearer {admin_token}
```

**查詢參數（Query Parameters）**

| 參數 | 類型 | 描述 |
|-----------|------|-------------|
| `reportType` | String | MONTHLY, QUARTERLY, ANNUAL |
| `period` | String | 報告期間 (YYYY-MM 或 YYYY-Q1) |
| `format` | String | JSON, CSV, PDF |

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "reportId": "RPT-2026-02",
    "period": "2026-02",
    "generatedAt": "2026-02-07T12:00:00Z",
    "summary": {
      "selfExclusions": {
        "new": 45,
        "active": 128,
        "completed": 12,
        "revoked": 3
      },
      "depositLimits": {
        "playersWithLimits": 3456,
        "limitBreaches": 234,
        "averageLimit": 1250.00
      },
      "lossLimits": {
        "playersWithLimits": 1234,
        "limitBreaches": 89
      },
      "realityChecks": {
        "total": 12456,
        "continueRate": 0.78,
        "averageResponseTime": 8.5
      },
      "gamstopSync": {
        "checksPerformed": 2345,
        "matchesFound": 12,
        "syncFailures": 0
      }
    },
    "downloadUrl": "/api/v1/admin/protection/reports/RPT-2026-02/download"
  }
}
```

### 4.4 Gamstop 查詢（Gamstop Lookup）

**請求（Request）**
```http
POST /api/v1/admin/protection/gamstop/check
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "playerId": 10001,
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "postcode": "SW1A 1AA"
}
```

**響應（Response）**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "excluded": false,
    "checkTime": "2026-02-07T12:00:00Z",
    "reference": "GS-CHK-123456"
  }
}
```

---

## 5. 資料庫結構（Database Schema - PostgreSQL）

### t_player_limit
儲存玩家設定的存款、虧損和會話時長限額。

```sql
CREATE TABLE t_player_limit (
    limit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    player_id UUID NOT NULL REFERENCES players(player_id),
    limit_type VARCHAR(50) NOT NULL, -- 'DEPOSIT', 'LOSS', 'SESSION', 'REALITY_CHECK'
    time_period VARCHAR(20) NOT NULL, -- 'DAILY', 'WEEKLY', 'MONTHLY', 'PER_SESSION'

    -- Limit values
    limit_amount DECIMAL(18,4), -- For deposit/loss limits (NULL for session/reality check)
    limit_duration_minutes INT, -- For session limits (NULL for deposit/loss limits)
    reality_check_interval_minutes INT, -- For reality checks (NULL for other types)

    -- Effective period
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_to TIMESTAMPTZ, -- NULL = active indefinitely

    -- Change management
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'PENDING', 'ACTIVE', 'SUPERSEDED', 'CANCELLED'
    pending_change_id UUID REFERENCES t_player_limit(limit_id), -- Links to future limit change
    cooldown_ends_at TIMESTAMPTZ, -- For limit increases (24-hour cooldown)

    -- Usage tracking
    current_used_amount DECIMAL(18,4) DEFAULT 0.00, -- Real-time usage counter
    reset_at TIMESTAMPTZ, -- Next reset time for daily/weekly/monthly limits

    -- Audit trail
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, -- Player-initiated (player_id) or admin-initiated (admin_id)
    reason TEXT, -- Optional reason provided by player or admin
    metadata JSONB, -- Additional context (e.g., IP address, user agent)

    CONSTRAINT valid_limit_type CHECK (limit_type IN ('DEPOSIT', 'LOSS', 'SESSION', 'REALITY_CHECK')),
    CONSTRAINT valid_time_period CHECK (time_period IN ('DAILY', 'WEEKLY', 'MONTHLY', 'PER_SESSION')),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUPERSEDED', 'CANCELLED')),
    CONSTRAINT valid_limit_amount CHECK (limit_amount IS NULL OR limit_amount > 0),
    CONSTRAINT valid_duration CHECK (limit_duration_minutes IS NULL OR limit_duration_minutes > 0),
    CONSTRAINT hierarchy_check CHECK (
        -- Monthly >= Weekly >= Daily
        (time_period = 'DAILY') OR
        (time_period = 'WEEKLY' AND limit_amount >= (SELECT limit_amount FROM t_player_limit WHERE player_id = t_player_limit.player_id AND time_period = 'DAILY' AND status = 'ACTIVE' AND limit_type = t_player_limit.limit_type LIMIT 1)) OR
        (time_period = 'MONTHLY' AND limit_amount >= (SELECT limit_amount FROM t_player_limit WHERE player_id = t_player_limit.player_id AND time_period = 'WEEKLY' AND status = 'ACTIVE' AND limit_type = t_player_limit.limit_type LIMIT 1))
    )
);

CREATE INDEX idx_t_player_limit_player ON t_player_limit(player_id, status, effective_from) WHERE status = 'ACTIVE';
CREATE INDEX idx_t_player_limit_type_period ON t_player_limit(limit_type, time_period) WHERE status = 'ACTIVE';
CREATE INDEX idx_t_player_limit_pending ON t_player_limit(status, cooldown_ends_at) WHERE status = 'PENDING';
CREATE INDEX idx_t_player_limit_reset ON t_player_limit(reset_at) WHERE status = 'ACTIVE' AND reset_at IS NOT NULL;

COMMENT ON TABLE t_player_limit IS '玩家設定和管理員設定的負責任博彩限額（存款、虧損、會話、現實檢查）';
COMMENT ON COLUMN t_player_limit.cooldown_ends_at IS '限額提高的 24 小時冷卻期（降低則立即生效）';
COMMENT ON COLUMN t_player_limit.current_used_amount IS '實時使用計數器（由存款/虧損交易更新）';
COMMENT ON COLUMN t_player_limit.hierarchy_check IS '強制執行同一 limit_type 的月限額 >= 週限額 >= 日限額';
```

### t_self_exclusion
追蹤自我排除 (Self-Exclusion) 和冷靜期 (Cooling-Off) 期間（玩家發起或操作員發起）。

```sql
CREATE TABLE t_self_exclusion (
    exclusion_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    player_id UUID NOT NULL REFERENCES players(player_id),
    exclusion_type VARCHAR(20) NOT NULL, -- 'SELF', 'OPERATOR', 'GAMSTOP', 'COOLING_OFF'
    duration VARCHAR(20) NOT NULL, -- '24H', '7D', '30D', '6M', '1Y', '5Y', 'PERMANENT'

    -- Time period
    start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time TIMESTAMPTZ, -- NULL for PERMANENT
    actual_end_time TIMESTAMPTZ, -- Actual end (for early revocations)

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'COMPLETED', 'PENDING_REVOCATION', 'REVOKED'
    revocation_reason TEXT, -- Reason for revocation (if applicable)
    revocation_requested_at TIMESTAMPTZ,
    revocation_approved_by UUID REFERENCES admins(admin_id),

    -- Gamstop integration
    gamstop_synced BOOLEAN NOT NULL DEFAULT FALSE,
    gamstop_sync_time TIMESTAMPTZ,
    gamstop_reference VARCHAR(100),

    -- Notification
    player_notified BOOLEAN NOT NULL DEFAULT FALSE,
    notification_sent_at TIMESTAMPTZ,

    -- Audit trail
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, -- Player (player_id) or Admin (admin_id)
    reason TEXT, -- Player or operator reason for exclusion
    metadata JSONB, -- Additional context (IP, user agent, verification method)

    CONSTRAINT valid_exclusion_type CHECK (exclusion_type IN ('SELF', 'OPERATOR', 'GAMSTOP', 'COOLING_OFF')),
    CONSTRAINT valid_duration CHECK (duration IN ('24H', '7D', '30D', '6M', '1Y', '5Y', 'PERMANENT')),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'PENDING_REVOCATION', 'REVOKED')),
    CONSTRAINT permanent_no_end CHECK (
        (duration = 'PERMANENT' AND end_time IS NULL) OR
        (duration != 'PERMANENT' AND end_time IS NOT NULL)
    ),
    CONSTRAINT no_overlap CHECK (
        -- Prevent overlapping active exclusions for same player
        NOT EXISTS (
            SELECT 1 FROM t_self_exclusion se2
            WHERE se2.player_id = t_self_exclusion.player_id
                AND se2.exclusion_id != t_self_exclusion.exclusion_id
                AND se2.status = 'ACTIVE'
                AND se2.start_time < t_self_exclusion.end_time
                AND se2.end_time > t_self_exclusion.start_time
        )
    )
);

CREATE INDEX idx_t_self_exclusion_player ON t_self_exclusion(player_id, status, start_time DESC);
CREATE INDEX idx_t_self_exclusion_active ON t_self_exclusion(status, end_time) WHERE status = 'ACTIVE';
CREATE INDEX idx_t_self_exclusion_pending_revocation ON t_self_exclusion(status, revocation_requested_at) WHERE status = 'PENDING_REVOCATION';
CREATE INDEX idx_t_self_exclusion_gamstop ON t_self_exclusion(gamstop_reference) WHERE gamstop_synced = TRUE;
CREATE INDEX idx_t_self_exclusion_type ON t_self_exclusion(exclusion_type, status);

COMMENT ON TABLE t_self_exclusion IS '負責任博彩合規的自我排除和冷靜期（UKGC/MGA）';
COMMENT ON COLUMN t_self_exclusion.duration IS '排除持續時間: 24H（冷靜期）到 PERMANENT（自我排除）';
COMMENT ON COLUMN t_self_exclusion.gamstop_synced IS 'TRUE 表示排除已與英國 Gamstop 登記系統同步';
COMMENT ON COLUMN t_self_exclusion.no_overlap IS '防止同一玩家的並行活動排除';
```

### 查詢範例（Query Examples）

**獲取玩家的活動限額和使用情況:**
```sql
SELECT
    limit_type,
    time_period,
    limit_amount,
    current_used_amount,
    (limit_amount - current_used_amount) AS remaining,
    (current_used_amount / limit_amount * 100)::DECIMAL(5,2) AS usage_percentage,
    reset_at
FROM t_player_limit
WHERE player_id = 'player-uuid-001'
    AND status = 'ACTIVE'
    AND effective_from <= NOW()
    AND (effective_to IS NULL OR effective_to > NOW())
ORDER BY limit_type, time_period;
```

**檢查玩家當前是否被排除:**
```sql
SELECT
    exclusion_id,
    exclusion_type,
    duration,
    start_time,
    end_time,
    status
FROM t_self_exclusion
WHERE player_id = 'player-uuid-001'
    AND status = 'ACTIVE'
    AND start_time <= NOW()
    AND (end_time IS NULL OR end_time > NOW())
LIMIT 1;
```

**生成合規報告（每月自我排除統計）:**
```sql
SELECT
    exclusion_type,
    COUNT(*) AS total_exclusions,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') AS currently_active,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_this_month,
    COUNT(*) FILTER (WHERE status = 'REVOKED') AS revoked,
    AVG(EXTRACT(EPOCH FROM (COALESCE(actual_end_time, end_time, NOW()) - start_time)) / 86400)::DECIMAL(10,2) AS avg_duration_days
FROM t_self_exclusion
WHERE created_at >= DATE_TRUNC('month', NOW())
    AND created_at < DATE_TRUNC('month', NOW()) + INTERVAL '1 month'
GROUP BY exclusion_type
ORDER BY total_exclusions DESC;
```

**審計限額違規（超過限額的存款）:**
```sql
SELECT
    pl.player_id,
    pl.limit_type,
    pl.time_period,
    pl.limit_amount,
    pl.current_used_amount,
    (pl.current_used_amount - pl.limit_amount) AS breach_amount,
    pl.reset_at
FROM t_player_limit pl
WHERE pl.status = 'ACTIVE'
    AND pl.limit_amount IS NOT NULL
    AND pl.current_used_amount > pl.limit_amount
ORDER BY (pl.current_used_amount - pl.limit_amount) DESC;
```

---

## 6. 錯誤碼（Error Codes）

| 錯誤碼 | 描述 | HTTP 狀態碼 |
|------------|-------------|-------------|
| `RG_001` | 玩家已被排除 | 403 |
| `RG_002` | 玩家處於冷靜期 | 403 |
| `RG_003` | 存款限額已超過 | 400 |
| `RG_004` | 虧損限額已超過 | 400 |
| `RG_005` | 無法撤銷永久排除 | 400 |
| `RG_006` | 排除期間尚未結束 | 400 |
| `RG_007` | 不存在待處理的限額變更 | 400 |
| `RG_008` | 玩家處於強制休息期 | 403 |
| `RG_009` | Gamstop 同步失敗 | 502 |
| `RG_010` | 無效的限額層級 | 400 |

---

## 6. Controller 實現（Controller Implementation）

```java
@RestController
@RequestMapping("/api/v1/player/protection")
@RequiredArgsConstructor
@Tag(name = "玩家保護", description = "負責任博彩相關 API")
public class PlayerProtectionController {

    private final PlayerProtectionService protectionService;
    private final DepositLimitService depositLimitService;
    private final LossLimitService lossLimitService;
    private final SelfExclusionService selfExclusionService;
    private final CoolingOffService coolingOffService;
    private final SessionManagementService sessionService;

    @GetMapping("/settings")
    @Operation(summary = "獲取保護設定總覽")
    public ResponseDTO<ProtectionSettingsVO> getSettings() {
        Long playerId = StpUtil.getLoginIdAsLong();
        return ResponseDTO.ok(protectionService.getSettings(playerId));
    }

    @PutMapping("/deposit-limits")
    @Operation(summary = "設定存款限額")
    public ResponseDTO<LimitUpdateResultVO> setDepositLimits(
            @Valid @RequestBody DepositLimitForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return depositLimitService.setDepositLimits(playerId, form);
    }

    @PutMapping("/loss-limits")
    @Operation(summary = "設定虧損限額")
    public ResponseDTO<LimitUpdateResultVO> setLossLimits(
            @Valid @RequestBody LossLimitForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return lossLimitService.setLossLimits(playerId, form);
    }

    @PostMapping("/self-exclusion")
    @Operation(summary = "申請自我排除")
    public ResponseDTO<ExclusionResultVO> requestSelfExclusion(
            @Valid @RequestBody SelfExclusionForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return selfExclusionService.requestSelfExclusion(playerId, form);
    }

    @PostMapping("/cooling-off")
    @Operation(summary = "啟動冷靜期")
    public ResponseDTO<CoolingOffResultVO> startCoolingOff(
            @Valid @RequestBody CoolingOffForm form) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return coolingOffService.startCoolingOff(playerId, form);
    }

    @GetMapping("/usage")
    @Operation(summary = "獲取限額使用情況")
    public ResponseDTO<LimitUsageVO> getLimitUsage() {
        Long playerId = StpUtil.getLoginIdAsLong();
        return ResponseDTO.ok(protectionService.getLimitUsage(playerId));
    }

    @GetMapping("/activity-history")
    @Operation(summary = "獲取活動歷史")
    public ResponseDTO<PageResult<ActivityHistoryVO>> getActivityHistory(
            @Valid ActivityHistoryQueryForm queryForm) {
        Long playerId = StpUtil.getLoginIdAsLong();
        return ResponseDTO.ok(protectionService.getActivityHistory(playerId, queryForm));
    }
}
```

---

## 7. 整合架構（Integration Architecture）

```mermaid
flowchart TD
    subgraph 玩家端 (Player Facing)
        A[玩家客戶端<br/>Player Client] --> B[PlayerProtectionController]
    end

    subgraph 管理端 (Admin Facing)
        C[管理員控制台<br/>Admin Console] --> D[AdminProtectionController]
    end

    subgraph Service Layer
        B --> E[PlayerProtectionService]
        B --> F[DepositLimitService]
        B --> G[LossLimitService]
        B --> H[SelfExclusionService]
        B --> I[CoolingOffService]
        B --> J[SessionManagementService]
        D --> K[AdminProtectionService]
    end

    subgraph Manager Layer
        E --> L[ProtectionManager]
        F --> L
        G --> L
        H --> L
        I --> L
        K --> L
    end

    subgraph 外部系統 (External Systems)
        L --> M[Gamstop API]
        L --> N[通知服務<br/>Notification Service]
    end

    subgraph 資料層 (Data Layer)
        L --> O[(Protection DB)]
        L --> P[(審計日誌<br/>Audit Log)]
    end
```

---

## 8. 交叉引用（Cross-References）

| 主題 | 文檔 |
|-------|----------|
| 業務需求 | requirements/05_Risk_Compliance/Player_Protection_Requirements.md |
| 自我排除詳情 | source/15_Responsible_Gambling/15-01_Self_Exclusion.md |
| 存款限額詳情 | source/15_Responsible_Gambling/15-02_Deposit_Limits.md |
| API 設計標準 | source/09_Technical_Infrastructure/09-03-04_Domain_APIs.md |

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-12
**維護團隊**: SmartAdmin Architecture Team
