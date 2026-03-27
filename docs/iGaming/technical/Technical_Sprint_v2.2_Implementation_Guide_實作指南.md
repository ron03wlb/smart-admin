---
title: "Sprint v2.2 Gap Supplements — 技術實作指南"
subtitle: "Technical Implementation Guide for 14 Gap Items"
part: technical
module: sprint-v2.2
version: v1.0
created: 2026-03-25
status: Draft
---

# Sprint v2.2 技術實作指南 — 14 項缺漏補充

**版本**: v1.0
**建立日期**: 2026-03-25
**狀態**: 草稿 — 技術審核中

## 目錄

1. [GAP-1: 出金流水驗證時序](#gap-1-出金流水驗證時序)
2. [GAP-2: 代理帳戶 MFA/IP 安全](#gap-2-代理帳戶-mfaip-安全)
3. [GAP-3: 結算審批門檻量化](#gap-3-結算審批門檻量化)
4. [GAP-4: 推薦人反欺詐](#gap-4-推薦人反欺詐)
5. [GAP-5: 第三方資料駐留](#gap-5-第三方資料駐留)
6. [GAP-6: 可負擔性評估超時](#gap-6-可負擔性評估超時)
7. [GAP-7: GAMSTOP 匹配超時](#gap-7-gamstop-匹配超時)
8. [GAP-8: 報表修正監管影響](#gap-8-報表修正監管影響)
9. [GAP-9: 假日出金 SLA](#gap-9-假日出金-sla)
10. [GAP-10: GP 破產玩家保護](#gap-10-gp-破產玩家保護)
11. [N-02: ValidBet 術語統一](#n-02-validbet-術語統一)
12. [F-04: FX 風險分攤](#f-04-fx-風險分攤)
13. [F-05: 事件回應框架](#f-05-事件回應框架)
14. [交叉切割: 配置參數集成](#交叉切割-配置參數集成)

---

## GAP-1: 出金流水驗證時序

### 概述

出金請求到達時，系統執行**同步阻塞流水檢查** (SLA < 200ms)。若流水未完成，拒絕出金；若服務超時（> 500ms），隊列至人工審核。

### API 變更

**新 Endpoint**: `POST /api/v1/withdrawal/verify-wager`

```json
{
  "method": "POST",
  "path": "/api/v1/withdrawal/verify-wager",
  "headers": {
    "Authorization": "Bearer {token}",
    "X-Idempotency-Key": "{uuid}"
  },
  "body": {
    "withdrawal_id": "WD-2026-03-25-001",
    "player_id": 12345,
    "amount": 500.00,
    "currency": "USD",
    "tenant_id": 1
  },
  "response": {
    "status": "success",
    "wager_verification": {
      "status": "COMPLETED",  // COMPLETED | PENDING | PARTIALLY_COMPLETED
      "completed_amount": 500.00,
      "remaining_wagered": 0.00,
      "completion_percentage": 100,
      "last_update_at": "2026-03-25T14:30:00Z",
      "estimated_completion_at": null
    },
    "decision": "APPROVED",  // APPROVED | REJECTED | MANUAL_REVIEW
    "message": "Wager verification passed. Withdrawal approved.",
    "processing_time_ms": 145
  }
}
```

**超時場景** (500ms+ 內無響應):

```json
{
  "decision": "MANUAL_REVIEW",
  "manual_review_queue_id": "MR-2026-03-25-042",
  "message": "Wager service unavailable. Request queued for manual review (SLA: 24h)",
  "player_message": "Your withdrawal is under review. Our team will process it within 24 hours.",
  "auto_escalation_at": "2026-03-26T14:30:00Z",
  "processing_time_ms": 501
}
```

### 資料庫 Schema 變更

**新表**: `t_wager_verification`

```sql
CREATE TABLE t_wager_verification (
    id BIGSERIAL PRIMARY KEY,
    withdrawal_id BIGINT NOT NULL UNIQUE,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    -- Wager status at verification time
    total_wagered DECIMAL(19,4) NOT NULL,
    completed_wagered DECIMAL(19,4) NOT NULL DEFAULT 0,
    remaining_wagered DECIMAL(19,4) NOT NULL,
    completion_percentage DECIMAL(5,2) NOT NULL,

    -- Auto/Manual review flags
    verification_status VARCHAR(20) NOT NULL,  -- COMPLETED, PENDING, PARTIALLY_COMPLETED
    review_decision VARCHAR(20),  -- APPROVED, REJECTED, null (pending manual)
    manual_review_queue_id BIGINT,  -- FK to manual review queue

    -- SLA tracking
    verified_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processing_time_ms INT NOT NULL,
    verification_timeout_exceeded BOOLEAN DEFAULT FALSE,

    -- Audit
    verified_by VARCHAR(100),  -- 'SYSTEM' or agent ID
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_wager_withdrawal FOREIGN KEY (withdrawal_id)
        REFERENCES t_withdrawal(id),
    INDEX idx_wager_player_tenant (player_id, tenant_id),
    INDEX idx_wager_status (verification_status, review_decision)
);

-- Manual review queue for timeout cases
CREATE TABLE t_withdrawal_manual_review_queue (
    id BIGSERIAL PRIMARY KEY,
    withdrawal_id BIGINT NOT NULL UNIQUE,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    reason VARCHAR(200) NOT NULL,  -- 'WAGER_SERVICE_TIMEOUT', 'MANUAL_INSPECTION_REQUIRED'
    risk_level VARCHAR(20),  -- LOW, MEDIUM, HIGH
    queued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_to BIGINT,  -- Agent ID
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, COMPLETED
    decision VARCHAR(20),  -- APPROVED, REJECTED
    decision_reason TEXT,
    decided_at TIMESTAMP,
    sla_expires_at TIMESTAMP NOT NULL,  -- 24h from queue time
    escalated_to BIGINT,  -- Agent ID if escalated
    escalation_level INT DEFAULT 0,  -- 0 = initial, 1 = manager, 2 = director

    CONSTRAINT fk_manual_review_withdrawal FOREIGN KEY (withdrawal_id)
        REFERENCES t_withdrawal(id),
    INDEX idx_queue_player_tenant (player_id, tenant_id),
    INDEX idx_queue_status_sla (status, sla_expires_at)
);

-- Track all verification attempts for audit
CREATE TABLE t_wager_verification_audit (
    id BIGSERIAL PRIMARY KEY,
    withdrawal_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    response_status VARCHAR(20),  -- SUCCESS, TIMEOUT, ERROR
    completed_wagered DECIMAL(19,4),
    processing_time_ms INT,
    error_message TEXT,
    logged_at TIMESTAMP NOT NULL DEFAULT NOW(),

    INDEX idx_audit_withdrawal (withdrawal_id)
);
```

### 狀態機變更

**提款狀態轉移**:

```
INITIATED
  ↓
WAGER_VERIFICATION_IN_PROGRESS (blocking sync call)
  ├─ [200ms response] → WAGER_COMPLETED ✓
  │   ↓
  │   PROCESSING
  ├─ [200ms response] → WAGER_PENDING ✗
  │   ↓
  │   REJECTED_INCOMPLETE_WAGER (show player remaining amount)
  │
  └─ [> 500ms timeout]
      ↓
      MANUAL_REVIEW_QUEUED (no auto-approval)
      ↓
      AWAITING_AGENT_DECISION (SLA 24h)
      ├─ [Agent approves] → PROCESSING
      ├─ [Agent rejects] → REJECTED_BY_AGENT
      └─ [24h SLA expires] → AUTO_ESCALATE → AWAITING_MANAGER
```

### 錯誤處理

```javascript
// Pseudocode for wager verification controller

async function verifyWithdrawalWager(withdrawalId, playerId, amount) {
  const startTime = Date.now();
  const timeout = 500; // ms

  try {
    // Call wager service with timeout
    const wagerResult = await Promise.race([
      wagerService.getPlayerWagerStatus(playerId),
      new Promise((_, reject) =>
        setTimeout(() => reject(new Error('WAGER_SERVICE_TIMEOUT')), timeout)
      )
    ]);

    const processingTime = Date.now() - startTime;

    if (wagerResult.remaining_wagered > 0) {
      // Log failed verification
      await logWagerVerificationAudit(withdrawalId, {
        status: 'INCOMPLETE',
        processingTime,
        remaining: wagerResult.remaining_wagered
      });

      return {
        decision: 'REJECTED',
        reason: 'INCOMPLETE_WAGER',
        remainingWagered: wagerResult.remaining_wagered,
        completionPercentage: wagerResult.completion_percentage
      };
    }

    // Wager completed - proceed
    await logWagerVerificationAudit(withdrawalId, {
      status: 'SUCCESS',
      processingTime,
      remaining: 0
    });

    return { decision: 'APPROVED' };

  } catch (error) {
    if (error.message === 'WAGER_SERVICE_TIMEOUT') {
      const processingTime = Date.now() - startTime;

      // Queue for manual review
      const manualReviewId = await queueManualReview(withdrawalId, playerId, {
        reason: 'WAGER_SERVICE_TIMEOUT',
        riskLevel: 'MEDIUM',
        slaExpiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000)
      });

      // Audit timeout
      await logWagerVerificationAudit(withdrawalId, {
        status: 'TIMEOUT',
        processingTime,
        errorMessage: 'Wager service timeout > 500ms'
      });

      // Notify player
      await playerService.sendNotification(playerId, {
        type: 'WITHDRAWAL_UNDER_REVIEW',
        message: 'Your withdrawal is under review. Our team will process it within 24 hours.',
        manualReviewId
      });

      return { decision: 'MANUAL_REVIEW', manualReviewId };
    }

    // Other errors - escalate
    throw error;
  }
}
```

### 測試需求

**單元測試**:
- ✓ Wager 100% 完成 → 批准 (< 200ms SLA)
- ✓ Wager 50% 完成 → 拒絕 (顯示剩餘金額)
- ✓ Wager 服務超時 (> 500ms) → 隊列至人工 (無自動批准)
- ✓ 24 小時內新流水計算更新 → 重新驗證
- ✓ 玩家 30 天未登入 → 流水自動標記已完成

**端對端測試**:
- ✓ 完整流程：玩家發起出金 → 流水驗證 → 批准 → 支付
- ✓ 超時流程：玩家發起出金 → 服務超時 → 隊列至人工 → 代理批准 → 支付
- ✓ SLA 驗證：99% 驗證在 200ms 內完成

---

## GAP-2: 代理帳戶 MFA/IP 安全

### 概述

代理帳戶需強制 MFA (TOTP/FIDO2)、IP 白名單、敏感操作重新認證、會話超時 (30 min idle)。

### API 變更

**MFA 註冊 Endpoint**: `POST /api/v1/agent/mfa/setup`

```json
{
  "method": "POST",
  "path": "/api/v1/agent/mfa/setup",
  "body": {
    "agent_id": "AG-001",
    "mfa_type": "TOTP"  // TOTP | FIDO2
  },
  "response": {
    "setup_id": "SETUP-2026-03-25-001",
    "qr_code": "data:image/png;base64,...",  // For TOTP
    "secret": "JBSWY3DPEBLW64TMMQ======",
    "backup_codes": ["CODE-001", "CODE-002", ...],
    "next_step": "Scan QR code and enter verification code"
  }
}
```

**IP 白名單管理**: `POST /api/v1/agent/ip-whitelist`

```json
{
  "method": "POST",
  "path": "/api/v1/agent/ip-whitelist",
  "body": {
    "agent_id": "AG-001",
    "ip_address": "203.0.113.45",
    "ip_range": "203.0.113.0/24",  // Optional CIDR notation
    "label": "Office"
  },
  "response": {
    "whitelist_id": "WL-001",
    "ip_address": "203.0.113.45",
    "status": "PENDING_VERIFICATION",
    "verification_code": "VERIFY-123456",
    "message": "IP added. Verify by entering code sent to registered contact."
  }
}
```

**敏感操作重新認證**: `POST /api/v1/agent/sensitive-action/verify`

```json
{
  "method": "POST",
  "path": "/api/v1/agent/sensitive-action/verify",
  "body": {
    "agent_id": "AG-001",
    "action_type": "CREDIT_ADJUSTMENT",  // CREDIT_ADJUSTMENT | SETTLEMENT_APPROVAL
    "action_value": 5000.00,
    "mfa_code": "123456"
  },
  "response": {
    "verification_status": "SUCCESS",
    "re_auth_token": "TOKEN-2026-03-25-001",
    "valid_for_seconds": 300,
    "allowed_action": {
      "type": "CREDIT_ADJUSTMENT",
      "max_value": 5000.00,
      "agent_id": "AG-001"
    }
  }
}
```

### 資料庫 Schema 變更

**Agent Security 表**:

```sql
CREATE TABLE t_agent_mfa_config (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL UNIQUE,
    mfa_type VARCHAR(20) NOT NULL,  -- TOTP, FIDO2
    mfa_secret VARCHAR(255),  -- Encrypted TOTP secret
    fido2_credential_id VARCHAR(255),  -- FIDO2 credential ID
    backup_codes TEXT,  -- JSON array of backup codes, hashed
    enabled BOOLEAN DEFAULT FALSE,
    setup_completed_at TIMESTAMP,
    last_used_at TIMESTAMP,
    failed_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_agent_mfa_agent FOREIGN KEY (agent_id)
        REFERENCES t_agent(id),
    INDEX idx_mfa_agent (agent_id),
    INDEX idx_mfa_locked (locked_until)
);

CREATE TABLE t_agent_ip_whitelist (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    ip_address VARCHAR(45),  -- IPv4 or IPv6
    ip_range VARCHAR(50),  -- CIDR notation (e.g., 203.0.113.0/24)
    label VARCHAR(100),  -- "Office", "Home", etc.
    status VARCHAR(20) DEFAULT 'PENDING_VERIFICATION',  -- PENDING_VERIFICATION, ACTIVE, REVOKED
    verification_code VARCHAR(100),  -- Temporary OTP for verification
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ip_whitelist_agent FOREIGN KEY (agent_id)
        REFERENCES t_agent(id),
    INDEX idx_whitelist_agent_status (agent_id, status),
    UNIQUE (agent_id, ip_address, ip_range)
);

CREATE TABLE t_agent_session (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),

    -- Session management
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_activity_at TIMESTAMP NOT NULL DEFAULT NOW(),
    idle_timeout_seconds INT DEFAULT 1800,  -- 30 minutes
    expires_at TIMESTAMP NOT NULL,
    expired_reason VARCHAR(100),  -- 'IDLE_TIMEOUT', 'EXPLICIT_LOGOUT', 'FORCED_LOGOUT'
    is_active BOOLEAN DEFAULT TRUE,

    -- Re-auth for sensitive operations
    last_sensitive_action_mfa_at TIMESTAMP,
    re_auth_token VARCHAR(255),
    re_auth_valid_until TIMESTAMP,

    INDEX idx_session_agent_active (agent_id, is_active),
    INDEX idx_session_expires (expires_at)
);

-- Audit log for all agent operations
CREATE TABLE t_agent_audit_log (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    action_type VARCHAR(100) NOT NULL,  -- LOGIN, LOGOUT, CREDIT_ADJUSTMENT, SETTLEMENT_APPROVAL, IP_WHITELIST_ADD, MFA_SETUP
    action_description VARCHAR(500),
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    session_id BIGINT,
    status VARCHAR(20),  -- SUCCESS, FAILED
    risk_level VARCHAR(20),  -- LOW, MEDIUM, HIGH (auto-flagged for sensitive actions)
    requires_approval BOOLEAN DEFAULT FALSE,  -- For sensitive operations
    approval_status VARCHAR(20),  -- PENDING, APPROVED, REJECTED
    approved_by BIGINT,  -- Manager/admin agent ID
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_agent FOREIGN KEY (agent_id)
        REFERENCES t_agent(id),
    INDEX idx_audit_agent_action (agent_id, action_type),
    INDEX idx_audit_risk_level (risk_level, created_at),
    INDEX idx_audit_timestamp (created_at)
);
```

### 狀態機變更

**代理登入流程**:

```
LOGIN_INITIATED
  ↓
CREDENTIALS_VERIFIED
  ├─ [MFA 未設置] → MFA_SETUP_REQUIRED (block)
  └─ [MFA 已啟用]
      ↓
      MFA_VERIFICATION_PENDING
      ├─ [MFA 驗證成功] → IP_WHITELIST_CHECK
      │   ├─ [IP in whitelist] → SESSION_CREATED → LOGGED_IN ✓
      │   └─ [IP not in whitelist]
      │       ↓
      │       IP_VERIFICATION_REQUIRED (send OTP)
      │       ├─ [OTP 驗證成功] → SESSION_CREATED → LOGGED_IN ✓
      │       └─ [OTP 失敗] → LOGIN_DENIED
      │
      └─ [MFA 驗證失敗]
          ↓
          LOGIN_FAILED_COUNTER += 1
          ├─ [count >= 5] → ACCOUNT_LOCKED_15MIN
          └─ [count < 5] → MFA_RETRY_REQUIRED

LOGGED_IN (session active)
  ├─ [Idle > 30 min] → SESSION_EXPIRED → LOGOUT
  ├─ [Sensitive action]
  │   ↓
  │   RE_AUTH_REQUIRED (re-enter MFA)
  │   ├─ [Success] → ACTION_ALLOWED
  │   └─ [Failure] → ACTION_DENIED
  │
  └─ [Explicit logout] → SESSION_ENDED
```

### 錯誤處理

```javascript
// Agent MFA middleware

async function agentMFAMiddleware(req, res, next) {
  const agentId = req.user.agent_id;

  // Check if MFA is enabled
  const mfaConfig = await db.query(
    'SELECT * FROM t_agent_mfa_config WHERE agent_id = $1',
    [agentId]
  );

  if (!mfaConfig?.enabled) {
    return res.status(403).json({
      error: 'MFA_NOT_CONFIGURED',
      message: 'MFA is mandatory for agent accounts. Please set up MFA to continue.',
      mfa_setup_required: true
    });
  }

  // For sensitive operations, require re-authentication
  if (isSensitiveAction(req)) {
    const now = new Date();
    const lastMFATime = new Date(mfaConfig.last_used_at);
    const timeSinceLastMFA = (now - lastMFATime) / 1000 / 60; // minutes

    if (timeSinceLastMFA > 5) {  // Re-auth every 5 minutes
      return res.status(403).json({
        error: 'RE_AUTH_REQUIRED',
        message: 'Please enter MFA code to confirm this sensitive action',
        re_auth_endpoint: `/api/v1/agent/sensitive-action/verify`,
        action_type: getActionType(req),
        action_value: req.body.amount
      });
    }
  }

  next();
}

// IP whitelist verification
async function verifyAgentIP(agentId, ipAddress) {
  const whitelist = await db.query(
    'SELECT * FROM t_agent_ip_whitelist WHERE agent_id = $1 AND status = $2',
    [agentId, 'ACTIVE']
  );

  for (const entry of whitelist) {
    if (entry.ip_range) {
      // Check CIDR match
      if (ipInRange(ipAddress, entry.ip_range)) return true;
    } else if (entry.ip_address === ipAddress) {
      return true;
    }
  }

  // IP not in whitelist - send verification code
  const verificationCode = generateOTP();
  await db.query(
    'UPDATE t_agent_ip_whitelist SET verification_code = $1 WHERE agent_id = $2 AND ip_address = $3',
    [verificationCode, agentId, ipAddress]
  );

  // Send OTP via registered contact method
  await notificationService.sendOTP(agentId, verificationCode);

  return false;
}
```

### 測試需求

**單元測試**:
- ✓ MFA 設置流程（TOTP QR 碼、Backup codes）
- ✓ MFA 驗證（5 次失敗鎖定 15 分鐘）
- ✓ IP 白名單驗證（精確 IP、CIDR 範圍、動態 IP）
- ✓ 敏感操作重新認證（信用調整 > $1,000、結算批准 > $10,000）
- ✓ 會話超時（30 分鐘空閒自動登出）

**端對端測試**:
- ✓ 代理首次登入強制 MFA 設置
- ✓ 從新 IP 登入需 IP 驗證
- ✓ 敏感操作（>$1,000 信用調整）需重新 MFA
- ✓ 審計日誌完整記錄所有操作

---

## GAP-3: 結算審批門檻量化

### 概述

結算審批分層：$10K-$50K (經理)、$50K-$100K (CFO)、$100K+ (CFO+CEO)、月度 ≥$500K (合規升級)。支援按品牌/司法管轄區配置。

### API 變更

**配置獲取**: `GET /api/v1/settlement/approval-thresholds`

```json
{
  "method": "GET",
  "path": "/api/v1/settlement/approval-thresholds?brand_id=1&jurisdiction=MT",
  "response": {
    "thresholds": [
      {
        "tier": 1,
        "min_amount": 10000.00,
        "max_amount": 49999.99,
        "approval_level": "MANAGER",
        "required_approvers": 1,
        "sla_hours": 24,
        "description": "Agent Manager approval"
      },
      {
        "tier": 2,
        "min_amount": 50000.00,
        "max_amount": 99999.99,
        "approval_level": "CFO",
        "required_approvers": 1,
        "sla_hours": 24,
        "description": "CFO approval"
      },
      {
        "tier": 3,
        "min_amount": 100000.00,
        "max_amount": null,
        "approval_level": "DUAL_CFO_CEO",
        "required_approvers": 2,
        "sla_hours": 24,
        "description": "CFO + CEO dual approval"
      }
    ],
    "monthly_aggregate_threshold": 500000.00,
    "monthly_aggregate_escalation": "COMPLIANCE_REVIEW",
    "version": 3,
    "effective_date": "2026-03-01",
    "last_updated_at": "2026-03-01T00:00:00Z",
    "updated_by": "admin@platform.com"
  }
}
```

**結算審批提交**: `POST /api/v1/settlement/request-approval`

```json
{
  "method": "POST",
  "path": "/api/v1/settlement/request-approval",
  "body": {
    "settlement_id": "ST-2026-03-25-001",
    "agent_id": "AG-001",
    "amount": 75000.00,
    "currency": "USD",
    "settlement_period": "2026-03-15 to 2026-03-31",
    "breakdown": {
      "player_winnings": 45000.00,
      "bonuses_paid": 20000.00,
      "commissions": 10000.00
    }
  },
  "response": {
    "approval_request_id": "APR-2026-03-25-001",
    "settlement_id": "ST-2026-03-25-001",
    "amount": 75000.00,
    "approval_tier": 2,
    "approval_level": "CFO",
    "status": "AWAITING_CFO_APPROVAL",
    "required_approvers": ["cfo@platform.com"],
    "sla_expires_at": "2026-03-26T14:30:00Z",
    "queue_position": 5,
    "estimated_approval_time_hours": 3
  }
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_approval_threshold (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    brand_id BIGINT,
    jurisdiction_code VARCHAR(10),
    tier INT NOT NULL,

    min_amount DECIMAL(19,4) NOT NULL,
    max_amount DECIMAL(19,4),  -- null = no upper limit
    approval_level VARCHAR(50),  -- MANAGER, CFO, DUAL_CFO_CEO, COMPLIANCE
    required_approvers INT DEFAULT 1,
    sla_hours INT DEFAULT 24,

    -- Config metadata
    active BOOLEAN DEFAULT TRUE,
    version INT DEFAULT 1,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(100),
    change_reason TEXT,

    CONSTRAINT fk_threshold_tenant FOREIGN KEY (tenant_id)
        REFERENCES t_tenant(id),
    CONSTRAINT fk_threshold_brand FOREIGN KEY (brand_id)
        REFERENCES t_brand(id),
    UNIQUE (tenant_id, brand_id, jurisdiction_code, tier, effective_date),
    INDEX idx_threshold_brand_jurisdiction (brand_id, jurisdiction_code)
);

CREATE TABLE t_settlement_approval_request (
    id BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL UNIQUE,
    agent_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    approval_tier INT NOT NULL,
    approval_level VARCHAR(50),  -- MANAGER, CFO, DUAL_CFO_CEO, COMPLIANCE
    required_approvers INT NOT NULL,

    status VARCHAR(30),  -- PENDING, IN_PROGRESS, APPROVED, REJECTED, ESCALATED
    escalation_count INT DEFAULT 0,
    escalated_to_tier INT,
    escalation_reason TEXT,

    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    sla_expires_at TIMESTAMP NOT NULL,
    decision_at TIMESTAMP,

    notes TEXT,
    supporting_documents JSON,  -- Array of document IDs

    CONSTRAINT fk_approval_settlement FOREIGN KEY (settlement_id)
        REFERENCES t_settlement(id),
    CONSTRAINT fk_approval_agent FOREIGN KEY (agent_id)
        REFERENCES t_agent(id),
    INDEX idx_approval_status_sla (status, sla_expires_at),
    INDEX idx_approval_tier (approval_tier),
    INDEX idx_approval_agent (agent_id)
);

CREATE TABLE t_settlement_approval_decision (
    id BIGSERIAL PRIMARY KEY,
    approval_request_id BIGINT NOT NULL,
    settlement_id BIGINT NOT NULL,

    approver_id BIGINT NOT NULL,  -- Admin/Manager ID
    approval_level VARCHAR(50),
    decision VARCHAR(20) NOT NULL,  -- APPROVED, REJECTED
    decision_reason TEXT,
    conditions TEXT,  -- Additional conditions if APPROVED

    approved_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Maker-Checker pattern
    checked_by BIGINT,  -- Secondary approver for dual approval
    checked_at TIMESTAMP,

    CONSTRAINT fk_decision_approval_request FOREIGN KEY (approval_request_id)
        REFERENCES t_settlement_approval_request(id),
    INDEX idx_decision_approver (approver_id, decision)
);

CREATE TABLE t_settlement_approval_audit (
    id BIGSERIAL PRIMARY KEY,
    approval_request_id BIGINT NOT NULL,
    event_type VARCHAR(50),  -- STATUS_CHANGE, ESCALATION, OVERRIDE, SLA_BREACH
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    description TEXT,
    triggered_by VARCHAR(100),
    event_at TIMESTAMP NOT NULL DEFAULT NOW(),

    INDEX idx_audit_request (approval_request_id),
    INDEX idx_audit_event_type (event_type)
);
```

### 狀態機變更

```
SETTLEMENT_INITIATED
  ↓
CALCULATE_APPROVAL_TIER (based on amount & config)
  ├─ [$10K-$50K] → TIER_1_MANAGER_APPROVAL
  ├─ [$50K-$100K] → TIER_2_CFO_APPROVAL
  ├─ [$100K+] → TIER_3_DUAL_APPROVAL
  └─ [Monthly ≥ $500K] → Additional COMPLIANCE_REVIEW

AWAITING_APPROVAL
  ├─ [< 24h, Approved] → APPROVED ✓
  │   ↓
  │   PROCESSING → COMPLETED
  │
  ├─ [< 24h, Rejected] → REJECTED
  │   └─ Resubmit with additional docs?
  │
  ├─ [> 24h, No decision]
  │   ↓
  │   AUTO_ESCALATE → Next Tier
  │   ├─ [T1 → T2] → ESCALATED_TO_CFO
  │   └─ [T2 → T3] → ESCALATED_TO_CEO
  │
  └─ [> 48h, Still pending] → ESCALATED_TO_COMPLIANCE
      → COMPLIANCE_REVIEW
```

### 錯誤處理

```javascript
// Settlement approval service

async function requestSettlementApproval(settlementId, agentId, amount) {
  // Calculate approval tier based on amount
  const tier = await calculateApprovalTier(amount, agentId);

  // Check monthly aggregate
  const monthlyAggregate = await getMonthlySettlementTotal(agentId);
  let hasComplianceEscalation = false;

  if (monthlyAggregate + amount >= 500000) {
    hasComplianceEscalation = true;
  }

  // Get approval thresholds
  const threshold = await db.queryOne(
    `SELECT * FROM t_approval_threshold
     WHERE tier = $1 AND effective_date <= NOW()
     ORDER BY effective_date DESC LIMIT 1`,
    [tier]
  );

  if (!threshold) {
    throw new Error('THRESHOLD_NOT_CONFIGURED');
  }

  // Create approval request
  const approvalReq = await db.queryOne(
    `INSERT INTO t_settlement_approval_request (
      settlement_id, agent_id, amount, approval_tier, approval_level, required_approvers,
      sla_expires_at, status
    ) VALUES ($1, $2, $3, $4, $5, $6, NOW() + INTERVAL '1 day', 'PENDING')
    RETURNING *`,
    [settlementId, agentId, amount, tier, threshold.approval_level, threshold.required_approvers]
  );

  // Route to appropriate queue
  if (hasComplianceEscalation) {
    await escalateToComplianceQueue(approvalReq.id);
  } else {
    await routeToApprovalQueue(approvalReq.id, threshold.approval_level);
  }

  return approvalReq;
}

// Auto-escalation handler (scheduled job)
async function autoEscalateExpiredApprovals() {
  const expiredRequests = await db.query(
    `SELECT * FROM t_settlement_approval_request
     WHERE status = 'PENDING' AND sla_expires_at < NOW()
     AND escalation_count < 2`
  );

  for (const req of expiredRequests) {
    const nextTier = getNextApprovalTier(req.approval_tier);

    await db.query(
      `UPDATE t_settlement_approval_request
       SET status = 'ESCALATED', escalation_count = escalation_count + 1,
           escalated_to_tier = $1
       WHERE id = $2`,
      [nextTier, req.id]
    );

    // Notify escalated approver
    await notificationService.notifyApprovalEscalation(req);

    // Audit trail
    await logApprovalAudit(req.id, {
      eventType: 'ESCALATION',
      description: `Auto-escalated from tier ${req.approval_tier} to ${nextTier}`
    });
  }
}
```

### 測試需求

**單元測試**:
- ✓ Tier 計算邏輯（金額邊界 $10K、$50K、$100K）
- ✓ 月度累計計算（多筆結算正確累積）
- ✓ 自動升級邏輯（24h SLA 超期自動升級）
- ✓ 配置變更版本控制（新版本生效日期正確應用）

**端對端測試**:
- ✓ $75K 結算 → CFO tier 審批
- ✓ $120K 結算 → CEO+CFO 雙重審批
- ✓ 24h 無審批 → 自動升級
- ✓ 月度 ≥$500K → 合規團隊升級

---

## GAP-4: 推薦人反欺詐

### 概述

推薦人必須完成 KYC Level 1+。設備指紋/IP/支付方式欺詐偵測。推薦人 30 天內限 10 人。90 天回收機制。

### API 變更

**推薦驗證 Endpoint**: `POST /api/v1/referral/validate-reward`

```json
{
  "method": "POST",
  "path": "/api/v1/referral/validate-reward",
  "body": {
    "referrer_id": 1001,
    "referred_id": 2001,
    "first_deposit_id": "DEP-2026-03-25-001",
    "amount": 100.00
  },
  "response": {
    "validation_result": "APPROVED",  // APPROVED | SUSPICIOUS | REJECTED
    "reward_id": "REW-2026-03-25-001",
    "referrer_kyc_status": "LEVEL_1_VERIFIED",
    "fraud_checks": {
      "device_fingerprint_match": false,
      "ip_match": false,
      "payment_method_match": false,
      "referrer_monthly_count": 8,  // < 10 limit
      "suspicious_flags": 0
    },
    "reward_payout": {
      "status": "APPROVED",
      "amount": 10.00,
      "processed_at": "2026-03-25T14:30:00Z"
    }
  }
}
```

**可疑推薦升級**: `POST /api/v1/referral/escalate-suspicious`

```json
{
  "referral_id": "REF-2026-03-25-001",
  "escalation_reason": "DEVICE_FINGERPRINT_MATCH",
  "risk_level": "HIGH",
  "queue": "FRAUD_INVESTIGATION"
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_referral_fraud_check (
    id BIGSERIAL PRIMARY KEY,
    referral_id BIGINT NOT NULL UNIQUE,
    referrer_id BIGINT NOT NULL,
    referred_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    -- KYC Check
    referrer_kyc_level VARCHAR(20),  -- LEVEL_0, LEVEL_1, LEVEL_2, LEVEL_3
    referrer_kyc_complete BOOLEAN NOT NULL,
    referrer_kyc_verified_at TIMESTAMP,
    kyc_check_passed BOOLEAN DEFAULT TRUE,

    -- Device Fingerprint
    referrer_device_fingerprint VARCHAR(255),
    referred_device_fingerprint VARCHAR(255),
    device_fingerprint_match BOOLEAN DEFAULT FALSE,
    device_match_score DECIMAL(5,2),  -- 0-100

    -- IP Address
    referrer_ip_address VARCHAR(45),
    referred_ip_address VARCHAR(45),
    ip_match BOOLEAN DEFAULT FALSE,
    ip_geolocation_same_country BOOLEAN,
    ip_geolocation_same_region BOOLEAN,

    -- Payment Method
    referrer_primary_payment_method VARCHAR(100),
    referred_primary_payment_method VARCHAR(100),
    payment_method_match BOOLEAN DEFAULT FALSE,
    bank_account_match BOOLEAN DEFAULT FALSE,

    -- Referrer monthly limit tracking
    referrer_monthly_count INT NOT NULL DEFAULT 0,  -- Count in last 30 days
    referrer_monthly_limit_exceeded BOOLEAN DEFAULT FALSE,

    -- Fraud flags
    suspicious_level VARCHAR(20),  -- LOW, MEDIUM, HIGH
    fraud_flags JSON,  -- Array of flagged issues
    auto_escalated BOOLEAN DEFAULT FALSE,

    -- Status
    validation_status VARCHAR(20),  -- PENDING, APPROVED, SUSPICIOUS, REJECTED
    investigation_status VARCHAR(20),  -- null, IN_PROGRESS, COMPLETED
    investigation_result VARCHAR(20),  -- null, LEGITIMATE, FRAUDULENT

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_fraud_referral FOREIGN KEY (referral_id)
        REFERENCES t_referral(id),
    INDEX idx_fraud_referrer (referrer_id),
    INDEX idx_fraud_status (validation_status, suspicious_level),
    INDEX idx_fraud_monthly (referrer_id, created_at)
);

CREATE TABLE t_referral_clawback (
    id BIGSERIAL PRIMARY KEY,
    referral_id BIGINT NOT NULL,
    referred_id BIGINT NOT NULL,
    referrer_id BIGINT NOT NULL,

    original_reward_amount DECIMAL(19,4) NOT NULL,
    clawback_amount DECIMAL(19,4) NOT NULL,
    clawback_reason VARCHAR(50),  -- FRAUD_DETECTED, ACCOUNT_DISABLED, 90_DAY_BREACH

    trigger_event_id BIGINT,  -- Account disable event, fraud discovery, etc.
    trigger_event_date TIMESTAMP,
    clawback_processed_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Reversal transaction
    reversal_transaction_id BIGINT,

    CONSTRAINT fk_clawback_referral FOREIGN KEY (referral_id)
        REFERENCES t_referral(id),
    INDEX idx_clawback_referrer (referrer_id),
    INDEX idx_clawback_reason (clawback_reason)
);

-- Device fingerprint history
CREATE TABLE t_player_device_fingerprint (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500),
    screen_resolution VARCHAR(20),
    plugins_hash VARCHAR(255),

    last_seen_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_device_player FOREIGN KEY (player_id)
        REFERENCES t_player(id),
    UNIQUE (player_id, device_fingerprint),
    INDEX idx_device_fingerprint (device_fingerprint)
);
```

### 錯誤處理

```javascript
// Referral validation service

async function validateReferralReward(referrerId, referredId, depositId) {
  // 1. Check referrer KYC
  const referrerKYC = await db.queryOne(
    'SELECT kyc_level, verified_at FROM t_player_kyc WHERE player_id = $1',
    [referrerId]
  );

  if (!referrerKYC || referrerKYC.kyc_level < 1) {
    return {
      validation_result: 'REJECTED',
      reason: 'REFERRER_KYC_INCOMPLETE',
      kyc_status: referrerKYC?.kyc_level || 'LEVEL_0',
      reward_status: 'HELD_PENDING_KYC'
    };
  }

  // 2. Fraud checks
  const fraudChecks = await performFraudChecks(referrerId, referredId);

  if (fraudChecks.suspicious_level === 'HIGH' ||
      fraudChecks.multiple_fraud_flags) {
    // Auto-escalate to investigation queue
    const investigation = await createInvestigation(referrerId, referredId, fraudChecks);

    return {
      validation_result: 'SUSPICIOUS',
      reason: 'FRAUD_INDICATORS_DETECTED',
      fraud_checks: fraudChecks,
      investigation_id: investigation.id,
      reward_status: 'HELD_FOR_INVESTIGATION'
    };
  }

  // 3. Check referrer monthly limit
  const monthlyCount = await getMonthlyReferralCount(referrerId);
  if (monthlyCount >= 10) {
    return {
      validation_result: 'REJECTED',
      reason: 'MONTHLY_REFERRAL_LIMIT_EXCEEDED',
      monthly_count: monthlyCount,
      limit: 10
    };
  }

  // 4. Approve and process reward
  const reward = await processReferralReward(referrerId, referredId, depositId);

  return {
    validation_result: 'APPROVED',
    reward_id: reward.id,
    reward_amount: reward.amount,
    processed_at: reward.processed_at
  };
}

async function performFraudChecks(referrerId, referredId) {
  const referrer = await db.queryOne(
    'SELECT device_fingerprint, ip_address FROM t_player_session WHERE player_id = $1',
    [referrerId]
  );

  const referred = await db.queryOne(
    'SELECT device_fingerprint, ip_address FROM t_player_session WHERE player_id = $1',
    [referredId]
  );

  const flags = [];

  // Device match
  if (referrer.device_fingerprint === referred.device_fingerprint) {
    flags.push('DEVICE_FINGERPRINT_MATCH');
  }

  // IP match
  if (referrer.ip_address === referred.ip_address) {
    flags.push('IP_ADDRESS_MATCH');
  }

  // Payment method match
  const referrerPayment = await getPlayerPaymentMethod(referrerId);
  const referredPayment = await getPlayerPaymentMethod(referredId);

  if (referrerPayment.bank_account_id === referredPayment.bank_account_id) {
    flags.push('PAYMENT_METHOD_MATCH');
  }

  return {
    fraud_flags: flags,
    suspicious_level: flags.length >= 2 ? 'HIGH' : (flags.length === 1 ? 'MEDIUM' : 'LOW')
  };
}

// 90-day clawback scheduler
async function checkReferralClawbackTriggers() {
  // Find referred accounts disabled in last 90 days
  const disabledAccounts = await db.query(
    `SELECT player_id FROM t_player_account_action
     WHERE action_type = 'ACCOUNT_DISABLED'
     AND action_date > NOW() - INTERVAL '90 days'`
  );

  for (const account of disabledAccounts) {
    const referralRecord = await db.queryOne(
      'SELECT * FROM t_referral WHERE referred_id = $1',
      [account.player_id]
    );

    if (referralRecord && !referralRecord.clawback_processed) {
      // Clawback the reward
      await processClawback(referralRecord.id, 'ACCOUNT_DISABLED');
    }
  }
}
```

### 測試需求

**單元測試**:
- ✓ 推薦人 KYC < Level 1 → 拒絕
- ✓ 設備指紋匹配 → 可疑標記
- ✓ IP 地址匹配 → 可疑標記
- ✓ 支付方式匹配 → 可疑標記
- ✓ 30 天 > 10 推薦 → 拒絕超額部分
- ✓ 90 天內被禁用 → 回收獎勵

**端對端測試**:
- ✓ 正常推薦流程（不同設備/IP/支付）→ 批准
- ✓ 可疑推薦（相同設備指紋）→ 升級調查
- ✓ 違規推薦（>10/月）→ 後续拒絕
- ✓ 回收流程（被推薦人禁用）→ 獎勵自動回收

---

## GAP-5: 第三方資料駐留

### 概述

規格骨架 — 待法務確認。EU PII 儲存在 EU 資料中心。SCC/DPA 簽署。年度審計。玩家權利（存取、修正、刪除、可攜性）。

### API 變更（框架）

**玩家資料存取請求**: `POST /api/v1/compliance/player-data-request`

```json
{
  "request_type": "DATA_SUBJECT_ACCESS",  // DATA_SUBJECT_ACCESS | DATA_DELETION | DATA_PORTABILITY | DATA_CORRECTION
  "player_id": 1001,
  "request_reason": "GDPR Article 15"
}
```

**第三方審計狀態**: `GET /api/v1/compliance/third-party-audits`

```json
{
  "third_parties": [
    {
      "provider_id": "PSP-001",
      "provider_name": "PaymentGatewayXYZ",
      "data_residency_jurisdiction": "EU",
      "scc_signed": true,
      "scc_signature_date": "2025-01-15",
      "dpa_signed": true,
      "dpa_effective_date": "2025-01-15",
      "last_audit_date": "2025-09-20",
      "next_audit_due": "2026-09-20",
      "audit_status": "COMPLIANT",
      "major_findings": 0
    }
  ]
}
```

### 資料庫 Schema（框架）

```sql
-- Third-party SCC/DPA tracking
CREATE TABLE t_third_party_data_contract (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    third_party_id BIGINT NOT NULL,
    contract_type VARCHAR(20),  -- SCC, DPA

    data_transfer_direction VARCHAR(50),  -- INBOUND, OUTBOUND, BIDIRECTIONAL
    data_categories TEXT,  -- JSON array of data types (PII, KYC, Transaction, etc.)
    transfer_jurisdictions VARCHAR(500),  -- e.g., "EU→US", "EU→Philippines"

    signed_date DATE NOT NULL,
    signature_file_id BIGINT,
    signer_name VARCHAR(100),
    signer_title VARCHAR(100),

    effective_date DATE NOT NULL,
    expiry_date DATE,
    renewal_required BOOLEAN DEFAULT FALSE,

    review_schedule VARCHAR(50),  -- ANNUAL, BI_ANNUAL
    next_review_date DATE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_contract_third_party FOREIGN KEY (third_party_id)
        REFERENCES t_third_party_provider(id),
    INDEX idx_contract_jurisdiction (transfer_jurisdictions)
);

-- Third-party audit schedule
CREATE TABLE t_third_party_audit (
    id BIGSERIAL PRIMARY KEY,
    third_party_id BIGINT NOT NULL,
    audit_type VARCHAR(50),  -- SECURITY, COMPLIANCE, DATA_PROTECTION

    planned_date DATE NOT NULL,
    audit_start_date DATE,
    audit_end_date DATE,

    audit_questionnaire_id BIGINT,
    questionnaire_sent_date TIMESTAMP,
    questionnaire_received_date TIMESTAMP,

    audit_status VARCHAR(20),  -- PLANNED, IN_PROGRESS, COMPLETED
    audit_report_id BIGINT,
    findings_count INT,
    major_findings INT,
    recommendations TEXT,

    compliance_score INT,  -- 0-100
    action_items TEXT,  -- JSON array of required actions
    action_items_due_date DATE,

    next_audit_due DATE,

    CONSTRAINT fk_audit_third_party FOREIGN KEY (third_party_id)
        REFERENCES t_third_party_provider(id),
    INDEX idx_audit_status_due (audit_status, next_audit_due)
);

-- Player data subject rights requests
CREATE TABLE t_player_data_subject_request (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    request_type VARCHAR(50),  -- DATA_SUBJECT_ACCESS, DATA_DELETION, DATA_PORTABILITY, DATA_CORRECTION
    request_date TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20),  -- PENDING, IN_PROGRESS, COMPLETED, DENIED

    completion_target_date DATE,
    completed_date TIMESTAMP,

    data_categories_requested TEXT,  -- JSON array
    third_parties_involved TEXT,  -- JSON array of provider IDs

    request_details TEXT,
    denial_reason TEXT,  -- If DENIED

    CONSTRAINT fk_data_request_player FOREIGN KEY (player_id)
        REFERENCES t_player(id),
    INDEX idx_data_request_status (request_type, status)
);
```

### 測試需求（框架）

- ✓ SCC/DPA 簽署驗證
- ✓ 年度審計計畫和執行
- ✓ 玩家存取請求處理 (< 30 days SLA)
- ✓ 玩家刪除請求級聯至第三方
- ✓ 資料洩露事件 72h 通知流程

---

## GAP-6: 可負擔性評估超時

### 概述

評估超時 7 天時，允許進行中遊戲自然完成（不強制中斷）。新下注被拒。餘額可提取。獎勵流水計時器暫停。

### API 變更

**遊戲隊列檢查**: `GET /api/v1/game/session/validate-bet`

```json
{
  "method": "GET",
  "path": "/api/v1/game/session/validate-bet?player_id=1001&game_session_id=GS-001",
  "response": {
    "can_place_bet": false,
    "reason": "AFFORDABILITY_ASSESSMENT_PENDING",
    "assessment_status": "OVERDUE",
    "days_overdue": 2,
    "current_round": {
      "round_id": "RD-001",
      "can_complete": true,
      "message": "Current round will be allowed to complete naturally"
    }
  }
}
```

**提取請求（評估暫停期間）**: `POST /api/v1/wallet/withdraw-during-assessment`

```json
{
  "amount": 500.00,
  "reason": "AFFORDABILITY_ASSESSMENT_PENDING",
  "response": {
    "withdrawal_approved": true,
    "withdrawal_id": "WD-001",
    "message": "Withdrawal approved. Assessment remain pending - gaming access still restricted.",
    "gaming_access_not_restored": true
  }
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_affordability_assessment (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    trigger_event VARCHAR(50),  -- DEPOSIT_SURGE, HIGH_LOSS_RATE, VELOCITY_PATTERN, MANUAL_REVIEW
    assessment_type VARCHAR(50),  -- QUESTIONNAIRE, DOCUMENT_SUBMISSION, THIRD_PARTY_REVIEW

    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    due_date TIMESTAMP NOT NULL,  -- NOW() + 7 days

    -- Assessment completion
    status VARCHAR(30),  -- PENDING, SUBMITTED, UNDER_REVIEW, COMPLETED, OVERDUE, EXPIRED
    submitted_at TIMESTAMP,
    completed_at TIMESTAMP,
    completion_result VARCHAR(20),  -- PASSED, FAILED, INCONCLUSIVE

    -- Impact on account
    deposits_blocked BOOLEAN DEFAULT TRUE,
    gaming_access_blocked BOOLEAN DEFAULT TRUE,
    withdrawal_allowed BOOLEAN DEFAULT TRUE,

    -- Current round tracking
    current_game_session_id BIGINT,
    current_round_allowed BOOLEAN DEFAULT TRUE,
    round_completion_required BOOLEAN DEFAULT FALSE,

    -- Bonus wagering pause
    bonus_wager_paused BOOLEAN DEFAULT FALSE,
    bonus_wager_pause_start TIMESTAMP,
    bonus_wager_pause_end TIMESTAMP,
    bonus_wager_paused_amount DECIMAL(19,4),

    -- Overdue handling
    is_overdue BOOLEAN DEFAULT FALSE,
    overdue_since TIMESTAMP,
    days_overdue INT,
    auto_marked_expired BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_assessment_player FOREIGN KEY (player_id)
        REFERENCES t_player(id),
    INDEX idx_assessment_status_due (status, due_date),
    INDEX idx_assessment_overdue (is_overdue, overdue_since)
);

-- Track gaming activity during assessment
CREATE TABLE t_game_session_during_assessment (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    game_session_id BIGINT NOT NULL,

    round_id BIGINT NOT NULL,
    round_start_time TIMESTAMP,
    round_end_time TIMESTAMP,

    bet_allowed BOOLEAN,  -- FALSE if new bet attempted during assessment pause
    bet_amount DECIMAL(19,4),
    bet_status VARCHAR(20),  -- ALLOWED, BLOCKED, PENDING_ROUND_COMPLETION

    CONSTRAINT fk_session_assessment FOREIGN KEY (assessment_id)
        REFERENCES t_affordability_assessment(id),
    INDEX idx_session_player_assessment (player_id, assessment_id)
);
```

### 狀態機變更

```
AFFORDABILITY_ASSESSMENT_TRIGGERED
  ↓
ASSESSMENT_PENDING (7 day SLA)
  ├─ [Deposits blocked] ✓
  ├─ [Gaming access blocked (new bets)] ✓
  ├─ [Withdrawals allowed] ✓
  ├─ [Bonus wager timer paused] ✓
  │
  ├─ [Player completes assessment < 7 days]
  │   ↓
  │   SUBMITTED → UNDER_RISK_REVIEW (4h SLA)
  │   ├─ [Risk assessment passes] → CLEARED → Restore all access
  │   └─ [Risk assessment fails] → FAILED → Maintain restrictions
  │
  ├─ [7 days passed, assessment not submitted]
  │   ↓
  │   OVERDUE → AUTO_MARK_EXPIRED
  │   └─ [If player returns] → REASSESSMENT_REQUIRED
  │
  └─ [Assessment SLA expires during assessment]
      └─ [Auto-restore deposits & gaming if no other risk flags]
```

### 錯誤處理

```javascript
// Game session validation during affordability assessment

async function validateBetDuringAssessment(playerId, gameSessionId, betAmount) {
  const assessment = await db.queryOne(
    `SELECT * FROM t_affordability_assessment
     WHERE player_id = $1 AND status = 'PENDING'`,
    [playerId]
  );

  if (!assessment) {
    return { can_place_bet: true };  // No pending assessment
  }

  const currentRound = await getCurrentGameRound(gameSessionId);

  // Allow current round to complete
  if (currentRound && currentRound.is_active) {
    return {
      can_place_bet: false,
      reason: 'AFFORDABILITY_ASSESSMENT_PENDING',
      current_round: {
        round_id: currentRound.id,
        can_complete: true,
        message: 'Your current round will be allowed to complete naturally'
      }
    };
  }

  // New round/bet - blocked
  return {
    can_place_bet: false,
    reason: 'AFFORDABILITY_ASSESSMENT_PENDING',
    assessment_due_date: assessment.due_date,
    message: 'Please complete your affordability assessment to resume gaming.'
  };
}

// Auto-expire overdue assessments (scheduled job)
async function autoExpireOverdueAssessments() {
  const overdueAssessments = await db.query(
    `SELECT * FROM t_affordability_assessment
     WHERE status = 'PENDING' AND due_date < NOW()
     AND auto_marked_expired = FALSE`
  );

  for (const assessment of overdueAssessments) {
    // Mark as expired
    await db.query(
      `UPDATE t_affordability_assessment
       SET is_overdue = TRUE, overdue_since = NOW(),
           auto_marked_expired = TRUE, status = 'EXPIRED'
       WHERE id = $1`,
      [assessment.id]
    );

    // Check if account has other risk flags
    const otherRiskFlags = await checkOtherRiskFlags(assessment.player_id);

    if (!otherRiskFlags) {
      // Auto-restore access
      await db.query(
        `UPDATE t_affordability_assessment
         SET deposits_blocked = FALSE, gaming_access_blocked = FALSE
         WHERE id = $1`,
        [assessment.id]
      );
    }

    // Resume bonus wager timer if paused
    if (assessment.bonus_wager_paused) {
      await resumeBonusWagerTimer(assessment.player_id);
    }
  }
}
```

### 測試需求

- ✓ 評估超時時遊戲自然完成（回合不被中斷）
- ✓ 新下注被拒（清晰訊息）
- ✓ 餘額可提取（無手續費）
- ✓ 獎勵流水計時器暫停/恢復
- ✓ 4h SLA 風控審查
- ✓ 評估逾期 7 天自動標記為過期

---

## GAP-7: GAMSTOP 匹配超時

### 概述

GAMSTOP 匹配 (≥70%) 時自動暫停。24h/48h/72h 升級。維持暫停（絕不自動解除）。合規人員審查確認/駁回。

### API 變更

**GAMSTOP 匹配檢查**: `POST /api/v1/compliance/gamstop-check`

```json
{
  "method": "POST",
  "path": "/api/v1/compliance/gamstop-check",
  "body": {
    "player_id": 1001,
    "player_name": "John Doe",
    "date_of_birth": "1980-01-15"
  },
  "response": {
    "match_status": "MATCH_DETECTED",
    "similarity_score": 0.75,  // 75%
    "matched_self_exclusion_records": [
      {
        "record_id": "SE-2025-06-001",
        "register_date": "2025-06-15",
        "exclusion_period": "INDEFINITE"
      }
    ],
    "action_taken": "ACCOUNT_SUSPENDED",
    "suspension_start": "2026-03-25T14:30:00Z",
    "manual_review_required": true,
    "manual_review_queue_id": "GAMSTOP-MR-001",
    "auto_escalation_schedule": [
      {
        "hours": 24,
        "escalate_to": "COMPLIANCE_MANAGER",
        "status": "PENDING"
      },
      {
        "hours": 48,
        "escalate_to": "MLRO",
        "status": "PENDING"
      },
      {
        "hours": 72,
        "escalate_to": "COMPLIANCE_DIRECTOR",
        "status": "PENDING"
      }
    ]
  }
}
```

**GAMSTOP 決定 Endpoint**: `POST /api/v1/compliance/gamstop-decision`

```json
{
  "method": "POST",
  "path": "/api/v1/compliance/gamstop-decision",
  "body": {
    "manual_review_id": "GAMSTOP-MR-001",
    "decision": "CONFIRMED_MATCH",  // CONFIRMED_MATCH | FALSE_POSITIVE
    "decision_reason": "Name, DOB, and address match GAMSTOP record SE-2025-06-001",
    "reviewer_id": "CM-001",
    "reviewed_at": "2026-03-25T16:45:00Z"
  },
  "response": {
    "decision_recorded": true,
    "account_action": "PERMANENTLY_DISABLED",
    "gamstop_report_sent": true,
    "gamstop_report_id": "GR-2026-03-25-001",
    "player_notification_sent": true,
    "notification_id": "PN-2026-03-25-001"
  }
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_gamstop_match (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    match_check_date TIMESTAMP NOT NULL DEFAULT NOW(),
    similarity_score DECIMAL(5,2),  -- 0-100
    matched_records JSON,  -- Array of GAMSTOP records matched

    -- Suspension status
    account_suspended_at TIMESTAMP NOT NULL,
    suspension_status VARCHAR(30),  -- SUSPENDED, PENDING_REVIEW, CONFIRMED_DISABLED, FALSE_POSITIVE_RESTORED

    -- Manual review
    manual_review_queue_id BIGINT,
    review_assigned_to BIGINT,  -- Agent ID
    review_assigned_at TIMESTAMP,

    -- Escalation tracking
    escalation_level INT DEFAULT 0,  -- 0 = initial, 1 = manager, 2 = MLRO, 3 = director
    next_escalation_at TIMESTAMP,
    last_escalation_at TIMESTAMP,
    escalation_history JSON,  -- Array of escalations with timestamps

    -- Decision
    decision VARCHAR(30),  -- CONFIRMED_MATCH, FALSE_POSITIVE
    decision_made_by BIGINT,  -- Agent ID
    decision_made_at TIMESTAMP,
    decision_reason TEXT,

    -- GAMSTOP report
    gamstop_report_id VARCHAR(100),
    gamstop_reported_at TIMESTAMP,
    gamstop_report_status VARCHAR(20),  -- SUBMITTED, ACKNOWLEDGED, REJECTED

    -- Player notification
    player_notified_at TIMESTAMP,
    notification_message TEXT,
    appeal_allowed BOOLEAN DEFAULT FALSE,
    appeal_deadline TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_gamstop_player FOREIGN KEY (player_id)
        REFERENCES t_player(id),
    INDEX idx_gamstop_player_status (player_id, suspension_status),
    INDEX idx_gamstop_escalation (escalation_level, next_escalation_at)
);
```

### 狀態機變更

```
GAMSTOP_CHECK_TRIGGERED (≥70% match)
  ↓
ACCOUNT_SUSPENDED
  │
  ├─ MANUAL_REVIEW_QUEUED (SLA: 24h for initial review)
  │   ├─ [Reviewed < 24h, Confirmed] → PERMANENTLY_DISABLED → Report to GAMSTOP ✓
  │   ├─ [Reviewed < 24h, False Positive] → ACCOUNT_RESTORED
  │   │
  │   └─ [No review by 24h]
  │       ↓
  │       AUTO_ESCALATE_LEVEL_1 (Compliance Manager notified)
  │       ├─ [Still no review by 48h]
  │       │   ↓
  │       │   AUTO_ESCALATE_LEVEL_2 (MLRO notified)
  │       │   ├─ [Still no review by 72h]
  │       │   │   ↓
  │       │   │   AUTO_ESCALATE_LEVEL_3 (Director notified)
  │       │   │   └─ [Possible regulatory report if escalation continues]
  │       │   │
  │       │   └─ [MLRO reviews, decides] → FINAL DECISION
  │       │
  │       └─ [Manager reviews, decides] → FINAL DECISION
  │
  └─ [Account remains suspended until decision made - NO AUTO-RELEASE]
```

### 錯誤處理

```javascript
// GAMSTOP auto-escalation scheduler

async function checkGAMSTOPEscalations() {
  const matchesAwaitingReview = await db.query(
    `SELECT * FROM t_gamstop_match
     WHERE suspension_status = 'PENDING_REVIEW'
     AND account_suspended_at < NOW() - INTERVAL '24 hours'
     AND escalation_level < 3`
  );

  for (const match of matchesAwaitingReview) {
    const hoursSinceInit = getHoursSince(match.account_suspended_at);
    let escalateLevel = match.escalation_level;
    let escalateTo = null;

    if (hoursSinceInit >= 72) {
      escalateLevel = 3;
      escalateTo = 'COMPLIANCE_DIRECTOR';
    } else if (hoursSinceInit >= 48) {
      escalateLevel = 2;
      escalateTo = 'MLRO';
    } else if (hoursSinceInit >= 24) {
      escalateLevel = 1;
      escalateTo = 'COMPLIANCE_MANAGER';
    }

    if (escalateTo) {
      // Update escalation
      await db.query(
        `UPDATE t_gamstop_match
         SET escalation_level = $1, last_escalation_at = NOW(),
             next_escalation_at = NOW() + INTERVAL '1 hour'
         WHERE id = $2`,
        [escalateLevel, match.id]
      );

      // Send escalation notification
      const escalateUser = await getEscalationUser(escalateTo);
      await notificationService.sendAlert(escalateUser, {
        type: 'GAMSTOP_ESCALATION',
        gamstop_id: match.id,
        escalation_level: escalateLevel,
        hours_pending: hoursSinceInit,
        account_id: match.player_id
      });

      // Log escalation
      await logEscalation(match.id, {
        level: escalateLevel,
        escalated_to: escalateTo,
        triggered_at: new Date()
      });
    }
  }
}

// CRITICAL: Never auto-release suspended accounts
async function makeGAMSTOPDecision(matchId, decision, reviewerId, reason) {
  const match = await db.queryOne(
    'SELECT * FROM t_gamstop_match WHERE id = $1',
    [matchId]
  );

  if (!match) throw new Error('GAMSTOP_MATCH_NOT_FOUND');

  // Record decision
  await db.query(
    `UPDATE t_gamstop_match
     SET decision = $1, decision_made_by = $2, decision_made_at = NOW(),
         decision_reason = $3
     WHERE id = $4`,
    [decision, reviewerId, reason, matchId]
  );

  if (decision === 'CONFIRMED_MATCH') {
    // Permanently disable account
    await disablePlayerAccount(match.player_id, 'GAMSTOP_MATCH_CONFIRMED');

    // Report to GAMSTOP
    const reportId = await reportToGAMSTOP(match);

    // Notify player
    await playerService.sendNotification(match.player_id, {
      type: 'ACCOUNT_PERMANENTLY_DISABLED',
      reason: 'GAMSTOP Self-Exclusion Match Confirmed',
      appeal_possible: true,
      appeal_deadline: addDays(new Date(), 30)
    });

  } else if (decision === 'FALSE_POSITIVE') {
    // Restore account (but don't auto-release suspension)
    await db.query(
      `UPDATE t_gamstop_match
       SET suspension_status = 'FALSE_POSITIVE_RESTORED'
       WHERE id = $1`,
      [matchId]
    );

    // Notify player
    await playerService.sendNotification(match.player_id, {
      type: 'ACCOUNT_REVIEW_COMPLETED',
      result: 'Your account has been reviewed and restored. No action was taken.'
    });
  }
}
```

### 測試需求

- ✓ ≥70% 匹配自動暫停帳戶
- ✓ 24h/48h/72h 自動升級通知
- ✓ 帳戶維持暫停（絕不自動解除）
- ✓ 合規人員確認/駁回決定記錄
- ✓ 禁用決定報告至 GAMSTOP (API 集成)
- ✓ 玩家通知在匹配/禁用時發送
- ✓ SLA 監控儀表板顯示待審查清單

---

## GAP-8: 報表修正監管影響

### 概述

規格骨架 — 待合規確認。修正涉及已提交報告時自動標記。實質性門檻 0.1%。修正通知生成。監管機構報告決定流程。

### API 變更（框架）

**報表修正提交**: `POST /api/v1/reporting/submit-correction`

```json
{
  "report_id": "RPT-2026-02-001",
  "field_name": "total_player_deposits",
  "original_value": 1000000.00,
  "corrected_value": 1005000.00,
  "correction_reason": "Duplicate transaction reversal",
  "response": {
    "correction_recorded": true,
    "correction_id": "CORR-2026-03-25-001",
    "impacts_regulatory_report": true,
    "materiality_threshold_exceeded": true,
    "materiality_percentage": 0.5,  // > 0.1%
    "requires_regulatory_notice": true,
    "compliance_review_queue_id": "CRV-001"
  }
}
```

### 資料庫 Schema（框架）

```sql
CREATE TABLE t_correction_audit_log (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,

    original_value DECIMAL(19,4),
    corrected_value DECIMAL(19,4),
    difference DECIMAL(19,4),

    report_total_value DECIMAL(19,4),
    materiality_percentage DECIMAL(7,4),  -- Calculate as (difference / total) * 100
    is_material BOOLEAN,  -- TRUE if > 0.1%

    correction_reason VARCHAR(500),
    correction_category VARCHAR(50),  -- DUPLICATE_TRANSACTION, INPUT_ERROR, SYSTEM_FAULT, DATA_DISCOVERY

    corrected_by VARCHAR(100),
    corrected_at TIMESTAMP NOT NULL DEFAULT NOW(),

    regulatory_impact BOOLEAN DEFAULT FALSE,
    affected_reports JSON,  -- Array of report IDs impacted
    requires_regulatory_notice BOOLEAN,

    compliance_review_status VARCHAR(20),  -- PENDING, APPROVED, DENIED
    compliance_review_decision_at TIMESTAMP,
    compliance_reviewer_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    INDEX idx_correction_materiality (is_material),
    INDEX idx_correction_report (report_id)
);

CREATE TABLE t_regulatory_correction_notice (
    id BIGSERIAL PRIMARY KEY,
    correction_id BIGINT NOT NULL,

    notice_type VARCHAR(50),  -- CORRECTION_NOTICE, AMENDED_REPORT
    original_report_id BIGINT,
    amendment_report_id BIGINT,

    notice_generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notice_submitted_at TIMESTAMP,

    regulatory_body VARCHAR(100),  -- UKGC, MGA, etc.
    submission_status VARCHAR(20),  -- PENDING_REVIEW, SUBMITTED, ACKNOWLEDGED

    CONSTRAINT fk_notice_correction FOREIGN KEY (correction_id)
        REFERENCES t_correction_audit_log(id)
);
```

---

## GAP-9: 假日出金 SLA

### 概述

維護按國家銀行假日日曆。自動調整出金 SLA（銀行轉賬排除非營業日）。提供電子錢包/加密貨幣替代選項（無假日影響）。營運儀表板監控。

### API 變更

**出金 SLA 計算**: `POST /api/v1/withdrawal/calculate-sla`

```json
{
  "method": "POST",
  "path": "/api/v1/withdrawal/calculate-sla",
  "body": {
    "withdrawal_method": "BANK_TRANSFER",
    "bank_country": "GB",
    "requested_date": "2026-12-23",
    "standard_sla_days": 1
  },
  "response": {
    "requested_date": "2026-12-23",
    "bank_country": "GB",
    "holidays_in_sla_window": [
      {
        "date": "2026-12-25",
        "name": "Christmas Day",
        "bank_closed": true
      },
      {
        "date": "2026-12-26",
        "name": "Boxing Day",
        "bank_closed": true
      }
    ],
    "adjusted_sla_days": 5,
    "estimated_arrival_date": "2026-12-28",
    "alternative_methods": [
      {
        "method": "E_WALLET",
        "estimated_arrival": "2026-12-24",
        "holiday_adjusted": false
      },
      {
        "method": "CRYPTO",
        "estimated_arrival": "2026-12-24",
        "holiday_adjusted": false
      }
    ]
  }
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_bank_holiday_calendar (
    id BIGSERIAL PRIMARY KEY,
    country_code VARCHAR(10) NOT NULL,  -- GB, MT, PH, etc.
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(100),
    holiday_type VARCHAR(50),  -- PUBLIC_HOLIDAY, BANK_CLOSURE, SPECIAL_CLOSURE

    year INT NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,  -- TRUE for annual holidays like Christmas

    bank_closed BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (country_code, holiday_date),
    INDEX idx_calendar_country_year (country_code, year)
);

CREATE TABLE t_withdrawal_sla_adjustment (
    id BIGSERIAL PRIMARY KEY,
    withdrawal_id BIGINT NOT NULL,
    withdrawal_method VARCHAR(50),
    bank_country VARCHAR(10),

    standard_sla_days INT,
    holidays_in_window INT,
    adjusted_sla_days INT,

    standard_sla_date TIMESTAMP,
    adjusted_sla_date TIMESTAMP,

    alternative_methods_offered JSON,  -- Array of alternative methods with adjusted SLAs
    alternative_method_selected VARCHAR(50),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_sla_withdrawal FOREIGN KEY (withdrawal_id)
        REFERENCES t_withdrawal(id),
    INDEX idx_sla_adjustment_method (withdrawal_method)
);
```

### 錯誤處理

```javascript
// Calculate withdrawal SLA with holiday adjustment

async function calculateWithdrawalSLA(withdrawalId, method, bankCountry) {
  const standardSLADays = getStandardSLA(method);  // 1-2 days for bank transfer

  const requestDate = new Date();
  const slaWindow = addDays(requestDate, standardSLADays);

  // Get holidays in SLA window
  const holidays = await getHolidaysInRange(bankCountry, requestDate, slaWindow);
  const holidaysCount = holidays.filter(h => h.bank_closed).length;

  let adjustedSLADate = slaWindow;

  // Skip non-banking days
  for (let i = 0; i < holidaysCount; i++) {
    adjustedSLADate = addDays(adjustedSLADate, 1);
    // Check if new date is also a holiday
    while (isHoliday(bankCountry, adjustedSLADate)) {
      adjustedSLADate = addDays(adjustedSLADate, 1);
    }
  }

  // Get alternative methods (no holiday adjustment)
  const alternatives = [
    {
      method: 'E_WALLET',
      sla_hours: 4,
      sla_date: addHours(requestDate, 4)
    },
    {
      method: 'CRYPTO',
      sla_hours: 2,
      sla_date: addHours(requestDate, 2)
    }
  ];

  return {
    method,
    standard_sla_date: slaWindow,
    adjusted_sla_date: adjustedSLADate,
    holidays_count: holidaysCount,
    alternatives
  };
}
```

---

## GAP-10: GP 破產玩家保護

### 概述

規格骨架 — 待法務確認。未結算賭注自動回滾。已結算勝利金平台吸收。託管金考慮。2h 玩家通知。24h 監管報告。遊戲禁用但保留歷史。

### API 變更（框架）

**GP 破產觸發**: `POST /api/v1/game-provider/handle-bankruptcy`

```json
{
  "game_provider_id": "GP-001",
  "bankruptcy_announcement_date": "2026-03-25",
  "response": {
    "action_initiated": true,
    "auto_rollback_count": 5234,  // number of open rounds
    "total_refund_amount": 2350000.00,
    "affected_players": 8450,
    "games_disabled": true,
    "player_notifications_queued": 8450,
    "regulatory_report_scheduled": true,
    "regulatory_report_due": "2026-03-26T14:30:00Z"
  }
}
```

### 資料庫 Schema（框架）

```sql
CREATE TABLE t_game_provider_bankruptcy (
    id BIGSERIAL PRIMARY KEY,
    game_provider_id BIGINT NOT NULL,

    bankruptcy_announced_date TIMESTAMP NOT NULL DEFAULT NOW(),
    bankruptcy_effective_date DATE,

    status VARCHAR(30),  -- ANNOUNCED, IN_LIQUIDATION, RESOLVED

    -- Auto rollback
    open_rounds_at_announcement INT,
    rollback_initiated_at TIMESTAMP,
    rollback_completed_at TIMESTAMP,
    total_rollback_amount DECIMAL(19,4),

    -- Player impact
    affected_players INT,

    -- Game status
    games_disabled_at TIMESTAMP,
    games_disabled_reason VARCHAR(100),

    -- Platform liability
    platform_absorbed_loss DECIMAL(19,4),
    loss_reason VARCHAR(100),

    -- Regulatory reporting
    regulatory_report_required BOOLEAN DEFAULT TRUE,
    regulatory_body_list TEXT,  -- JSON array
    report_generated_at TIMESTAMP,
    report_submitted_at TIMESTAMP,

    CONSTRAINT fk_bankruptcy_gp FOREIGN KEY (game_provider_id)
        REFERENCES t_game_provider(id),
    INDEX idx_bankruptcy_status (status)
);

CREATE TABLE t_game_rollback (
    id BIGSERIAL PRIMARY KEY,
    game_provider_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,

    game_round_id BIGINT NOT NULL,
    bet_amount DECIMAL(19,4),

    rollback_initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    rollback_completed_at TIMESTAMP,
    refund_transaction_id BIGINT,

    CONSTRAINT fk_rollback_gp FOREIGN KEY (game_provider_id)
        REFERENCES t_game_provider(id),
    CONSTRAINT fk_rollback_player FOREIGN KEY (player_id)
        REFERENCES t_player(id),
    INDEX idx_rollback_provider (game_provider_id),
    INDEX idx_rollback_player (player_id)
);
```

---

## N-02: ValidBet 術語統一

### 概述

確認 `validBet` 為單一事實來源 (SSOT) 術語。更新所有文檔和代碼註釋（廢棄 `ValidTurnover`、`ValidSpins` 等）。

### 變更清單

**文檔更新**:
- [ ] Appendix A：術語表，`validBet` 標記為 SSOT
- [ ] 代碼庫：替換舊術語 (ValidTurnover、ValidSpins → validBet)
- [ ] API 文檔：欄位名稱確認 `valid_bet`

**Schema 更新**:
```sql
-- Rename columns if exists
ALTER TABLE t_wagering_requirement RENAME COLUMN valid_turnover TO valid_bet;
ALTER TABLE t_bonus_tracking RENAME COLUMN valid_spins TO valid_bet_count;
```

---

## F-04: FX 風險分攤

### 概述

法幣 FX 波動 ≤0.5% 平台吸收，>0.5% 各分 50%。加密 ≤2% 平台吸收，>2% 玩家承擔。代理結算使用結算日匯率。FX P&L 報告。

### API 變更

**FX 轉換計算**: `POST /api/v1/wallet/calculate-fx-conversion`

```json
{
  "method": "POST",
  "path": "/api/v1/wallet/calculate-fx-conversion",
  "body": {
    "from_currency": "USD",
    "to_currency": "EUR",
    "amount": 1000.00,
    "conversion_type": "PLAYER_DEPOSIT"  // PLAYER_DEPOSIT, PLAYER_WITHDRAWAL, AGENT_SETTLEMENT
  },
  "response": {
    "conversion_id": "FX-2026-03-25-001",
    "from_currency": "USD",
    "to_currency": "EUR",
    "input_amount": 1000.00,
    "exchange_rate": 0.920,
    "rate_timestamp": "2026-03-25T14:30:00Z",
    "rate_lock_window_seconds": 900,  // 15 minutes
    "fx_volatility_percentage": 0.65,  // > 0.5% threshold
    "platform_absorption_percentage": 0.50,
    "player_absorption_percentage": 0.15,
    "conversion_amount": 917.50,  // After adjustments
    "breakdown": {
      "base_conversion": 920.00,
      "fx_loss": 8.00,  // 0.65% volatility
      "platform_loss": -4.00,  // Platform absorbs 0.5%
      "player_loss": -1.20,  // Player absorbs 0.15% (half of remaining)
      "final_amount": 917.50
    },
    "lock_expires_at": "2026-03-25T14:45:00Z"
  }
}
```

**FX P&L 報告**: `GET /api/v1/finance/fx-pl-report?period=2026-03`

```json
{
  "period": "2026-03",
  "currency_pairs": [
    {
      "pair": "USD/EUR",
      "total_conversion_amount": 150000.00,
      "average_volatility": 0.42,
      "platform_loss_absorbed": -450.00,
      "player_loss_absorbed": -180.00,
      "conversions_count": 342
    }
  ],
  "total_platform_fx_loss": -2340.00,
  "total_player_fx_adjustment": -920.00
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_fx_conversion (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    player_id BIGINT,
    agent_id BIGINT,

    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,

    input_amount DECIMAL(19,4) NOT NULL,
    exchange_rate DECIMAL(19,6) NOT NULL,
    rate_timestamp TIMESTAMP NOT NULL,

    -- FX volatility calculation
    previous_exchange_rate DECIMAL(19,6),
    volatility_percentage DECIMAL(7,4),
    volatility_direction VARCHAR(10),  -- UP, DOWN

    -- Risk absorption
    is_fiat_conversion BOOLEAN,
    is_crypto_conversion BOOLEAN,
    volatility_threshold_exceeded BOOLEAN,

    platform_absorption_percentage DECIMAL(5,2),
    platform_loss_amount DECIMAL(19,4),
    player_absorption_percentage DECIMAL(5,2),
    player_loss_amount DECIMAL(19,4),

    -- Final amount
    conversion_amount DECIMAL(19,4),

    -- Lock window
    rate_lock_expires_at TIMESTAMP,
    lock_window_seconds INT,

    conversion_type VARCHAR(50),  -- PLAYER_DEPOSIT, PLAYER_WITHDRAWAL, AGENT_SETTLEMENT

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_fx_player FOREIGN KEY (player_id)
        REFERENCES t_player(id),
    INDEX idx_fx_pair (from_currency, to_currency),
    INDEX idx_fx_timestamp (rate_timestamp)
);

CREATE TABLE t_fx_pl_daily (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    report_date DATE NOT NULL,

    currency_pair VARCHAR(20) NOT NULL,  -- USD/EUR
    conversion_count INT,
    total_conversion_volume DECIMAL(19,4),

    average_volatility DECIMAL(7,4),
    platform_loss_total DECIMAL(19,4),
    player_loss_total DECIMAL(19,4),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (tenant_id, report_date, currency_pair),
    INDEX idx_pl_date_pair (report_date, currency_pair)
);
```

### 錯誤處理

```javascript
// FX conversion with risk absorption

async function calculateFXConversion(fromCurrency, toCurrency, amount, conversionType) {
  const currentRate = await getExchangeRate(fromCurrency, toCurrency);
  const previousRate = await getPreviousExchangeRate(fromCurrency, toCurrency);

  const volatilityPercentage = calculateVolatility(previousRate, currentRate);

  let platformAbsorption = 0;
  let playerAbsorption = 0;
  const isFiat = !isCryptoCurrency(fromCurrency) && !isCryptoCurrency(toCurrency);

  if (isFiat) {
    if (volatilityPercentage <= 0.5) {
      platformAbsorption = volatilityPercentage;
    } else {
      platformAbsorption = 0.5;  // Platform absorbs 0.5%
      playerAbsorption = (volatilityPercentage - 0.5) / 2;  // Split remaining
    }
  } else {
    // Crypto conversion
    if (volatilityPercentage <= 2) {
      platformAbsorption = volatilityPercentage;
    } else {
      platformAbsorption = 2;  // Platform absorbs 2%
      playerAbsorption = volatilityPercentage - 2;  // Player absorbs rest
    }
  }

  const baseConversion = amount * currentRate;
  const totalLoss = baseConversion * (volatilityPercentage / 100);
  const platformLoss = baseConversion * (platformAbsorption / 100);
  const playerLoss = baseConversion * (playerAbsorption / 100);

  const finalAmount = baseConversion - playerLoss;

  return {
    exchangeRate: currentRate,
    volatilityPercentage,
    platformAbsorption,
    playerAbsorption,
    platformLoss,
    playerLoss,
    finalAmount,
    lockWindowSeconds: 900,
    lockExpiresAt: addSeconds(new Date(), 900)
  };
}
```

---

## F-05: 事件回應框架

### 概述

事件分級 (P0-P4)。戰情室激活標準。升級矩陣。SLA 定義 (P0: 15 min、P1: 1h、P2: 4h)。事件後分析 (RCA)。業務連續性計畫演練。

### API 變更

**事件建立**: `POST /api/v1/incident/create`

```json
{
  "method": "POST",
  "path": "/api/v1/incident/create",
  "body": {
    "title": "Database primary node down",
    "description": "Primary DB node unresponsive, failover initiated",
    "severity": "P0",
    "affected_system": "WALLET_SYSTEM",
    "detection_source": "MONITORING_ALERT"
  },
  "response": {
    "incident_id": "INC-2026-03-25-001",
    "severity": "P0",
    "war_room_activated": true,
    "war_room_id": "WR-2026-03-25-001",
    "initial_response_sla": "15 minutes",
    "status_page_auto_update": true,
    "escalation_list": [
      {
        "escalation_level": 1,
        "role": "ON_CALL_ENGINEER",
        "assigned_to": "eng@platform.com"
      },
      {
        "escalation_level": 2,
        "role": "ENGINEERING_MANAGER",
        "escalate_at": "2026-03-25T14:45:00Z"
      }
    ]
  }
}
```

### 資料庫 Schema 變更

```sql
CREATE TABLE t_incident (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,

    incident_id VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,

    severity VARCHAR(10),  -- P0, P1, P2, P3, P4
    status VARCHAR(30),  -- DETECTED, IN_PROGRESS, RESOLVED, POST_ANALYSIS

    affected_systems TEXT,  -- JSON array of system codes
    detection_source VARCHAR(100),  -- MONITORING_ALERT, PLAYER_REPORT, INTERNAL_DISCOVERY
    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- War room
    war_room_activated BOOLEAN DEFAULT FALSE,
    war_room_id VARCHAR(50),
    war_room_created_at TIMESTAMP,

    -- Resolution
    root_cause_identified BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolution_description TEXT,

    -- SLA tracking
    initial_response_target TIMESTAMP,
    initial_response_met BOOLEAN,
    actual_response_time_minutes INT,

    resolution_target TIMESTAMP,
    resolution_met BOOLEAN,
    actual_resolution_time_minutes INT,

    -- Post-analysis
    rca_status VARCHAR(20),  -- null, PENDING, IN_PROGRESS, COMPLETED
    rca_document_id BIGINT,
    rca_completed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    INDEX idx_incident_severity_status (severity, status),
    INDEX idx_incident_timeline (detected_at)
);

CREATE TABLE t_incident_escalation (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL,

    escalation_level INT NOT NULL,
    escalate_to_role VARCHAR(100),  -- ON_CALL_ENGINEER, ENGINEERING_MANAGER, VP_ENGINEERING, CEO
    escalate_to_user_id BIGINT,

    scheduled_escalation_time TIMESTAMP,
    actual_escalation_time TIMESTAMP,
    escalation_reason TEXT,

    notification_sent BOOLEAN DEFAULT FALSE,
    notification_sent_at TIMESTAMP,
    acknowledgement_received BOOLEAN DEFAULT FALSE,
    acknowledgement_time TIMESTAMP,

    CONSTRAINT fk_escalation_incident FOREIGN KEY (incident_id)
        REFERENCES t_incident(id),
    INDEX idx_escalation_timeline (scheduled_escalation_time)
);
```

---

## 交叉切割: 配置參數集成

所有配置參數透過 `t_config_param_definition` 表管理，支援按租戶、品牌、司法管轄區覆蓋。

### 配置對應表

| GAP Item | Config Key | Type | Default | Tenant Override |
|----------|-----------|------|---------|-----------------|
| GAP-1 | `ch2.withdrawal.wager_verification_sla_ms` | INT | 200 | Yes |
| GAP-1 | `ch5.wagering.wager_timeout_threshold_ms` | INT | 500 | Yes |
| GAP-2 | `ch8.agent_security.session_idle_timeout_min` | INT | 30 | No |
| GAP-2 | `ch8.agent_security.mfa_failed_attempts_limit` | INT | 5 | Yes |
| GAP-2 | `ch8.agent_security.mfa_lockout_duration_min` | INT | 15 | Yes |
| GAP-3 | `ch8.settlement.tier1_max_amount` | DECIMAL | 50000.00 | Yes |
| GAP-3 | `ch8.settlement.tier2_max_amount` | DECIMAL | 100000.00 | Yes |
| GAP-3 | `ch8.settlement.monthly_compliance_threshold` | DECIMAL | 500000.00 | Yes |
| GAP-4 | `ch5.referral.monthly_limit` | INT | 10 | Yes |
| GAP-4 | `ch5.referral.clawback_days` | INT | 90 | No |
| GAP-4 | `ch5.referral.device_fingerprint_match_threshold` | DECIMAL | 0.95 | Yes |
| GAP-6 | `ch1.session.affordability_assessment_sla_days` | INT | 7 | Yes |
| GAP-6 | `ch1.session.risk_review_sla_hours` | INT | 4 | Yes |
| GAP-7 | `ch3.psp.gamstop_similarity_threshold` | DECIMAL | 0.70 | No |
| GAP-7 | `ch3.psp.gamstop_review_sla_hours` | INT | 24 | No |
| GAP-8 | `ch9.report.materiality_threshold_percentage` | DECIMAL | 0.1 | No |
| GAP-8 | `ch9.report.correction_regulatory_report_sla_days` | INT | 5 | No |
| GAP-9 | `ch4.jackpot.withdrawal_holiday_calendar_auto_update_enabled` | BOOLEAN | TRUE | No |
| GAP-10 | `ch4.gp_bankruptcy.player_notification_sla_hours` | INT | 2 | No |
| GAP-10 | `ch4.gp_bankruptcy.regulatory_report_sla_hours` | INT | 24 | No |
| F-04 | `ch2.fx.fiat_absorption_pct` | DECIMAL | 0.5 | Yes |
| F-04 | `ch2.fx.crypto_absorption_pct` | DECIMAL | 2.0 | Yes |
| F-04 | `ch2.fx.exchange_rate_lock_window_seconds` | INT | 900 | No |

### 配置 API

**獲取配置**: `GET /api/v1/config/param/{key}?tenant_id=1&brand_id=1&jurisdiction=MT`

```json
{
  "key": "ch8.settlement.tier1_max_amount",
  "value": 50000.00,
  "type": "DECIMAL",
  "scope": "BRAND",  // GLOBAL, TENANT, BRAND, JURISDICTION
  "effective_date": "2026-03-01",
  "last_updated": "2026-03-01T00:00:00Z",
  "updated_by": "admin@platform.com"
}
```

**更新配置**: `POST /api/v1/config/param/{key}`

```json
{
  "value": 60000.00,
  "scope": "BRAND",
  "brand_id": 1,
  "effective_date": "2026-04-01",
  "change_reason": "Risk appetite adjustment after Q1 review"
}
```

### Schema

```sql
CREATE TABLE t_config_param_definition (
    id BIGSERIAL PRIMARY KEY,
    param_key VARCHAR(100) NOT NULL,
    param_type VARCHAR(20),  -- INT, DECIMAL, STRING, BOOLEAN, JSON

    -- Scope
    scope_level VARCHAR(50),  -- GLOBAL, TENANT, BRAND, JURISDICTION
    tenant_id BIGINT,
    brand_id BIGINT,
    jurisdiction_code VARCHAR(10),

    -- Value
    default_value VARCHAR(500),
    current_value VARCHAR(500) NOT NULL,

    -- Versioning
    version INT DEFAULT 1,
    effective_date DATE NOT NULL,
    expiry_date DATE,

    -- Audit
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    change_reason TEXT,

    -- Governance
    requires_approval BOOLEAN DEFAULT FALSE,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,

    UNIQUE (param_key, scope_level, tenant_id, brand_id, jurisdiction_code, effective_date),
    INDEX idx_config_scope (scope_level, effective_date)
);
```

---

## 總結

本技術實作指南涵蓋 14 項 Sprint v2.2 缺漏補充的完整技術規格，包括 API 變更、資料庫 Schema、狀態機、錯誤處理和測試需求。所有配置參數已整合至中央配置管理系統，支援按層級 (租戶/品牌/司法管轄區) 覆蓋。

**後續步驟**:
1. 技術審核與角色確認
2. 規格細化（尤其是待法務/合規確認的項目）
3. 實現計畫與里程碑排定
4. 測試策略與自動化測試框架建立
