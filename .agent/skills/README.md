# .agent/skills/ - Antigravity AI Skills v2.0.0

## 系統定位

| 目錄 | 目標使用者 | 技能數量 | 格式 |
|------|-----------|---------|------|
| **.claude/skills/** | Claude Code CLI | 35個 | Claude Code 專用語法 |
| **.agent/skills/** | Antigravity, Gemini 等 | **29個** ✅ | 通用 AI 指令格式 |

> **Note**: 全部 29 個技能已完成遷移 (P0: 6個, P1: 8個, P2: 15個)。

**為什麼分開？**
- Claude Code 有特定的技能調用機制 (`[skill:name]` 語法)
- Antigravity 和其他 AI 需要更通用的指令結構
- 兩者共享 `.agent/rules/` 架構規則

---

## 快速導航

### P0 Foundation (6 skills) - 核心技能

| 技能 | 用途 | 路徑 |
|------|------|------|
| **archunit-test-generator** | 生成 ArchUnit 架構測試 | [foundation/backend/](foundation/backend/archunit-test-generator/) |
| **security-hardening-pro** | 安全加固 (SM2/SM3/SM4, XSS/CSRF) | [foundation/backend/](foundation/backend/security-hardening-pro/) |
| **vavr-refactoring-assistant** | Vavr Option/Try/Either 重構 | [foundation/backend/](foundation/backend/vavr-refactoring-assistant/) |
| **smartadmin-crud-generator** | 完整 CRUD 模塊生成 | [foundation/full-stack/](foundation/full-stack/smartadmin-crud-generator/) |
| **smartadmin-integration-test** | Testcontainers 整合測試 | [foundation/full-stack/](foundation/full-stack/smartadmin-integration-test/) |
| **test-fixture-generator** | 測試數據建構器 | [foundation/testing/](foundation/testing/test-fixture-generator/) |

### P1 Extended (8 skills) - 擴展技能

| 技能 | 用途 | 路徑 |
|------|------|------|
| **liteflow-rule-builder** | LiteFlow 規則 DSL 生成 | [extended/domain/](extended/domain/liteflow-rule-builder/) |
| **fraud-detection-pattern-generator** | 風控反詐系統 | [extended/domain/](extended/domain/fraud-detection-pattern-generator/) |
| **igame-feature-builder** | iGaming 功能模塊 | [extended/domain/](extended/domain/igame-feature-builder/) |
| **igame-pm-analyst** | iGaming 產品分析 | [extended/domain/](extended/domain/igame-pm-analyst/) |
| **quality-gate-orchestrator** | 品質門檻編排 | [extended/orchestration/](extended/orchestration/quality-gate-orchestrator/) |
| **concurrency-safety-auditor** | 並發安全審計 | [extended/quality/](extended/quality/concurrency-safety-auditor/) |
| **spring-pattern-checker** | Spring 模式驗證 | [extended/quality/](extended/quality/spring-pattern-checker/) |
| **naming-convention-checker** | 命名規範檢查 | [extended/quality/](extended/quality/naming-convention-checker/) |

### P2 Productivity (15 skills) - 生產力技能

| 類別 | 技能 | 用途 |
|------|------|------|
| **DevOps** | apm-integration | APM 監控整合 |
| | cicd-pipeline-builder | CI/CD 管線建置 |
| | db-migration-manager | 資料庫遷移管理 |
| | scheduled-task-manager | 排程任務管理 |
| | websocket-sse-realtime-generator | 即時通訊生成 |
| **Integration** | cache-strategy-generator | 多層快取策略 |
| | full-text-search-integration | Elasticsearch 整合 |
| | i18n-generator | 國際化生成 |
| | message-queue-pattern-generator | 消息隊列整合 |
| | postgresql-best-practices | PostgreSQL 優化 |
| | report-generator | 報表匯出生成 |
| **Composite** | smartadmin-performance-suite | 效能優化套件 |
| | smartadmin-testing-suite | 測試套件 |
| **Refactoring** | smartadmin-manager-extractor | Manager 層提取 |
| | markdown-quality-checker | Markdown 品質檢查 |

---

## 目錄結構

```
.agent/skills/
├── skill-registry.yml          # SSOT: 所有技能元數據
├── VERSIONS.yml                # 版本追蹤
├── README.md                   # 本文件
│
├── foundation/                 # P0 - 6 skills (核心)
│   ├── backend/                # 3 skills
│   │   ├── archunit-test-generator/
│   │   ├── security-hardening-pro/
│   │   └── vavr-refactoring-assistant/
│   ├── full-stack/             # 2 skills
│   │   ├── smartadmin-crud-generator/
│   │   └── smartadmin-integration-test/
│   └── testing/                # 1 skill
│       └── test-fixture-generator/
│
├── extended/                   # P1 - 8 skills (擴展)
│   ├── domain/                 # 4 skills
│   │   ├── liteflow-rule-builder/
│   │   ├── fraud-detection-pattern-generator/
│   │   ├── igame-feature-builder/
│   │   └── igame-pm-analyst/
│   ├── orchestration/          # 1 skill
│   │   └── quality-gate-orchestrator/
│   └── quality/                # 3 skills
│       ├── concurrency-safety-auditor/
│       ├── spring-pattern-checker/
│       └── naming-convention-checker/
│
├── productivity/               # P2 - 15 skills (生產力)
│   ├── devops/                 # 5 skills
│   ├── integration/            # 6 skills
│   ├── composite/              # 2 skills
│   └── refactoring/            # 2 skills
│
└── _shared/                    # 共享資源
    ├── templates/              # Antigravity 模板
    └── references/             # 通用參考
```

---

## 技能標準結構

每個技能目錄包含：

```
{skill-name}/
├── SKILL.md                    # 技能定義 (必須)
├── config.yml                  # 元數據配置 (必須)
├── knowledge/                  # 知識庫 (必須)
│   ├── quick-reference.md      # 快速參考
│   └── examples.md             # 使用範例
└── templates/                  # 代碼模板 (選用)
```

---

## Antigravity SKILL.md 格式

```markdown
---
name: {skill-name}
description: {簡短描述}
priority: P0|P1|P2
category: foundation|extended|productivity
---

# {Skill Name}

{概述段落}

## Usage

User: "{示範請求}"
AI: [執行步驟]

## When to Use

- {觸發條件 1}
- {觸發條件 2}

## Workflow

1. {步驟 1}
2. {步驟 2}

## Related Rules

- [Rule Name](../../rules/xxx.md)

## Example Session

**User:** {請求}
**AI:** {動作}
```

---

## 與 .claude/skills/ 的差異

| 元素 | .claude/ (Claude Code) | .agent/ (Antigravity) |
|------|------------------------|----------------------|
| Trigger Keywords | Primary/Secondary 分類 | "When to Use" 條件列表 |
| Phase Execution | `--backend-only` 等 flags | 描述式工作流 |
| Skill References | `[skill:name]` 語法 | 完整路徑引用 |
| Backward Compatibility | skill-aliases.json | 不需要 |

---

## 技能選擇指南

### 按場景選擇

| 場景 | 推薦技能 | 優先級 |
|------|----------|--------|
| 新建 CRUD 模塊 | smartadmin-crud-generator | P0 |
| ArchUnit 測試失敗 | archunit-test-generator, spring-pattern-checker | P0, P1 |
| @Transactional 位置錯誤 | smartadmin-manager-extractor | P2 |
| 並發安全審計 | concurrency-safety-auditor | P1 |
| 效能問題診斷 | smartadmin-performance-suite | P2 |
| 資料庫查詢慢 | postgresql-best-practices | P2 |
| CI/CD 設定 | cicd-pipeline-builder | P2 |

### 按 ArchUnit 錯誤選擇

| 錯誤訊息 | 對應技能 |
|----------|----------|
| "Service uses java.util.Optional" | vavr-refactoring-assistant |
| "@Transactional not in Manager layer" | smartadmin-manager-extractor |
| "Controller calls Dao directly" | spring-pattern-checker |
| "Field injection detected" | spring-pattern-checker |

---

## 版本資訊

| 組件 | 版本 | 狀態 |
|------|------|------|
| Skills Registry | 2.0.0 | ✅ Stable |
| 總技能數 | 29 | ✅ Complete |
| P0 Foundation | 6 | ✅ Complete |
| P1 Extended | 8 | ✅ Complete |
| P2 Productivity | 15 | ✅ Complete |

**更新日期**: 2026-02-05

---

## 相關文檔

- [skill-registry.yml](skill-registry.yml) - 技能元數據 SSOT
- [VERSIONS.yml](VERSIONS.yml) - 版本追蹤
- [.agent/rules/00-INDEX.md](../rules/00-INDEX.md) - 規則路由中心
- [CLAUDE.md](../../CLAUDE.md) - 專案入口點
