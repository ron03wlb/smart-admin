# .agent/skills/ - Antigravity AI Skills v3.0.0

## 系統定位

| 目錄 | 目標使用者 | 技能數量 | 格式 |
|------|-----------|---------|------|
| **.claude/skills/** | Claude Code CLI | 15 個 | Claude Code 專用語法 |
| **.agent/skills/** | Antigravity, Gemini 等 | **14 個** ✅ | 通用 AI 指令格式 |

> **Note**: `.claude/` 多 1 個前端技能 (smartadmin-react-crud)，其餘 14 個技能兩邊完全對齊。

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
| **igame-feature-builder** | iGaming 功能模塊 | [extended/domain/](extended/domain/igame-feature-builder/) |
| **igame-pm-analyst** | iGaming 產品分析 | [extended/domain/](extended/domain/igame-pm-analyst/) |
| **concurrency-safety-auditor** | 並發安全審計 | [extended/quality/](extended/quality/concurrency-safety-auditor/) |
| **spring-pattern-checker** | Spring 模式驗證 | [extended/quality/](extended/quality/spring-pattern-checker/) |
| **naming-convention-checker** | 命名規範檢查 | [extended/quality/](extended/quality/naming-convention-checker/) |
| **smartadmin-manager-extractor** | Manager 層提取 | [extended/quality/](extended/quality/smartadmin-manager-extractor/) |
| **postgresql-best-practices** | PostgreSQL 優化 | [extended/quality/](extended/quality/postgresql-best-practices/) |

---

## 目錄結構

```
.agent/skills/
├── skill-registry.yml          # SSOT: 所有技能元數據
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
│   ├── domain/                 # 3 skills
│   │   ├── liteflow-rule-builder/
│   │   ├── igame-feature-builder/
│   │   └── igame-pm-analyst/
│   └── quality/                # 5 skills
│       ├── concurrency-safety-auditor/
│       ├── spring-pattern-checker/
│       ├── naming-convention-checker/
│       ├── smartadmin-manager-extractor/
│       └── postgresql-best-practices/
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
└── knowledge/                  # 知識庫 (必須)
    └── quick-reference.md      # 快速參考
```

---

## 技能選擇指南

### 按場景選擇

| 場景 | 推薦技能 | 優先級 |
|------|----------|--------|
| 新建 CRUD 模塊 | smartadmin-crud-generator | P0 |
| ArchUnit 測試失敗 | archunit-test-generator, spring-pattern-checker | P0, P1 |
| @Transactional 位置錯誤 | smartadmin-manager-extractor | P1 |
| 並發安全審計 | concurrency-safety-auditor | P1 |
| 資料庫查詢慢 | postgresql-best-practices | P1 |
| 安全加固 | security-hardening-pro | P0 |
| Vavr 重構 | vavr-refactoring-assistant | P0 |

### 按 ArchUnit 錯誤選擇

| 錯誤訊息 | 對應技能 |
|----------|----------|
| "Service uses java.util.Optional" | vavr-refactoring-assistant |
| "@Transactional not in Manager layer" | smartadmin-manager-extractor |
| "Controller calls Dao directly" | spring-pattern-checker |
| "Field injection detected" | spring-pattern-checker |

---

## 與 .claude/skills/ 的差異

| 元素 | .claude/ (Claude Code) | .agent/ (Antigravity) |
|------|------------------------|----------------------|
| 技能總數 | 15 (含 React CRUD) | 14 |
| Trigger Keywords | Primary/Secondary 分類 | "When to Use" 條件列表 |
| Phase Execution | `--backend-only` 等 flags | 描述式工作流 |
| Skill References | `[skill:name]` 語法 | 完整路徑引用 |

---

## 版本資訊

| 組件 | 版本 | 狀態 |
|------|------|------|
| Skills Registry | 3.0.0 | ✅ Stable |
| 總技能數 | 14 | ✅ Complete |
| P0 Foundation | 6 | ✅ Complete |
| P1 Extended | 8 | ✅ Complete |

**更新日期**: 2026-03-08

---

## 版本歷史

### 3.0.0 (2026-03-08) - 精簡對齊
- 從 32 → 14 技能（移除磁碟上不存在的幽靈技能引用）
- 移除 P2 Productivity 全部引用（目錄不存在）
- 移除 `extended/orchestration/`、`extended/domain/fraud-detection-pattern-generator/` 等幽靈目錄引用
- 與 skill-registry.yml v3.0.0 完全同步

### 2.0.0 (2026-02-05) - 初始版本
- 從 .claude/skills/ 遷移 32 個技能

---

## 相關文檔

- [skill-registry.yml](skill-registry.yml) - 技能元數據 SSOT
- [.agent/rules/00-INDEX.md](../rules/00-INDEX.md) - 規則路由中心
- [CLAUDE.md](../../CLAUDE.md) - 專案入口點
