# Workflow Orchestrator - Quick Start Guide

**版本**: 2.0.0
**日期**: 2026-01-28
**狀態**: ✅ 已實施

---

## 📋 概述

Workflow Orchestrator 是 SmartAdmin Auto-Coding 的核心編排器，統一管理所有 CrewAI Crews 的執行，實現完整的 Telegram → Orchestrator → CrewAI → Claude AI 全自動化流程。

### 新增功能 (v2.0.0)

1. ✅ **Workflow Orchestrator** (`orchestrator.py`)
   - 統一管理 3 個 Crews (Analyzer, Developer, QA)
   - 實現工作流串接 (Analyzer → Developer → QA)
   - 提供單個 Crew 觸發接口

2. ✅ **Telegram Command Handler** (`telegram-webhook/command_handler.py`)
   - 處理 Telegram 命令 (`/new_feature`, `/code_review`, `/analyze`, `/help`)
   - 調用 Orchestrator 觸發工作流
   - 格式化執行結果為 Markdown 消息

3. ✅ **Telegram Webhook 增強** (`telegram-webhook/app.py` v2.0.0)
   - 新增 `/command` 端點
   - 集成 Command Handler
   - 健康檢查顯示 Command Handler 狀態

---

## 🚀 Quick Start

### 1. 驗證安裝

確保所有新文件已創建：

```bash
# 檢查文件是否存在
ls -la automation/clawdbot/orchestrator.py
ls -la automation/clawdbot/telegram-webhook/command_handler.py
ls -la automation/clawdbot/test_orchestrator.py

# 檢查 app.py 版本
grep "版本:" automation/clawdbot/telegram-webhook/app.py
# 應該顯示：版本: 2.0.0
```

### 2. 啟動 Telegram Webhook

```bash
cd automation/clawdbot/telegram-webhook

# 設置環境變量
export TELEGRAM_BOT_TOKEN="your_bot_token"
export TELEGRAM_CHAT_ID="your_chat_id"
export CLAUDE_API_KEY="your_claude_api_key"
export DB_CONNECTION_STRING="postgresql://user:pass@localhost:5432/smart_admin"
export PROJECT_ROOT="/path/to/smart-admin"

# 啟動服務
python3 app.py
```

預期輸出：
```
INFO - TelegramCommandHandler initialized successfully
INFO - Starting Telegram Webhook Service...
INFO - Version: 2.0.0
INFO - Listening on: 0.0.0.0:8080
```

### 3. 運行測試

在另一個終端運行測試：

```bash
cd automation/clawdbot

# 基礎測試（不執行真實 Crews）
python3 test_orchestrator.py

# 完整測試（WARNING: 會創建真實 PR）
python3 test_orchestrator.py --enable-crew-tests
```

預期輸出：
```
======================================================================
Test Summary
======================================================================
✅ PASS - Health Check
✅ PASS - /help Command
✅ PASS - Orchestrator Import
✅ PASS - Command Handler Import
✅ PASS - /new_feature Command (Dry Run)

----------------------------------------------------------------------
Total: 5/5 tests passed (100%)
======================================================================
```

---

## 🎯 使用方式

### 方式 1: HTTP API (推薦用於測試)

**1. 健康檢查**

```bash
curl http://localhost:8080/health
```

響應：
```json
{
  "status": "healthy",
  "version": "2.0.0",
  "telegram_bot": "connected",
  "command_handler": "enabled"
}
```

**2. 執行 /help 命令**

```bash
curl -X POST http://localhost:8080/command \
  -H "Content-Type: application/json" \
  -d '{
    "command": "/help",
    "args": {}
  }'
```

**3. 創建新功能（完整工作流）**

```bash
curl -X POST http://localhost:8080/command \
  -H "Content-Type: application/json" \
  -d '{
    "command": "/new_feature",
    "args": {
      "name": "Employee Management",
      "entity": "Employee",
      "target": "sa-admin"
    }
  }'
```

這將觸發：
1. **Analyzer Crew** - 分析現有代碼結構
2. **Developer Crew** - 使用 Claude AI 生成代碼
3. **QA Crew** - 執行質量檢查
4. **自動創建 PR** - 提交到 GitHub
5. **發送 Telegram 通知** - 通知完成狀態

**4. 代碼審查**

```bash
curl -X POST http://localhost:8080/command \
  -H "Content-Type: application/json" \
  -d '{
    "command": "/code_review",
    "args": {
      "pr_number": 123,
      "target": "sa-admin"
    }
  }'
```

**5. 代碼分析**

```bash
curl -X POST http://localhost:8080/command \
  -H "Content-Type: application/json" \
  -d '{
    "command": "/analyze",
    "args": {
      "target": "sa-admin"
    }
  }'
```

---

### 方式 2: Python 直接調用

**1. 使用 Orchestrator**

```python
from orchestrator import WorkflowOrchestrator

# 創建 Orchestrator
orchestrator = WorkflowOrchestrator()

# 執行完整工作流
result = orchestrator.trigger_workflow(
    workflow_type='full_feature',
    params={
        'name': 'Employee Management',
        'entity': 'Employee',
        'target': 'sa-admin'
    }
)

print(result)
# {
#   "status": "SUCCESS",
#   "pr_url": "https://github.com/...",
#   "duration": "5m30s",
#   ...
# }

# 清理資源
orchestrator.cleanup()
```

**2. 使用 Command Handler**

```python
from telegram-webhook.command_handler import TelegramCommandHandler

# 創建 Handler
handler = TelegramCommandHandler()

# 處理命令
result = handler.handle_command('/new_feature', {
    'name': 'Employee Management',
    'entity': 'Employee'
})

print(result)
# "✅ Feature Created Successfully\n..."

# 清理資源
handler.cleanup()
```

---

### 方式 3: 命令行接口

**1. Orchestrator CLI**

```bash
# 完整工作流
python3 automation/clawdbot/orchestrator.py full_feature \
  --name "Employee Management" \
  --entity "Employee" \
  --target sa-admin

# 代碼審查
python3 automation/clawdbot/orchestrator.py code_review \
  --pr-number 123 \
  --target sa-admin

# 代碼分析
python3 automation/clawdbot/orchestrator.py analysis_only \
  --target sa-admin
```

---

## 📊 工作流執行流程

### Full Feature Workflow

```
用戶發送命令
    ↓
Telegram Webhook (/command)
    ↓
TelegramCommandHandler.handle_command()
    ↓
WorkflowOrchestrator.trigger_workflow('full_feature')
    ↓
┌─────────────────────────────────────┐
│ Step 1: Analyzer Crew               │
│ - 分析現有代碼結構                    │
│ - 識別設計模式                        │
│ - 檢測潛在問題                        │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 2: Developer Crew              │
│ - 創建 Git 分支                      │
│ - Claude AI 生成代碼                 │
│   • Entity, Dao, Manager, Service   │
│   • Controller, Form, VO            │
│   • Vue components, API client      │
│ - 提交更改                           │
│ - 創建 Pull Request                  │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 3: QA Crew                     │
│ - 執行 ArchUnit 測試                 │
│ - 檢查代碼質量                        │
│ - 韌性測試                           │
│ - 批准/拒絕決策                       │
└─────────────────────────────────────┘
    ↓
發送 Telegram 通知（包含 PR 鏈接）
```

---

## 🔧 支持的命令

### /new_feature

**功能**: 創建新功能（完整工作流）

**參數**:
- `name` (required): 功能名稱
- `entity` (required): 實體名稱
- `target` (optional): 目標模塊，默認 "sa-admin"
- `endpoints` (optional): API 端點列表，默認 ["list", "add", "update", "delete"]
- `views` (optional): 前端視圖列表，默認 ["list", "form"]
- `validation` (optional): 驗證規則

**示例**:
```bash
curl -X POST http://localhost:8080/command \
  -d '{
    "command": "/new_feature",
    "args": {
      "name": "Employee Management",
      "entity": "Employee"
    }
  }'
```

**輸出**:
```
✅ Feature Created Successfully

Command: /new_feature
Feature: Employee Management
Duration: 5m30s

Workflow Steps:
• Step 1: Analyzer - SUCCESS
• Step 2: Developer - SUCCESS
• Step 3: QA - SUCCESS

Output:
• Branch: feature/employee-management
• PR: https://github.com/.../pull/123

Next Steps:
1. Review the generated code
2. Run tests locally
3. Approve and merge the PR
```

---

### /code_review

**功能**: 代碼審查

**參數**:
- `pr_number` (required): PR 編號
- `target` (optional): 目標模塊，默認 "sa-admin"

**示例**:
```bash
curl -X POST http://localhost:8080/command \
  -d '{
    "command": "/code_review",
    "args": {
      "pr_number": 123
    }
  }'
```

**輸出**:
```
✅ Code Review Completed

PR: #123
Decision: APPROVED
Duration: 2m15s

QA Report:
- ArchUnit tests: PASSED
- Code quality: PASSED
- Security checks: PASSED
```

---

### /analyze

**功能**: 代碼分析

**參數**:
- `target` (required): 目標模塊

**示例**:
```bash
curl -X POST http://localhost:8080/command \
  -d '{
    "command": "/analyze",
    "args": {
      "target": "sa-admin"
    }
  }'
```

**輸出**:
```
✅ Code Analysis Completed

Target: sa-admin
Duration: 1m45s

Analysis Summary:
- Architecture: Compliant with SmartAdmin patterns
- Code quality: Good (85/100)
- Database performance: No slow queries detected
```

---

### /help

**功能**: 顯示幫助信息

**參數**: 無

**示例**:
```bash
curl -X POST http://localhost:8080/command \
  -d '{
    "command": "/help",
    "args": {}
  }'
```

---

## 🛠️ 故障排除

### 問題 1: Command Handler 未啟用

**症狀**: 健康檢查顯示 `"command_handler": "disabled"`

**解決方案**:
```bash
# 檢查 command_handler.py 是否存在
ls -la automation/clawdbot/telegram-webhook/command_handler.py

# 檢查 app.py 是否正確導入
grep "TelegramCommandHandler" automation/clawdbot/telegram-webhook/app.py

# 檢查錯誤日誌
# 查看 Flask 啟動時的 logger.error 消息
```

---

### 問題 2: Orchestrator 導入失敗

**症狀**: `ModuleNotFoundError: No module named 'crews'`

**解決方案**:
```bash
# 確保 PYTHONPATH 正確設置
export PYTHONPATH=/path/to/smart-admin

# 或使用絕對路徑運行
cd /path/to/smart-admin
python3 automation/clawdbot/orchestrator.py ...
```

---

### 問題 3: Crews 執行失敗

**症狀**: 工作流執行時報錯

**解決方案**:
```bash
# 1. 檢查環境變量
echo $CLAUDE_API_KEY
echo $DB_CONNECTION_STRING
echo $PROJECT_ROOT

# 2. 測試單個 Crew
python3 automation/clawdbot/crews/analyzer_crew.py --target sa-admin

# 3. 查看審計日誌
psql -U postgres -d smart_admin -c "SELECT * FROM t_ai_execution_log ORDER BY id DESC LIMIT 10;"
```

---

### 問題 4: Telegram 通知未發送

**症狀**: 命令執行成功但沒有收到 Telegram 通知

**解決方案**:
```bash
# 1. 檢查 Telegram 配置
echo $TELEGRAM_BOT_TOKEN
echo $TELEGRAM_CHAT_ID

# 2. 測試 Telegram Bot
curl https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/getMe

# 3. 手動發送測試消息
curl -X POST http://localhost:8080/workflow \
  -d '{"workflow_name":"test","status":"success","message":"Test"}'
```

---

## 📈 性能考慮

### 執行時間預估

| 工作流類型 | 預估時間 | 主要耗時環節 |
|-----------|---------|-------------|
| **Full Feature** | 5-10 分鐘 | Developer Crew (Claude AI 代碼生成) |
| **Code Review** | 2-5 分鐘 | QA Crew (ArchUnit 測試 + 代碼質量檢查) |
| **Analysis** | 1-3 分鐘 | Analyzer Crew (代碼掃描 + 數據庫查詢) |

### 並發限制

當前實現為**同步執行**，不支持並發。如需並發，請實施：

1. **任務隊列** (Redis + Celery)
2. **異步執行** (asyncio)
3. **Kubernetes Jobs** (Argo Workflows)

---

## 🔐 安全注意事項

1. **環境變量保護**
   - 使用 Kubernetes Secrets 存儲敏感信息
   - 不要在代碼中硬編碼 API Key

2. **文件訪問控制**
   - file_access_guard.py 強制白名單/黑名單
   - 路徑遍歷攻擊防護

3. **審計日誌**
   - 所有操作記錄到 PostgreSQL
   - 便於事後追蹤和審計

---

## 📝 下一步

### 立即可用

- ✅ 從 Telegram 命令觸發完整工作流
- ✅ Analyzer → Developer → QA 自動串接
- ✅ Claude AI 代碼生成
- ✅ 自動創建 PR
- ✅ Telegram 通知

### 未來增強（可選）

- [ ] **任務隊列** - 支持異步和並發執行
- [ ] **Argo Workflows** - Kubernetes 原生編排
- [ ] **Web Dashboard** - 可視化工作流監控
- [ ] **更多命令** - `/rollback`, `/deploy`, `/status`

---

## 📞 支持

如需幫助，請查閱：

- [架構分析報告](~/.claude-analysis/clawdbot-architecture-analysis.md)
- [SmartAdmin 文檔](https://github.com/1024-lab/smart-admin)
- [CrewAI 文檔](https://docs.crewai.com/)
- [Claude API 文檔](https://docs.anthropic.com/)

---

**版本**: 2.0.0
**生成時間**: 2026-01-28
**作者**: Claude Sonnet 4.5
