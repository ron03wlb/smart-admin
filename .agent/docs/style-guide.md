# .agent/ 文檔樣式指南

## 檔案命名規範

### Rules 檔案
**格式**：`{類別代碼}{兩位數序號}-{描述性名稱}.md`
- ✅ `F01-naming-conventions.md`
- ✅ `D01-postgresql-basics.md`
- ❌ `05-postgresql.md`（舊格式，已棄用）

**類別代碼**：
- `F` = Foundation（基礎架構）
- `D` = Database（資料庫技術）
- `P` = Patterns（設計模式）
- `S` = Security（安全規則）
- `Q` = Quality Tools（品質工具配置）
- `W` = Workflows（工作流程指南）

**範例**：
```
foundation/
├── F01-naming-conventions.md
├── F02-oop-principles.md
├── F03-manager-layer.md
└── F04-architecture-rules.md
```

### Workflows 檔案
**格式**：`{兩位數序號}-{描述性名稱}.md` 或 `{描述性名稱}.md`
- ✅ `01-environment-setup.md`
- ✅ `tdd-workflow.md`（描述性命名也可接受）
- ❌ `init.md`（太簡短，已棄用）

**特殊檔案**：
- `00-workflow-index.md` - 工作流程索引（入口點）

### Skills 目錄
**格式**：`{描述性名稱}/`（使用連字符）
- ✅ `smartadmin-crud-generator/`
- ✅ `quality-gate-orchestrator/`
- ❌ `SmartAdminCrudGenerator/`（不使用駝峰）

**結構**：
```
skills/{skill-name}/
├── SKILL.md          (必須)
├── examples/         (可選)
└── templates/        (可選)
```

### Docs 檔案
**格式**：`{描述性名稱}-{類型}.md`
- ✅ `coding-standards-summary.md`
- ✅ `faq-troubleshooting.md`
- ✅ `translation-glossary.md`
- ✅ `style-guide.md`（本檔案）

### Configs 檔案
**格式**：遵循工具的標準命名
- ✅ `checkstyle.xml`
- ✅ `pmd-ruleset.xml`
- ✅ `sonar-project.properties`
- ✅ `ArchitectureTest.java`

---

## 連結格式規範

### 相對路徑（優先）
**規則**：在 `.agent/` 系統內部，統一使用相對路徑。

**優點**：
- 可移植性高（目錄重命名不影響）
- 檔案關係清晰
- 符合 Markdown 最佳實踐

**範例**：
```markdown
<!-- 在 .agent/rules/00-INDEX.md 中 -->
[Naming Conventions](foundation/F01-naming-conventions.md)

<!-- 在 .agent/skills/smartadmin-crud-generator/SKILL.md 中 -->
[Architecture Rules](../../rules/foundation/F04-architecture-rules.md)

<!-- 在 .agent/workflows/01-environment-setup.md 中 -->
[ArchUnit Test](../configs/ArchitectureTest.java)
```

### 絕對路徑（跨系統）
**規則**：從專案根目錄引用時，使用絕對路徑。

**用途**：
- 跨系統引用（如 CLAUDE.md 引用 .agent/ 或 .claude/）
- 專案根目錄的文檔引用子目錄

**範例**：
```markdown
<!-- 在 CLAUDE.md 中 -->
[Architecture Rules](.agent/rules/foundation/F04-architecture-rules.md)
[SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)

<!-- 在 README.md 中 -->
[Quick Start](.agent/workflows/01-environment-setup.md)
```

### 錨點連結
**格式**：`#section-name`（使用連字符，小寫）

**範例**：
```markdown
[ResponseDTO Pattern](#responsedto-pattern)
[Service Layer Architecture](#service-layer-architecture)

<!-- 跨檔案錨點 -->
[Exception Handling](../patterns/P05-exception-logging.md#try-catch-patterns)
```

### 特殊情況

**同層級檔案**：
```markdown
<!-- 在 foundation/F01-naming-conventions.md 中引用同層級 -->
[OOP Principles](./F02-oop-principles.md)
或
[OOP Principles](F02-oop-principles.md)
```

**父目錄**：
```markdown
<!-- 在 technology/database/D01-postgresql-basics.md 中引用 foundation/ -->
[Naming Conventions](../../foundation/F01-naming-conventions.md)
```

**跨類別引用**：
```markdown
<!-- 在 technology/patterns/P05-exception-logging.md 中引用 database/ -->
[PostgreSQL Basics](../database/D01-postgresql-basics.md)
```

---

## Markdown 格式標準

### 標題層級
- `#` - 檔案標題（僅一個）
- `##` - 主要章節
- `###` - 子章節
- `####` - 詳細內容

**範例**：
```markdown
# F01-naming-conventions.md

## 命名約定總覽

### 類別命名
#### Controller 命名規則
#### Service 命名規則
```

### 程式碼區塊
**使用語言標記**：
````markdown
```java
public class UserService {
    // Java code
}
```

```bash
./gradlew build
```

```yaml
name: example
version: 1.0.0
```
````

### 表格
**對齊**：使用 `|` 對齊列
```markdown
| 欄位 | 說明 | 範例 |
|------|------|------|
| Name | 名稱 | Foo  |
| Type | 類型 | String |
```

### 清單
- 使用 `-` 而非 `*`
- 巢狀清單縮排2個空格
- 有序清單使用 `1.`, `2.`, `3.`

**範例**：
```markdown
- 主項目
  - 子項目1
  - 子項目2
- 主項目2

1. 第一步
2. 第二步
   - 注意事項
3. 第三步
```

### 強調
- **粗體**：`**text**`
- *斜體*：`*text*`
- `程式碼`：`` `code` ``
- ~~刪除線~~：`~~text~~`

### 引用區塊
```markdown
> **重要**：這是一個重要提示。

> **Reference**: 參考文檔連結
```

### 水平分隔線
使用 `---`（三個連字符）

---

## YAML Frontmatter

### Rules 檔案
```yaml
---
trigger: always_on
description: Short description of the rule
tags: [tag1, tag2, tag3]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - foundation/F01-naming-conventions.md
  - technology/database/D04-mybatis-plus-core.md
archunit_test: ArchitectureTest#methodName
last_updated: 2026-02-04
---
```

**欄位說明**：
- `trigger`: 觸發時機（always_on, on_demand, on_test_failure）
- `positioning`: current-standard, ideal, deprecated
- `ai_role`: code_reviewer_and_generator, test_generator, orchestrator
- `auto_apply`: 是否自動應用（true/false）
- `ask_before_fix`: 修復前是否詢問（true/false）

### Skills 檔案
```yaml
---
name: Skill Name
description: Short description
tags: [skill-type, domain]
version: 1.0.0
related_rules:
  - foundation/F04-architecture-rules.md
last_updated: 2026-02-04
---
```

### Workflows 檔案
```yaml
---
trigger: on_demand
description: Workflow description
tags: [workflow, initialization]
prerequisites:
  - Docker installed
  - Java 21 installed
related_rules:
  - technology/database/D01-postgresql-basics.md
last_updated: 2026-02-04
---
```

---

## 檔案大小限制

- **最大行數**：500行（建議）
- **最大字元數**：50,000字元
- **超過限制**：拆分為多個檔案

**拆分策略**：
```
# 原檔案：05-postgresql.md（過大）
# 拆分為：
├── D01-postgresql-basics.md
├── D02-postgresql-advanced.md
└── D03-postgresql-mybatis.md
```

---

## 驗證工具

### 連結檢查
```bash
# 使用 markdown-link-check
npx markdown-link-check .agent/**/*.md

# 檢查特定檔案
npx markdown-link-check .agent/rules/00-INDEX.md
```

### 命名檢查
```bash
# 檢查是否符合命名規範
cd .agent/rules
find foundation technology security quality-tools workflows -name "*.md" | \
  grep -vE "^[FDPSQW][0-9]{2}-|00-INDEX|ARCHITECTURE-RULES-CLARIFICATION"

# 應該無輸出（所有檔案符合規範）
```

### 格式檢查
```bash
# 使用 markdownlint
npx markdownlint .agent/**/*.md

# 使用配置檔案
npx markdownlint --config .agent/.markdownlint.json .agent/**/*.md
```

---

## 命名約定範例

### ✅ 正確範例

**Rules**：
```
foundation/F01-naming-conventions.md
technology/database/D04-mybatis-plus-core.md
technology/patterns/P05-exception-logging.md
security/S01-owasp-top10-part1.md
quality-tools/Q06-jacoco-coverage-rules.md
workflows/W02-commit-message-conventions.md
```

**Skills**：
```
smartadmin-crud-generator/
quality-gate-orchestrator/
smartadmin-testing-suite/
```

**Workflows**：
```
00-workflow-index.md
01-environment-setup.md
tdd-workflow.md
quality-gates-local-ci.md
```

**Docs**：
```
coding-standards-summary.md
faq-troubleshooting.md
translation-glossary.md
style-guide.md
```

### ❌ 錯誤範例

**不符合新規範**：
```
foundation/01-naming-conventions.md  (舊格式)
database/05-postgresql.md           (舊格式)
quality-tools/11-checkstyle.md      (舊格式)
```

**命名不清晰**：
```
init.md                             (太簡短)
doc.md                              (太模糊)
test.md                             (太通用)
```

**大小寫錯誤**：
```
SmartAdminCrudGenerator/            (應使用連字符)
Quality-Gate-Orchestrator/          (多餘的大寫)
```

---

## 維護指南

### 新增檔案檢查清單

建立新規則檔案時：
- [ ] 檔名符合 `{類別代碼}{兩位數}-{描述性名稱}.md` 格式
- [ ] YAML frontmatter 完整（trigger, description, tags, positioning）
- [ ] `related_rules` 連結使用相對路徑
- [ ] 連結格式正確（相對路徑）
- [ ] 程式碼區塊使用語言標記
- [ ] 表格對齊整齊
- [ ] 檔案大小不超過500行

建立新技能時：
- [ ] 目錄名稱使用連字符（`skill-name/`）
- [ ] 包含 `SKILL.md` 主檔案
- [ ] YAML frontmatter 完整
- [ ] 連結到相關規則（使用相對路徑）
- [ ] 包含使用範例

### 更新檔案檢查清單

修改規則檔案時：
- [ ] 更新 `last_updated` 日期
- [ ] 檢查連結是否仍然有效
- [ ] 確認 `related_rules` 仍然相關
- [ ] 執行 markdown-link-check 驗證

### 重命名檔案流程

1. **使用 git mv 保留歷史**：
   ```bash
   git mv old-name.md new-name.md
   ```

2. **更新所有引用**：
   ```bash
   # 使用 sed 批次替換
   find .agent -name "*.md" -exec sed -i '' 's|old-name\.md|new-name.md|g' {} +
   ```

3. **驗證引用完整性**：
   ```bash
   grep -r "old-name" .agent/
   # 應該無輸出或僅剩歷史記錄
   ```

4. **更新索引檔案**：
   - `00-INDEX.md`（如果是規則檔案）
   - `00-workflow-index.md`（如果是工作流程）
   - `README.md`（如果是重要檔案）

---

## 相關文檔

- [.agent/README.md](../README.md) - .agent/ 系統總覽
- [.agent/rules/00-INDEX.md](../rules/00-INDEX.md) - 規則索引
- [.agent/docs/README.md](README.md) - 快速參考文檔說明
- [.agent/configs/README.md](../configs/README.md) - 配置檔案指南
