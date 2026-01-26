# SmartAdmin AI Agent 系統

**版本**: v1.0.0
**創建日期**: 2026-01-27
**狀態**: P0-3 安全修復已完成 ✅

---

## 📋 目錄

- [系統概述](#系統概述)
- [P0-3 安全修復](#p0-3-安全修復)
- [目錄結構](#目錄結構)
- [快速開始](#快速開始)
- [配置指南](#配置指南)
- [部署步驟](#部署步驟)
- [安全性](#安全性)
- [監控與告警](#監控與告警)

---

## 系統概述

SmartAdmin AI Agent 系統是一個基於 CrewAI 的 24/7 自主開發系統，實現：

- ✅ **多 Agent 協作**：Analyzer、Developer、QA、Meta 四個 Crew
- ✅ **安全訪問控制**：文件白名單 + K8s RBAC + 審計日誌
- ✅ **自動化工作流**：Argo Workflows 編排
- ✅ **實時通知**：Telegram Bot 集成
- ✅ **成本可控**：Claude API 月費訂閱，固定成本

---

## P0-3 安全修復

### 實施內容

| 組件 | 文件位置 | 狀態 |
|------|---------|------|
| **文件訪問白名單** | `tools/file_access_guard.py` | ✅ 完成 |
| **K8s RBAC 配置** | `../k8s-manifests/rbac/agent-role.yaml` | ✅ 完成 |
| **PostgreSQL 審計日誌** | `../smart-admin-api-java21-springboot3/sa-admin/src/main/resources/db/migration/V999__ai_system_tables.sql` | ✅ 完成 |
| **Telegram 通知系統** | `telegram-webhook/` | ✅ 完成 |
| **Claude API 配置** | `../k8s-manifests/secrets/claude-api-credentials.yaml` | ✅ 完成 |

### 安全特性

#### 1. 文件訪問控制

**白名單機制**：
- ✅ 僅允許訪問業務代碼層（Controller/Service/Manager/Dao）
- ✅ 前端代碼（.vue/.ts/.js）
- ✅ 測試代碼
- ✅ MyBatis Mapper XML

**黑名單防護**：
- ❌ 禁止訪問配置文件（application-prod.yml, .env）
- ❌ 禁止訪問安全代碼（SecurityConfig.java）
- ❌ 禁止訪問資料庫遷移腳本（V*.sql）
- ❌ 禁止訪問核心基礎設施（foundation/core）

**審計模式**：
- ⚠️ 訪問 foundation/ 需要人工審批
- ⚠️ 訪問全局配置需要人工審批

**使用示例**：
```python
from tools.file_access_guard import FileAccessGuard

guard = FileAccessGuard()
decision, reason = guard.check_access(
    "src/main/java/controller/EmployeeController.java",
    "write",
    agent_name="java-architect"
)

if decision == AccessDecision.ALLOW:
    # 執行操作
    pass
elif decision == AccessDecision.DENY:
    # 拒絕並記錄
    print(f"Denied: {reason}")
elif decision == AccessDecision.AUDIT:
    # 需要人工審批
    send_approval_request(reason)
```

#### 2. Kubernetes RBAC

**ServiceAccount**: `smartadmin-agent`

**授予權限**（最小權限原則）：
- ✅ Pod/Log: `get`, `list`, `watch`（僅讀取）
- ✅ ConfigMap: `get`, `list`（僅讀取）
- ✅ Secret: `get`（僅限指定的 claude-api-credentials 和 telegram-bot-credentials）
- ✅ Argo Workflows: `create`, `get`, `list`（可創建，但不能刪除）

**禁止權限**：
- ❌ 刪除任何資源（delete, deletecollection）
- ❌ 修改資源（patch, update）
- ❌ 集群級別操作（ClusterRole）
- ❌ 節點操作（nodes）

#### 3. PostgreSQL 審計日誌

**表結構**：

**t_ai_operation_audit**（操作審計）：
- 記錄每次文件訪問（read/write/delete）
- 記錄訪問決策（allow/deny/audit）
- 記錄拒絕原因

**t_ai_execution_log**（執行日誌）：
- 記錄每次 Workflow 執行
- 記錄執行時長、狀態、錯誤信息

**t_agent_performance**（Agent 性能）：
- 記錄 Claude API token 使用量
- 記錄成本（美元）
- 記錄質量評分

**t_skill_usage_stats**（Skill 統計）：
- 記錄 Skill 使用次數
- 記錄執行時間和成功率

**t_cost_attribution**（成本歸因）：
- 記錄每次 LLM 調用成本
- 按 Agent 和模型分類

**數據保留策略**：
- 自動清理 90 天前的數據
- 函數：`cleanup_old_audit_logs()`

---

## 目錄結構

```
k8s-agents/
├── README.md                    # 本文檔
├── requirements.txt             # Python 依賴
├── Dockerfile                   # CrewAI Agent Docker 鏡像
│
├── tools/                       # 工具集
│   └── file_access_guard.py     # P0-3: 文件訪問控制 ✅
│
├── crews/                       # CrewAI Crew 定義（待實施）
│   ├── analyzer_crew.py
│   ├── developer_crew.py
│   ├── qa_crew.py
│   └── meta_crew.py
│
├── monitoring/                  # 監控和成本追蹤（待實施）
│   ├── budget_tracker.py
│   └── performance_monitor.py
│
├── telegram-webhook/            # Telegram 通知服務 ✅
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
│
└── run_*.py                     # Crew 入口腳本（待實施）

k8s-manifests/
├── rbac/
│   └── agent-role.yaml          # P0-3: K8s RBAC 配置 ✅
│
├── secrets/
│   ├── telegram-credentials.yaml    # Telegram Bot 憑證 ✅
│   └── claude-api-credentials.yaml  # Claude API 憑證 ✅
│
└── telegram-webhook/
    └── deployment.yaml          # Telegram Webhook 部署 ✅
```

---

## 快速開始

### 前置要求

- Kubernetes 集群（Kind/EKS/GKE）
- kubectl 已配置
- Docker（用於構建鏡像）
- Claude API 訂閱
- Telegram Bot（通過 @BotFather 創建）

### 步驟 1: 創建命名空間

```bash
kubectl create namespace smartadmin
kubectl config set-context --current --namespace=smartadmin
```

### 步驟 2: 配置 Secrets

#### 2.1 Telegram Bot

1. 在 Telegram 中搜索 @BotFather
2. 發送 `/newbot` 創建新 Bot
3. 獲取 Bot Token
4. 在 Telegram 中搜索你的 Bot 並發送 `/start`
5. 訪問 `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates` 獲取 Chat ID

編輯 `k8s-manifests/secrets/telegram-credentials.yaml`：
```yaml
stringData:
  bot-token: "123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
  chat-id: "123456789"
```

部署：
```bash
kubectl apply -f ../k8s-manifests/secrets/telegram-credentials.yaml
```

#### 2.2 Claude API

1. 訪問 https://console.anthropic.com/ 獲取 API Key
2. 編輯 `k8s-manifests/secrets/claude-api-credentials.yaml`：
```yaml
stringData:
  api-key: "sk-ant-..."
```

部署：
```bash
kubectl apply -f ../k8s-manifests/secrets/claude-api-credentials.yaml
```

### 步驟 3: 部署 RBAC

```bash
kubectl apply -f ../k8s-manifests/rbac/agent-role.yaml
```

驗證：
```bash
kubectl get serviceaccount smartadmin-agent
kubectl get role smartadmin-agent-base
kubectl get rolebinding smartadmin-agent-base-binding
```

### 步驟 4: 創建 PostgreSQL 審計表

**前置條件**：SmartAdmin PostgreSQL 已運行

執行 Flyway 遷移：
```bash
cd ../smart-admin-api-java21-springboot3
./gradlew :sa-admin:flywayMigrate
```

驗證：
```sql
-- 連接到 PostgreSQL
psql -U postgres -d smart_admin

-- 檢查表是否創建
\dt t_ai_*

-- 應顯示：
-- t_ai_operation_audit
-- t_ai_execution_log
-- t_agent_performance
-- t_skill_usage_stats
-- t_cost_attribution
```

### 步驟 5: 部署 Telegram Webhook

構建鏡像：
```bash
cd telegram-webhook
docker build -t smartadmin/telegram-webhook:latest .
```

部署：
```bash
kubectl apply -f ../../k8s-manifests/telegram-webhook/deployment.yaml
```

驗證：
```bash
kubectl get pods -l app=telegram-webhook
kubectl logs -l app=telegram-webhook
```

測試發送消息：
```bash
kubectl port-forward svc/telegram-webhook 8080:8080

curl -X POST http://localhost:8080/workflow \
  -H "Content-Type: application/json" \
  -d '{
    "workflow_name": "test",
    "status": "success",
    "duration": "5s",
    "message": "Test notification from SmartAdmin AI"
  }'
```

您應該在 Telegram 收到消息：
```
✅ Workflow 狀態更新

名稱: test
狀態: success
耗時: 5s

Test notification from SmartAdmin AI
```

---

## 配置指南

### 文件訪問控制配置

編輯 `tools/file_access_guard.py` 調整白名單/黑名單：

```python
# 添加允許的路徑模式
ALLOWED_WRITE_PATTERNS = [
    r".*/(controller|service|manager|dao)/.*\.java$",
    # 添加新模式...
]

# 添加禁止的路徑模式
FORBIDDEN_PATTERNS = [
    r".*application-prod\.yml$",
    # 添加新模式...
]

# 添加審計模式
AUDIT_REQUIRED_PATTERNS = [
    r".*/foundation/.*\.java$",
    # 添加新模式...
]
```

### RBAC 權限調整

編輯 `../k8s-manifests/rbac/agent-role.yaml`：

```yaml
# 授予新權限
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list", "create"]  # 添加 create
```

### PostgreSQL 數據保留策略

編輯遷移腳本中的清理函數：

```sql
-- 修改保留期限（默認 90 天）
DELETE FROM t_ai_operation_audit WHERE created_at < NOW() - INTERVAL '30 days';
```

---

## 部署步驟

### 完整部署流程

```bash
# 1. 創建命名空間
kubectl create namespace smartadmin

# 2. 部署 Secrets
kubectl apply -f ../k8s-manifests/secrets/telegram-credentials.yaml
kubectl apply -f ../k8s-manifests/secrets/claude-api-credentials.yaml

# 3. 部署 RBAC
kubectl apply -f ../k8s-manifests/rbac/agent-role.yaml

# 4. 創建 PostgreSQL 表（在 SmartAdmin 應用內執行）
cd ../smart-admin-api-java21-springboot3
./gradlew :sa-admin:flywayMigrate

# 5. 構建並部署 Telegram Webhook
cd ../k8s-agents/telegram-webhook
docker build -t smartadmin/telegram-webhook:latest .
kubectl apply -f ../../k8s-manifests/telegram-webhook/deployment.yaml

# 6. 驗證部署
kubectl get all -n smartadmin
kubectl get secrets -n smartadmin
kubectl get rolebindings -n smartadmin

# 7. 測試 Telegram 通知
kubectl port-forward svc/telegram-webhook 8080:8080
curl -X POST http://localhost:8080/workflow \
  -H "Content-Type: application/json" \
  -d '{"workflow_name": "test", "status": "success", "message": "Hello from SmartAdmin"}'
```

---

## 安全性

### 已實施的安全措施

| 措施 | 狀態 | 說明 |
|------|------|------|
| **文件白名單** | ✅ | 僅允許訪問業務代碼 |
| **K8s RBAC** | ✅ | 最小權限原則 |
| **審計日誌** | ✅ | 記錄所有操作 |
| **Secret 加密** | ✅ | K8s Secret 存儲憑證 |
| **非 root 運行** | ✅ | 所有容器以非 root 用戶運行 |

### 威脅模型

| 威脅 | 風險等級 | 緩解措施 |
|------|---------|---------|
| **Agent 訪問敏感文件** | 高 | ✅ 文件白名單 + 黑名單 |
| **Agent 刪除資源** | 高 | ✅ K8s RBAC 禁止 delete |
| **Claude API Key 洩漏** | 高 | ✅ K8s Secret + RBAC |
| **惡意代碼注入** | 中 | ✅ 審計日誌 + 人工審批 |
| **成本超支** | 中 | ✅ 月費訂閱（固定成本） |

### 人工審批流程

**P0 問題**（自動批准）：
- Checkstyle 格式化
- 簡單的導入調整

**P1/P2 問題**（需要人工審批）：
- 業務邏輯變更
- 性能優化
- 資料庫查詢修改

**架構重構**（必須人工審批）：
- 訪問 foundation/ 模塊
- 修改全局配置
- 資料庫 Schema 變更

---

## 監控與告警

### Telegram 通知類型

#### 1. 工作流狀態通知

```json
POST /workflow
{
  "workflow_name": "analyzer-workflow",
  "status": "success",
  "duration": "5m30s",
  "message": "Analysis completed successfully"
}
```

#### 2. 人工審批請求

```json
POST /approval
{
  "type": "optimization",
  "priority": "P1",
  "title": "優化 SQL N+1 查詢",
  "description": "發現 EmployeeService 存在 N+1 查詢問題",
  "approval_url": "http://argo-ui:2746/workflows/..."
}
```

#### 3. 成本告警

```json
POST /cost
{
  "period": "daily",
  "current_cost": 75.50,
  "budget_limit": 50.00,
  "agent_breakdown": {
    "java-architect": 30.00,
    "postgres-pro": 20.00
  }
}
```

#### 4. Prometheus 告警

```json
POST /alert
{
  "alerts": [{
    "status": "firing",
    "labels": {
      "alertname": "HighErrorRate",
      "severity": "critical"
    },
    "annotations": {
      "summary": "錯誤率超過 5%"
    }
  }]
}
```

### 審計日誌查詢

```sql
-- 查詢所有被拒絕的操作
SELECT * FROM t_ai_operation_audit
WHERE decision = 'deny'
ORDER BY created_at DESC
LIMIT 100;

-- 查詢特定 Agent 的操作記錄
SELECT * FROM t_ai_operation_audit
WHERE agent_name = 'java-architect'
AND created_at > NOW() - INTERVAL '7 days';

-- 查詢成本統計
SELECT
    agent_name,
    SUM(llm_cost_usd) as total_cost,
    AVG(llm_tokens_used) as avg_tokens
FROM t_agent_performance
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY agent_name
ORDER BY total_cost DESC;
```

---

## 下一步

### 待實施組件

- [ ] CrewAI Analyzer Crew（代碼分析）
- [ ] CrewAI Developer Crew（代碼實施）
- [ ] CrewAI QA Crew（質量保證）
- [ ] CrewAI Meta Crew（自我改進）
- [ ] Argo Workflows 定義
- [ ] Chaos Mesh 混沌實驗
- [ ] Argo Rollouts 金絲雀部署

### 推薦順序

1. **Week 1**: 完成 Analyzer Crew POC
2. **Week 2**: 完成 Developer + QA Crew
3. **Week 3**: 集成 Argo Workflows
4. **Week 4**: 首個自動 PR

---

## 故障排查

### Telegram 消息未收到

```bash
# 檢查 Pod 狀態
kubectl get pods -l app=telegram-webhook

# 查看日誌
kubectl logs -l app=telegram-webhook

# 驗證 Secret
kubectl get secret telegram-bot-credentials -o yaml

# 測試 Telegram API
curl "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getMe"
```

### PostgreSQL 表未創建

```bash
# 檢查 Flyway 狀態
./gradlew :sa-admin:flywayInfo

# 重新運行遷移
./gradlew :sa-admin:flywayMigrate

# 檢查表
psql -U postgres -d smart_admin -c "\dt t_ai_*"
```

### RBAC 權限錯誤

```bash
# 查看 ServiceAccount
kubectl get sa smartadmin-agent

# 查看 RoleBinding
kubectl get rolebinding -l app=smartadmin-ai

# 測試權限
kubectl auth can-i get pods --as=system:serviceaccount:smartadmin:smartadmin-agent
```

---

## 聯繫方式

- **專案**: SmartAdmin
- **文檔版本**: v1.0.0
- **最後更新**: 2026-01-27
- **狀態**: P0-3 安全修復已完成 ✅
