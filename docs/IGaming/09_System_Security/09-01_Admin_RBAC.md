# 09-01 管理員權限系統 (Admin RBAC)

## 1. 系統概述
基於 **RBAC (Role-Based Access Control)** 模型設計，嚴格控制後台人員的數據訪問權限。
需區分 **平台管理員 (Platform Admin)** 與 **商戶管理員 (Tenant Admin)**。

## 2. 角色與權限

### 2.1 預設角色 (Default Roles)
- **Super Admin**：
  - 平台神級帳號，擁有所有權限。
  - 僅限 CTO 或核心運維持有。
  - **初始化**: 系統首次啟動時由 `Seed Job` 自動創建 (參見 `12-01_Deployment_Architecture` Section 5.1)，密碼由 K8s Secret 注入。
- **Tenant Owner**：
  - 商戶最高權限，可創建該商戶下的子帳號。
- **客服 (CS)**：需屏蔽玩家手機號/Email (脫敏顯示，如 0912***789)。
- **財務 (Finance)**：可操作出款審核，可查看報表。
- **運營 (Operation)**：可配置活動、Banner。
- **自定义角色 (Custom Roles)**：
  - 商戶預設只能使用上述標準模板。
  - 若需新增特殊角色 (如 "高級財務審核")，需由商戶 Admin 定義權限集合，並提交 **Platform Admin** 審核通過後方可使用，防止權限濫用。

### 2.2 權限粒度 (Granularity)
- **菜單級**：是否可見 "財務報表" 菜單。
- **按鈕級**：是否可見 "通過審核" 按鈕。
- **數據級**：
  - 只能看 "自己創建的代理"。
  - 只能看 "自己創建的代理"。
  - 只能看 "VIP 等級 < 5" 的玩家。

### 2.3 敏感操作授權 (Action-Based Permissions)
針對高風險指令，需定義獨立的授權規則，不應默認包含在角色中。
- **補單 (Manual Credit)**:
  - `< $100`: 客服組長 (CS Lead) 可執行。
  - `> $100`: 需財務經理 (Finance Manager) 審批。
- **踢線 (Kickout) / 凍結 (Freeze)**:
  - 需風控專員 (Risk Specialist) 執行，普通客服僅能 "申請"。
- **解鎖 (Unlock)**:
  - 需二級驗證 (如：電話確認本人) 後，由客服組長執行。


## 3. 安全機制
- **MFA 強制**：後台登入必須綁定 Google Authenticator。
- **IP 限制**：僅允許特定辦公室 IP 訪問後台。
- **Session 管理**：閒置 15 分鐘自動登出；單一帳號禁止多地登入 (互踢)。

## 4. 審批
- **權限變更**：
  - 為某個員工帳號 "新增權限" (如由客服升級為財務)，需主管審批。

---

## 5. 動態權限評估引擎 (Dynamic Permission Evaluation Engine)

### 5.1 架構總覽

```
[RBAC Permission Evaluation Architecture]
┌──────────────────────────────────────────────────────────────┐
│                Permission Evaluation Engine                  │
├──────────────────────────────────────────────────────────────┤
│  Input: (User, Action, Resource, Context)                   │
│                                                              │
│  Layer 1: Role Resolver                                     │
│  ├─ Flatten role hierarchy (DFS traversal)                  │
│  ├─ Detect circular inheritance                             │
│  └─ Output: Flat list of roles [R1, R2, R3...]             │
│                                                              │
│  Layer 2: Permission Aggregator                             │
│  ├─ Merge inherited permissions from all roles              │
│  ├─ Apply explicit deny rules (blacklist)                   │
│  └─ Output: {allowed: [P1, P2], denied: [P3]}              │
│                                                              │
│  Layer 3: Conflict Resolution Engine                        │
│  ├─ Rule 1: Explicit deny > Explicit allow                  │
│  ├─ Rule 2: Parent deny > Child allow                       │
│  ├─ Rule 3: Specific resource > Wildcard                    │
│  └─ Output: Final decision (ALLOW/DENY)                     │
│                                                              │
│  Layer 4: Context Evaluator (ABAC)                          │
│  ├─ Time-based access (working hours only)                  │
│  ├─ IP whitelist validation                                 │
│  ├─ Resource ownership check                                │
│  ├─ Data-level filtering (VIP < 5 only)                     │
│  └─ Output: Context-aware decision                          │
│                                                              │
│  Final Output: ALLOW + Filtered Dataset                     │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 角色繼承圖 (Role Inheritance Graph)

#### 5.2.1 繼承關係定義

```
[Role Hierarchy]
┌────────────────────────────────────────────────────────────┐
│  Super Admin (Level 0)                                     │
│    - Inherits: ALL permissions                             │
│    - Cannot be restricted                                  │
│    ↓                                                        │
│  Platform Admin (Level 1)                                  │
│    - Inherits: Super Admin (except user management)       │
│    - Additional: Cross-tenant reporting                    │
│    ↓                                                        │
│  Tenant Owner (Level 2)                                    │
│    - Inherits: None (isolated)                             │
│    - Permissions: All actions within own tenant            │
│    ├───→ Finance Manager (Level 3)                         │
│    │      - Inherits: Finance permissions                  │
│    │      - Additional: Approve withdrawals > $10K         │
│    │      ├───→ Finance (Level 4)                          │
│    │      │      - Inherits: Basic finance view            │
│    │      │      - Additional: Approve withdrawals < $1K   │
│    │                                                        │
│    ├───→ CS Manager (Level 3)                              │
│    │      - Inherits: CS permissions                       │
│    │      - Additional: Manual credit < $100               │
│    │      ├───→ CS Agent (Level 4)                         │
│    │      │      - Inherits: Player view (masked PII)      │
│    │      │      - Additional: Create tickets              │
│    │                                                        │
│    ├───→ Risk Manager (Level 3)                            │
│    │      - Inherits: Risk permissions                     │
│    │      - Additional: Freeze accounts, Blacklist         │
│    │      ├───→ Risk Analyst (Level 4)                     │
│    │      │      - Inherits: Risk dashboard view           │
│    │      │      - Additional: Flag suspicious accounts    │
│    │                                                        │
│    └───→ Marketing Manager (Level 3)                       │
│           - Inherits: Marketing permissions                │
│           - Additional: Create promotions > $5K budget     │
│           ├───→ Marketing Specialist (Level 4)             │
│           │      - Inherits: Banner management             │
│           │      - Additional: Create promotions < $1K     │
└────────────────────────────────────────────────────────────┘
```

#### 5.2.2 繼承規則 (Inheritance Rules)

| 規則 | 說明 | 範例 |
|---|---|---|
| **傳遞繼承** | 子角色繼承父角色的所有權限 | CS Agent 繼承 CS Manager 的 View Player 權限 |
| **累加繼承** | 可擁有多個父角色 (多重繼承) | "Compliance Officer" 可同時繼承 Finance + Risk |
| **覆寫繼承** | 子角色可覆寫父角色的特定權限 | Finance 可 "View Reports"，但子角色可設為 Deny |
| **循環檢測** | 禁止 A → B → C → A 的循環繼承 | 啟動時檢測，發現則拒絕加載角色 |

#### 5.2.3 循環檢測演算法

```python
def detect_circular_inheritance(roles):
    """
    Detect circular role inheritance using DFS (Depth-First Search)
    """
    visited = set()
    recursion_stack = set()

    def dfs(role_id):
        if role_id in recursion_stack:
            raise CircularInheritanceError(f"Circular dependency detected: {role_id}")
        if role_id in visited:
            return

        visited.add(role_id)
        recursion_stack.add(role_id)

        role = roles.get(role_id)
        for parent_id in role.inherits_from:
            dfs(parent_id)

        recursion_stack.remove(role_id)

    for role_id in roles.keys():
        dfs(role_id)

    return True  # No cycles detected
```

### 5.3 權限衝突矩陣 (Permission Conflict Resolution Matrix)

#### 5.3.1 衝突場景與解析策略

| 場景 | 角色 A 權限 | 角色 B 權限 | 解析結果 | 理由 |
|---|---|---|---|---|
| **明確拒絕優先** | Allow: View Players | Deny: View Players | **DENY** | 安全優先原則 |
| **父角色拒絕** | (Parent) Deny: Edit VIP | (Child) Allow: Edit VIP | **DENY** | 父角色決策優先 |
| **資源特定性** | Allow: `/players/*` | Deny: `/players/vip/*` | /players/123: **ALLOW**<br>/players/vip/456: **DENY** | 更具體的規則優先 |
| **動作特定性** | Allow: `players:*` | Deny: `players:delete` | Read: **ALLOW**<br>Delete: **DENY** | 細粒度規則優先 |
| **多角色累加** | Allow: View Reports | Allow: Export Reports | **ALLOW (Both)** | 無衝突時累加 |

#### 5.3.2 解析引擎實作

```javascript
function resolvePermissionConflict(permissions) {
  // Step 1: Collect all DENY rules
  const denyRules = permissions.filter(p => p.effect === 'DENY');
  const allowRules = permissions.filter(p => p.effect === 'ALLOW');

  // Step 2: Check for explicit DENY (highest priority)
  for (const deny of denyRules) {
    if (matchesResource(deny.resource, targetResource) &&
        matchesAction(deny.action, targetAction)) {
      return { decision: 'DENY', reason: 'Explicit deny rule matched' };
    }
  }

  // Step 3: Check for ALLOW rules
  for (const allow of allowRules) {
    if (matchesResource(allow.resource, targetResource) &&
        matchesAction(allow.action, targetAction)) {
      return { decision: 'ALLOW', reason: 'Allow rule matched' };
    }
  }

  // Step 4: Default DENY (fail-secure)
  return { decision: 'DENY', reason: 'No matching allow rule (default deny)' };
}

function matchesResource(pattern, target) {
  // Convert wildcard to regex: "/players/*" → /^\/players\/.+$/
  const regex = new RegExp('^' + pattern.replace(/\*/g, '.+') + '$');
  return regex.test(target);
}
```

### 5.4 ABAC 上下文屬性 (Attribute-Based Access Control Context)

#### 5.4.1 上下文屬性清單

| 類別 | 屬性 | 資料型態 | 範例 | 用途 |
|---|---|---|---|---|
| **時間** | `current_time` | DateTime | 2026-01-27 14:30 | 限制工作時段存取 |
| **時間** | `day_of_week` | String | "Monday" | 禁止週末修改配置 |
| **網路** | `source_ip` | IP Address | 192.168.1.100 | IP 白名單驗證 |
| **網路** | `geolocation` | Country Code | "TW" | 阻擋特定國家存取 |
| **身份** | `user_department` | String | "Finance" | 部門隔離 |
| **身份** | `employment_status` | Enum | ACTIVE | 離職員工自動撤銷 |
| **資源** | `resource_owner` | User ID | 12345 | 只能編輯自己創建的資源 |
| **資源** | `resource_sensitivity` | Enum | HIGH | 高敏感資料需額外驗證 |
| **請求** | `request_source` | Enum | WEB, API, CLI | CLI 操作需雙因素驗證 |
| **請求** | `mfa_verified` | Boolean | true | MFA 驗證狀態 |

#### 5.4.2 ABAC Policy 範例

```json
{
  "policy_id": "finance_working_hours_only",
  "description": "Finance team can only approve withdrawals during working hours",
  "effect": "ALLOW",
  "principals": ["role:Finance", "role:Finance Manager"],
  "actions": ["withdrawals:approve"],
  "resources": ["/api/v1/withdrawals/*"],
  "conditions": {
    "time_range": {
      "start": "09:00",
      "end": "18:00",
      "timezone": "Asia/Taipei"
    },
    "day_of_week": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"],
    "ip_whitelist": ["192.168.1.0/24", "10.0.0.0/16"],
    "mfa_required": true
  }
}
```

#### 5.4.3 動態上下文評估引擎

```python
def evaluate_abac_conditions(policy, context):
    """
    Evaluate ABAC policy conditions against request context
    """
    conditions = policy.get('conditions', {})

    # Time-based check
    if 'time_range' in conditions:
        current_time = context['current_time']
        start = datetime.strptime(conditions['time_range']['start'], '%H:%M')
        end = datetime.strptime(conditions['time_range']['end'], '%H:%M')
        if not (start.time() <= current_time.time() <= end.time()):
            return {'allowed': False, 'reason': 'Outside working hours'}

    # Day of week check
    if 'day_of_week' in conditions:
        if context['day_of_week'] not in conditions['day_of_week']:
            return {'allowed': False, 'reason': 'Weekend/Holiday restriction'}

    # IP whitelist check
    if 'ip_whitelist' in conditions:
        if not ip_in_whitelist(context['source_ip'], conditions['ip_whitelist']):
            return {'allowed': False, 'reason': 'IP not in whitelist'}

    # MFA requirement check
    if conditions.get('mfa_required') and not context.get('mfa_verified'):
        return {'allowed': False, 'reason': 'MFA verification required'}

    # Resource ownership check
    if 'require_ownership' in conditions and conditions['require_ownership']:
        if context['user_id'] != context['resource_owner']:
            return {'allowed': False, 'reason': 'Not resource owner'}

    return {'allowed': True, 'reason': 'All conditions satisfied'}
```

### 5.5 Session 管理策略 (Session Management)

#### 5.5.1 Session 配置參數

| 參數 | 預設值 | 範圍 | 說明 |
|---|---|---|---|
| `idle_timeout` | 15 分鐘 | 5-60 分鐘 | 閒置超時自動登出 |
| `absolute_timeout` | 8 小時 | 1-24 小時 | 絕對超時 (防止長期 session) |
| `max_concurrent_sessions` | 1 | 1-3 | 單一帳號最大並發 session 數 |
| `session_binding` | IP + User-Agent | - | Session 綁定策略 |
| `remember_me_duration` | 禁用 | - | 後台不支援 "記住我" |

#### 5.5.2 並發 Session 處理策略

| 策略 | 行為 | 適用角色 |
|---|---|---|
| **互踢模式** (Kick Previous) | 新登入踢掉舊 session | 所有角色 (預設) |
| **阻擋模式** (Block New) | 拒絕新登入請求 | Super Admin (防止帳號被盜) |
| **多點模式** (Allow Multiple) | 允許最多 3 個 session | 客服 (需多螢幕工作) |

#### 5.5.3 Session 綁定與防劫持

```python
class SecureSession:
    def __init__(self, user_id):
        self.user_id = user_id
        self.session_id = secrets.token_urlsafe(32)
        self.created_at = datetime.now()
        self.last_activity = datetime.now()
        self.ip_address = request.remote_addr
        self.user_agent = request.headers.get('User-Agent')
        self.fingerprint = self._generate_fingerprint()

    def _generate_fingerprint(self):
        """
        Generate session fingerprint to prevent session hijacking
        """
        components = [
            self.ip_address,
            self.user_agent,
            request.headers.get('Accept-Language', ''),
            request.headers.get('Accept-Encoding', '')
        ]
        return hashlib.sha256('|'.join(components).encode()).hexdigest()

    def validate(self):
        """
        Validate session on each request
        """
        # Check idle timeout
        if (datetime.now() - self.last_activity).seconds > IDLE_TIMEOUT:
            raise SessionExpiredError("Session expired due to inactivity")

        # Check absolute timeout
        if (datetime.now() - self.created_at).seconds > ABSOLUTE_TIMEOUT:
            raise SessionExpiredError("Session exceeded maximum duration")

        # Check fingerprint (detect session hijacking)
        current_fingerprint = self._generate_fingerprint()
        if current_fingerprint != self.fingerprint:
            logger.alert(f"Session hijacking detected for user {self.user_id}")
            raise SecurityError("Session validation failed")

        # Update last activity
        self.last_activity = datetime.now()
        return True
```

#### 5.5.4 Session 存儲架構

```
[Session Storage Architecture]
┌──────────────────────────────────────────────────────────────┐
│  Redis (Primary Storage)                                     │
│  Key: session:{session_id}                                   │
│  TTL: 15 minutes (auto-extend on activity)                   │
│  Value: {                                                    │
│    user_id, roles, permissions, ip, user_agent, fingerprint  │
│  }                                                           │
├──────────────────────────────────────────────────────────────┤
│  PostgreSQL (Audit Trail)                                    │
│  Table: session_logs                                         │
│  Columns: session_id, user_id, login_time, logout_time,     │
│           ip, user_agent, logout_reason                      │
│  Retention: 90 days                                          │
└──────────────────────────────────────────────────────────────┘
```

### 5.6 API 規格 (Permission Check API)

#### 5.6.1 權限檢查接口

```
POST /api/v1/rbac/check-permission
Authorization: Bearer {admin_jwt}

Request Body:
{
  "user_id": 12345,
  "action": "withdrawals:approve",
  "resource": "/api/v1/withdrawals/wd-789012",
  "context": {
    "current_time": "2026-01-27T14:30:00Z",
    "source_ip": "192.168.1.100",
    "mfa_verified": true
  }
}

Response 200 OK (ALLOW):
{
  "decision": "ALLOW",
  "reason": "User has required permissions and meets all conditions",
  "evaluated_policies": [
    {
      "policy_id": "finance_approve_withdrawal",
      "effect": "ALLOW",
      "matched": true
    }
  ],
  "execution_time_ms": 15
}

Response 200 OK (DENY):
{
  "decision": "DENY",
  "reason": "Outside working hours (09:00-18:00 required)",
  "evaluated_policies": [
    {
      "policy_id": "finance_working_hours_only",
      "effect": "ALLOW",
      "matched": false,
      "failed_condition": "time_range"
    }
  ],
  "execution_time_ms": 12
}
```

#### 5.6.2 批次權限檢查 (效能優化)

```
POST /api/v1/rbac/batch-check
Authorization: Bearer {admin_jwt}

Request Body:
{
  "user_id": 12345,
  "checks": [
    {"action": "players:view", "resource": "/players/123"},
    {"action": "players:edit", "resource": "/players/123"},
    {"action": "withdrawals:approve", "resource": "/withdrawals/456"}
  ]
}

Response 200 OK:
{
  "results": [
    {"action": "players:view", "decision": "ALLOW"},
    {"action": "players:edit", "decision": "DENY", "reason": "Insufficient role"},
    {"action": "withdrawals:approve", "decision": "ALLOW"}
  ],
  "execution_time_ms": 25  // Batch processing faster than 3 individual calls
}
```

### 5.7 效能考量與優化

#### 5.7.1 快取策略

| 快取項目 | 存儲位置 | TTL | 更新策略 |
|---|---|---|---|
| 用戶角色清單 | Redis | 10 分鐘 | 角色變更時主動失效 |
| 權限評估結果 | Redis | 5 分鐘 | 權限配置變更時主動失效 |
| 角色繼承樹 | Memory (App) | 啟動載入 | 配置變更時重啟服務 |
| ABAC Policy | Memory (App) | 啟動載入 | 配置變更時熱重載 |

#### 5.7.2 性能基準

| 操作 | P99 延遲 | P50 延遲 | QPS 目標 |
|---|---|---|---|
| 簡單權限檢查 (無繼承) | < 5ms | < 2ms | 10,000+ |
| 複雜權限檢查 (3 層繼承) | < 20ms | < 8ms | 5,000+ |
| ABAC 條件評估 | < 30ms | < 12ms | 3,000+ |
| 批次檢查 (10 項) | < 50ms | < 25ms | 1,000+ |

### 5.8 審計日誌 (Permission Audit Trail)

#### 5.8.1 記錄範疇

所有權限檢查結果必須記錄至審計日誌：

```sql
CREATE TABLE permission_audit_logs (
  log_id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  action VARCHAR(100) NOT NULL,
  resource VARCHAR(255) NOT NULL,
  decision VARCHAR(10) NOT NULL,  -- ALLOW | DENY
  reason TEXT,
  context JSONB,                  -- IP, time, MFA status, etc.
  evaluated_policies JSONB,       -- Which policies were evaluated
  execution_time_ms INT,
  created_at TIMESTAMP DEFAULT NOW(),

  INDEX idx_user (user_id),
  INDEX idx_decision (decision),
  INDEX idx_created_at (created_at)
) PARTITION BY RANGE (created_at);  -- 按月分區
```

#### 5.8.2 異常行為檢測

```python
# Alert on suspicious patterns
def detect_permission_abuse(user_id, time_window='1h'):
    logs = db.query("""
        SELECT action, decision, COUNT(*) as attempts
        FROM permission_audit_logs
        WHERE user_id = %s AND created_at > NOW() - INTERVAL %s
        GROUP BY action, decision
    """, (user_id, time_window))

    for log in logs:
        # Alert if >50 DENY in 1 hour (potential privilege escalation attempt)
        if log.decision == 'DENY' and log.attempts > 50:
            alert_security_team(
                f"User {user_id} attempted {log.action} {log.attempts} times (all denied)"
            )

        # Alert if accessing >100 different resources in 1 hour (data scraping)
        if log.attempts > 100:
            alert_security_team(
                f"User {user_id} accessed {log.attempts} resources in 1 hour"
            )
```

---

## 📚 相關文檔

### 核心依賴
- [07-01 系統層級架構](../07_Platform_Management/07-01_Hierarchy_Architecture.md) - 多租戶架構、層級設計
- [09-02 審計日誌系統](./09-02_Audit_Log_System.md) - 操作日誌記錄

### 業務整合
- [01-01 玩家賬戶系統](../01_Player_Center/01-01_Player_Account_System.md) - 玩家權限初始化
- [09-04 審批工作流系統](./09-04_Approval_Workflow_System.md) - 敏感操作審批

### 技術參考
- [09-03 數據安全標準](./09-03_Data_Security_Standard.md) - 權限數據加密
- [12-03 網關架構](../12_Technical_Operations/12-03_Gateway_Architecture.md) - API 權限驗證

### 延伸閱讀
- [07-02 租戶配置管理](../07_Platform_Management/07-02_Tenant_Configuration.md) - 商戶權限配置
- [11-01 客服平台設計](../11_Customer_Service/11-01_CS_Platform_Design.md) - 客服權限管理

---

**文件版本**: V2.0 (Enhanced)
**最後更新**: 2026-01-27
**狀態**: Architecture-Level Complete
**維護者**: Security Team
