# SmartAdmin Auto-Coding - Kubernetes Agents

**版本**: 1.0.0
**日期**: 2026-01-27
**狀態**: 🚧 框架已創建，待實施

---

## 📂 目錄結構

```
automation/clawdbot/
├── tools/                          # 工具層
│   └── file_access_guard.py        # 文件訪問白名單/黑名單控制 (571 行)
├── telegram-webhook/               # Telegram 通知服務
│   ├── app.py                      # Flask 應用 (487 行)
│   ├── requirements.txt            # Python 依賴
│   ├── Dockerfile                  # Docker 鏡像
│   └── test_webhook.py             # 測試腳本 (248 行)
├── crews/                          # CrewAI Crew 實現
│   ├── common/                     # 共享基礎類
│   │   ├── base_crew.py            # 基礎 Crew 類 (420 行)
│   │   └── tools.py                # 共享工具 (465 行)
│   ├── analyzer_crew.py            # Analyzer Workflow (360 行)
│   ├── developer_crew.py           # Developer Workflow (425 行)
│   ├── qa_crew.py                  # QA Workflow (375 行)
│   └── tests/                      # Crew 測試
│       └── test_all_crews.py       # 集成測試腳本 (225 行)
├── workflows/                      # Argo Workflows (P2 - 待實施)
│   └── templates/                  # Workflow 模板
├── deploy.sh                       # 一鍵部署腳本 (441 行)
└── README.md                       # 本文件
```

---

## 🚀 快速開始

### 1. 部署 PostgreSQL 審計表

```bash
cd ../smart-admin-api-java21-springboot3
./gradlew :sa-admin:flywayMigrate
```

或手動執行 SQL（如果 Flyway 未配置）：

```bash
psql -U postgres -d smart_admin -f ../smart-admin-api-java21-springboot3/sa-admin/src/main/resources/db/migration/V999__ai_system_tables.sql
```

### 2. 構建 Telegram Webhook 鏡像

```bash
cd telegram-webhook
docker build -t smartadmin/telegram-webhook:latest .
```

### 3. 配置 Kubernetes Secrets

```bash
# 編輯 Secrets 文件，替換實際密鑰
vim ../k8s/secrets/claude-api-credentials.yaml
vim ../k8s/secrets/telegram-bot-credentials.yaml

# 應用配置
kubectl apply -f ../k8s/secrets/
```

### 4. 部署 RBAC 和服務

```bash
kubectl apply -f ../k8s/rbac/agent-role.yaml
kubectl apply -f ../k8s/telegram-webhook/deployment.yaml
```

### 5. 驗證部署

```bash
kubectl get all -n smartadmin
kubectl logs -l app=smartadmin-auto-coding -n smartadmin
```

---

## 📋 待實施任務

### P0 (Critical - 立即實施)

- [x] **file_access_guard.py** - ✅ 實現完成 (~220 行)
  - ✅ `normalize_path()` - 路徑規範化
  - ✅ `_check_pattern_match()` - 正則表達式模式匹配
  - ✅ `check_access()` - 4 層訪問控制邏輯
  - ✅ `__init__()` - PostgreSQL 連接池初始化
  - ✅ `log_access_decision()` - 審計日誌記錄
  - ✅ 7 個測試用例實現（內建 TestGuard mock）

- [x] **app.py** - ✅ 實現完成 (~180 行)
  - ✅ `/health` - 健康檢查（含 Telegram Bot 連接測試）
  - ✅ `/alert` - Prometheus 告警處理
  - ✅ `/workflow` - Argo Workflow 狀態通知
  - ✅ `/approval` - 人工審批請求（含 UUID 生成）
  - ✅ `/cost` - 成本告警
  - ✅ `send_telegram_message()` - Telegram API 消息發送
  - ✅ `format_workflow_message()` - Workflow 消息格式化
  - ✅ `format_approval_message()` - 審批請求格式化
  - ✅ `format_cost_alert()` - 成本告警格式化

- [x] **test_webhook.py** - ✅ 測試腳本完成 (~200 行)
  - ✅ 5 個端點集成測試
  - ✅ 完整測試報告輸出

- [x] **deploy.sh** - ✅ 實現完成 (~441 行)
  - ✅ 前置條件檢查（kubectl, docker, python3, psql/psycopg2）
  - ✅ 創建 Kubernetes 命名空間（自動處理已存在情況）
  - ✅ 交互式 Secrets 部署（Claude API + Telegram Bot）
  - ✅ RBAC 配置應用
  - ✅ PostgreSQL 審計表創建（Flyway 或直接 SQL）
  - ✅ Docker 鏡像構建（含驗證）
  - ✅ Kubernetes 服務部署（含 Pod 就緒等待）
  - ✅ 部署驗證（資源、Secrets、日誌、Pod 狀態）
  - ✅ Telegram 通知測試（可選）

### P1 (Important - 第二優先級)

- [x] **crews/common/base_crew.py** - ✅ 實現完成 (~420 行)
  - ✅ BaseCrew 抽象基礎類
  - ✅ PostgreSQL 審計日誌集成
  - ✅ Telegram 通知集成
  - ✅ 錯誤處理框架
  - ✅ 配置管理方法

- [x] **crews/common/tools.py** - ✅ 實現完成 (~465 行)
  - ✅ SafeFileAccessTool - 安全文件訪問
  - ✅ CodeAnalysisTool - 代碼分析（Checkstyle, PMD, SpotBugs, ArchUnit）
  - ✅ DatabaseQueryTool - 數據庫查詢和性能分析
  - ✅ GitOperationTool - Git 分支、提交、PR 操作

- [x] **crews/analyzer_crew.py** - ✅ 實現完成 (~360 行)
  - ✅ Java Architect Agent - 架構分析
  - ✅ PostgreSQL Pro Agent - 數據庫性能分析
  - ✅ Code Reviewer Agent - 代碼質量檢查
  - ✅ 審計日誌記錄
  - ✅ Telegram 通知發送
  - ✅ 命令行接口

- [x] **crews/developer_crew.py** - ✅ 實現完成 (~425 行)
  - ✅ Java Architect Agent - 後端實現
  - ✅ Vue Expert Agent - 前端實現
  - ✅ DevOps Engineer Agent - 部署配置
  - ✅ Git 分支創建和提交
  - ✅ PR 自動創建
  - ✅ 命令行接口

- [x] **crews/qa_crew.py** - ✅ 實現完成 (~375 行)
  - ✅ Chaos Engineer Agent - 韌性測試
  - ✅ Code Reviewer Agent - 質量門檐
  - ✅ 批准/拒絕決策邏輯
  - ✅ 測試執行與報告生成
  - ✅ 命令行接口

- [x] **crews/tests/test_all_crews.py** - ✅ 測試腳本完成 (~225 行)
  - ✅ Analyzer Crew 集成測試
  - ✅ Developer Crew 集成測試
  - ✅ QA Crew 集成測試
  - ✅ 完整測試報告輸出

### P2 (Nice-to-have - 第三優先級)

- [ ] **workflows/analyzer-workflow.yaml** - Analyzer Workflow (150 行)
- [ ] **workflows/developer-workflow.yaml** - Developer Workflow (200 行)
- [ ] **workflows/qa-workflow.yaml** - QA Workflow (150 行)

---

## 🧪 測試

### 文件訪問控制測試

```bash
cd tools
python3 -m pytest file_access_guard.py -v
```

預期輸出：7 個測試用例通過

### Telegram Webhook 測試

```bash
# 啟動 Flask 應用（本地測試）
cd telegram-webhook
export TELEGRAM_BOT_TOKEN="your_token_here"
export TELEGRAM_CHAT_ID="your_chat_id_here"
python3 app.py
```

在另一個終端發送測試請求：

```bash
curl -X POST http://localhost:8080/workflow \
  -H "Content-Type: application/json" \
  -d '{"workflow_name":"test","status":"success","message":"Test message"}'
```

預期輸出：HTTP 200 OK，Telegram 收到測試消息

### CrewAI Crews 集成測試

```bash
cd crews/tests
export DB_CONNECTION_STRING="postgresql://postgres:@localhost:5432/smart_admin"
export PROJECT_ROOT="c:/Workspace/open_source/smart-admin"
python3 test_all_crews.py
```

預期輸出：3 個 Crew 測試通過

### Analyzer Crew 單獨測試

```bash
cd crews
python3 analyzer_crew.py --target sa-admin
```

### Developer Crew 單獨測試

```bash
cd crews
python3 developer_crew.py --feature-name "Test Feature" --entity "TestEntity"
```

### QA Crew 單獨測試

```bash
cd crews
python3 qa_crew.py --pr-number 123 --target sa-admin
```

### 部署腳本測試

```bash
cd automation/clawdbot
./deploy.sh
```

預期輸出：所有 9 個步驟順序執行，無錯誤

---

## 📊 實施時間表

| 階段 | 任務 | 預計時間 | 狀態 |
|------|------|---------|------|
| **階段 1** | 創建目錄結構和代碼框架 | 2-3 小時 | ✅ **已完成** |
| **階段 2** | P0 代碼實施 (file_access_guard + webhook + deploy) | 7 天 | ✅ **已完成** (100%) |
| **階段 2.1** | ↳ file_access_guard.py (~571 行) | - | ✅ **已完成** |
| **階段 2.2** | ↳ app.py + test_webhook.py (~735 行) | - | ✅ **已完成** |
| **階段 2.3** | ↳ deploy.sh 實施 (~441 行) | - | ✅ **已完成** |
| **階段 3** | P1 CrewAI Crews 實施 | 10 天 | ✅ **已完成** (100%) |
| **階段 3.1** | ↳ base_crew.py + tools.py (~885 行) | - | ✅ **已完成** |
| **階段 3.2** | ↳ analyzer_crew.py (~360 行) | - | ✅ **已完成** |
| **階段 3.3** | ↳ developer_crew.py (~425 行) | - | ✅ **已完成** |
| **階段 3.4** | ↳ qa_crew.py (~375 行) | - | ✅ **已完成** |
| **階段 3.5** | ↳ test_all_crews.py (~225 行) | - | ✅ **已完成** |
| **階段 4** | P2 Argo Workflows 實施 | 7 天 | ⏳ 計劃中 |

---

## 🔗 相關文檔

- [完整實施計劃](../docs/plans/auto-coding/wiggly-watching-lemon.md)
- [P0-3 安全實施報告](../docs/plans/auto-coding/P0-3-SECURITY-IMPLEMENTATION.md)
- [PostgreSQL 審計表 SQL](../smart-admin-api-java21-springboot3/sa-admin/src/main/resources/db/migration/V999__ai_system_tables.sql)

---

## 🛠️ 開發指南

### 實施 file_access_guard.py

**步驟 1**: 實現 `_check_pattern_match()` 方法

```python
def _check_pattern_match(self, file_path: str, patterns: list[str]) -> Tuple[bool, Optional[str]]:
    for pattern in patterns:
        if re.match(pattern, file_path):
            return (True, pattern)
    return (False, None)
```

**步驟 2**: 實現 `check_access()` 方法

```python
def check_access(self, file_path: str, operation: Operation, agent_name: str) -> AccessCheckResult:
    normalized_path = normalize_path(file_path)

    # 1. Check forbidden patterns
    is_forbidden, pattern = self._check_pattern_match(normalized_path, FORBIDDEN_PATTERNS)
    if is_forbidden:
        return AccessCheckResult(AccessDecision.DENY, f"Forbidden: {pattern}", pattern)

    # 2. Check audit patterns
    is_audit, pattern = self._check_pattern_match(normalized_path, AUDIT_REQUIRED_PATTERNS)
    if is_audit:
        return AccessCheckResult(AccessDecision.AUDIT, f"Audit required: {pattern}", pattern)

    # 3. Check allowed patterns
    patterns = ALLOWED_READ_PATTERNS if operation == Operation.READ else ALLOWED_WRITE_PATTERNS
    is_allowed, pattern = self._check_pattern_match(normalized_path, patterns)
    if is_allowed:
        return AccessCheckResult(AccessDecision.ALLOW, f"Allowed: {pattern}", pattern)

    # 4. Default deny
    return AccessCheckResult(AccessDecision.DENY, "No matching pattern", None)
```

**步驟 3**: 實現 `log_access_decision()` 方法（連接 PostgreSQL）

**步驟 4**: 實現 7 個測試用例

---

### 實施 Telegram Webhook

**步驟 1**: 實現 `send_telegram_message()` 函數

```python
def send_telegram_message(message: str, parse_mode: str = 'Markdown') -> bool:
    try:
        response = requests.post(
            TELEGRAM_API_URL,
            json={'chat_id': TELEGRAM_CHAT_ID, 'text': message, 'parse_mode': parse_mode},
            timeout=10
        )
        return response.status_code == 200
    except Exception as e:
        logger.error(f"Error: {e}")
        return False
```

**步驟 2**: 實現 3 個消息格式化函數

**步驟 3**: 實現 5 個 Flask 端點的處理邏輯

---

## 📞 支持

如需幫助，請查閱：
- [SmartAdmin 官方文檔](https://github.com/1024-lab/smart-admin)
- [Claude API 文檔](https://www.anthropic.com/api)
- [Telegram Bot API 文檔](https://core.telegram.org/bots/api)

---

**聯絡**: SmartAdmin Auto-Coding System
**許可**: MIT License
