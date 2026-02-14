# Week 2-3 執行計劃：分類重組 + Agent-Skill 邊界定義

**時間範圍**: Week 2-3 (10 工作日)
**狀態**: 🟢 執行中
**目標**: 解決過度分散的分類問題，明確 Public vs Internal Skills 邊界

---

## 📋 執行清單

### Phase 1: Foundation 重組 (Day 1-2)

**目標**: 合併 `foundation/{backend,full-stack,testing}` → `foundation/core/`

**當前結構**：
```
foundation/
├── backend/                    (3 個技能)
│   ├── archunit-test-generator
│   ├── security-hardening-pro
│   └── vavr-refactoring-assistant
├── full-stack/                 (2 個技能)
│   ├── smartadmin-crud-generator
│   └── smartadmin-integration-test
└── testing/                    (1 個技能)
    └── test-fixture-generator
```

**目標結構**：
```
foundation/
└── core/                       (6 個技能)
    ├── archunit-test-generator
    ├── security-hardening-pro
    ├── vavr-refactoring-assistant
    ├── smartadmin-crud-generator
    ├── smartadmin-integration-test
    └── test-fixture-generator
```

**操作步驟**：
1. ✅ 創建 `foundation/core/` 目錄
2. ⏳ 移動 3 個 backend 技能到 core/
3. ⏳ 移動 2 個 full-stack 技能到 core/
4. ⏳ 移動 1 個 testing 技能到 core/
5. ⏳ 刪除空目錄 (backend/, full-stack/, testing/)
6. ⏳ 更新 6 個技能的 SKILL.md 中的路徑引用
7. ⏳ 更新 skill-registry.yml 路徑

**驗證**：
```bash
ls foundation/core/  # 應該顯示 6 個技能
ls foundation/backend 2>/dev/null && echo "ERROR: backend still exists"
```

---

### Phase 2: Extended 重組 (Day 3-4)

**目標**: 重新組織 extended/ 分類

#### 2.1 移動 liteflow-rule-builder

**操作**：
```
extended/business-logic/liteflow-rule-builder
  → extended/domain/liteflow-rule-builder
```

**原因**: liteflow 是業務規則引擎，更適合放在 domain（領域）分類

#### 2.2 移動 quality-gate-orchestrator

**操作**：
```
extended/quality/quality-gate-orchestrator
  → extended/orchestration/quality-gate-orchestrator
```

**原因**: quality-gate 是編排多個工具的技能，屬於 orchestration 類型

#### 2.3 刪除空目錄

**操作**：
- 刪除 `extended/business-logic/`（移動後為空）
- 刪除 `extended/quality/`（移動後為空）

**最終結構**：
```
extended/
├── domain/                     (5 個技能)
│   ├── fraud-detection-pattern-generator
│   ├── igame-feature-builder
│   ├── igame-pm-analyst
│   ├── igaming-multi-tenant-wallet-pm
│   └── liteflow-rule-builder   ← 新增
└── orchestration/              (2 個技能)
    ├── batch-plan-executor
    └── quality-gate-orchestrator ← 新增
```

**驗證**：
```bash
ls extended/domain/ | wc -l  # 應該是 5
ls extended/orchestration/ | wc -l  # 應該是 2
```

---

### Phase 3: Productivity 重組 (Day 5-7)

**目標**: 拆分 `productivity/infrastructure/` 並創建 `refactoring/`

#### 3.1 創建新分類

**操作**：
1. 創建 `productivity/devops/`
2. 創建 `productivity/integration/`
3. 創建 `productivity/refactoring/`

#### 3.2 拆分 infrastructure (10 個技能)

**DevOps 類** (5 個) → `productivity/devops/`:
- apm-integration-skill (APM 監控)
- cicd-pipeline-builder (CI/CD 管道)
- db-migration-manager (數據庫遷移)
- scheduled-task-manager (定時任務)
- websocket-sse-realtime-generator (實時通訊)

**Integration 類** (5 個) → `productivity/integration/`:
- cache-strategy-generator (緩存策略)
- full-text-search-integration (全文搜索)
- i18n-generator (國際化)
- message-queue-pattern-generator (消息隊列)
- report-generator-skill (報表生成)

#### 3.3 移動 vavr-refactoring-assistant

**操作**：
```
foundation/core/vavr-refactoring-assistant
  → productivity/refactoring/vavr-refactoring-assistant
```

**原因**: 重構工具更適合放在 productivity 而非 foundation

#### 3.4 刪除空目錄

**操作**：
- 刪除 `productivity/infrastructure/`（全部移走後為空）

**最終結構**：
```
productivity/
├── analysis/                   (1 個技能)
│   └── java-performance-pro
├── composite/                  (2 個技能)
│   ├── smartadmin-performance-suite
│   └── smartadmin-testing-suite
├── devops/                     (5 個技能) ← 新建
│   ├── apm-integration-skill
│   ├── cicd-pipeline-builder
│   ├── db-migration-manager
│   ├── scheduled-task-manager
│   └── websocket-sse-realtime-generator
├── integration/                (5 個技能) ← 新建
│   ├── cache-strategy-generator
│   ├── full-text-search-integration
│   ├── i18n-generator
│   ├── message-queue-pattern-generator
│   └── report-generator-skill
└── refactoring/                (1 個技能) ← 新建
    └── vavr-refactoring-assistant
```

**驗證**：
```bash
ls productivity/devops/ | wc -l  # 應該是 5
ls productivity/integration/ | wc -l  # 應該是 5
ls productivity/refactoring/ | wc -l  # 應該是 1
```

---

### Phase 4: Metadata 更新 (Day 8-9)

**目標**: 更新 skill-registry.yml 添加新字段

#### 4.1 添加 visibility 字段

**定義**：
- `public`: 用戶可直接調用的技能
- `internal`: 僅供 Agent 內部使用的技能

**分類標準**：
- Public Skills: 用戶明確知道並會主動請求的功能
- Internal Skills: Agent 判斷需要使用的底層工具

#### 4.2 添加 preferred_via 字段

**定義**：
- `user_invoked`: 優先通過用戶明確請求觸發
- `agent_only`: 僅由 Agent 根據上下文自動觸發
- `hybrid`: 兩種方式皆可

#### 4.3 更新範例

**Before**:
```yaml
smartadmin-crud-generator:
  path: "foundation/full-stack/smartadmin-crud-generator"
  priority: "P0"
  type: "composite"
  phases: ["phase-1-backend", "phase-2-frontend", "phase-3-api-docs", "phase-4-tests"]
```

**After**:
```yaml
smartadmin-crud-generator:
  path: "foundation/core/smartadmin-crud-generator"  # 更新路徑
  priority: "P0"
  type: "composite"
  visibility: "public"                               # 新增
  preferred_via: "user_invoked"                      # 新增
  phases: ["phase-1-backend", "phase-2-frontend", "phase-3-api-docs", "phase-4-tests"]
```

#### 4.4 所有技能分類

**Public Skills (用戶直接調用)** - 19 個:

P0 Foundation (6):
- smartadmin-crud-generator (CRUD 生成器)
- smartadmin-integration-test (集成測試)
- security-hardening-pro (安全加固)
- archunit-test-generator (架構測試)
- test-fixture-generator (測試數據)
- vavr-refactoring-assistant (Vavr 重構)

P1 Extended (7):
- igame-feature-builder (iGaming 功能)
- igame-pm-analyst (iGaming PM)
- igaming-multi-tenant-wallet-pm (多租戶錢包)
- fraud-detection-pattern-generator (反欺詐)
- liteflow-rule-builder (業務規則)
- batch-plan-executor (批量計劃)
- quality-gate-orchestrator (質量門禁)

P2 Productivity (6):
- java-performance-pro (性能優化)
- smartadmin-performance-suite (性能套件)
- smartadmin-testing-suite (測試套件)
- db-migration-manager (數據庫遷移)
- cicd-pipeline-builder (CI/CD)
- scheduled-task-manager (定時任務)

**Internal Skills (Agent 內部使用)** - 10 個:

P2 Productivity (10):
- apm-integration-skill (APM 集成)
- cache-strategy-generator (緩存策略)
- full-text-search-integration (全文搜索)
- i18n-generator (國際化)
- message-queue-pattern-generator (消息隊列)
- report-generator-skill (報表生成)
- websocket-sse-realtime-generator (實時通訊)

---

### Phase 5: 文檔更新 (Day 10)

**目標**: 更新所有引用文檔

#### 5.1 更新 SKILL-INVOCATION-GUIDE.md

**新增章節**：
- Public vs Internal Skills 定義
- visibility 和 preferred_via 字段說明
- 調用決策流程圖

#### 5.2 更新 .claude/skills/README.md

**更新內容**：
- 新的目錄結構（v4.0.0）
- 分類邏輯說明
- 技能總數統計

#### 5.3 更新 CLAUDE.md

**更新章節**：
- "Specialized Skills" 章節更新技能列表
- 新增 v4.0.0 結構說明

#### 5.4 創建遷移報告

**文檔**: `WEEK2-3-COMPLETION-REPORT.md`

**內容**：
- 重組前後對比
- 移動操作記錄
- 破壞性變更說明
- 驗證結果

---

## 🎯 成功指標

| 指標 | 目標 | 驗證方式 |
|------|------|---------|
| Foundation 分類 | 1 個 (core) | `ls foundation/ \| wc -l` = 1 |
| Extended 分類 | 2 個 (domain, orchestration) | `ls extended/ \| wc -l` = 2 |
| Productivity 分類 | 5 個 (analysis, composite, devops, integration, refactoring) | `ls productivity/ \| wc -l` = 5 |
| skill-registry.yml | 所有技能有 visibility 和 preferred_via | 手動檢查 |
| 文檔更新 | 4 個文檔已更新 | Git diff |
| 破壞性變更 | 路徑變更已記錄 | WEEK2-3-COMPLETION-REPORT.md |

---

## 🔄 Git Commit 策略

每個 Phase 完成後創建一個 commit：

**Phase 1**: `refactor(skills): merge foundation categories into core/ (v4.0.0 Week 2 Day 1-2)`
**Phase 2**: `refactor(skills): reorganize extended categories (v4.0.0 Week 2 Day 3-4)`
**Phase 3**: `refactor(skills): split productivity/infrastructure into devops and integration (v4.0.0 Week 2 Day 5-7)`
**Phase 4**: `feat(skills): add visibility and preferred_via metadata (v4.0.0 Week 3 Day 8-9)`
**Phase 5**: `docs(skills): update documentation for v4.0.0 reorganization (v4.0.0 Week 3 Day 10)`

---

## 🛡️ 回滾策略

每個 Phase 獨立 commit，可以逐個回滾：

```bash
# 回滾最後一個 Phase
git revert HEAD

# 回滾特定 Phase
git revert <commit-hash>

# 回滾所有 Week 2-3 變更
git revert HEAD~5..HEAD
```

---

## 📊 預計影響範圍

- **技能目錄移動**: 17 個
- **skill-registry.yml**: 29 個條目更新
- **SKILL.md 更新**: ~20 個文件
- **文檔更新**: 4 個文件
- **總文件變更**: 預計 80-100 個文件

---

**準備好了嗎？** 開始 Phase 1: Foundation 重組！
