# SmartAdmin Documentation Standards

**版本**: 1.0.0
**最後更新**: 2026-02-04
**狀態**: Production

本文檔整合所有 SmartAdmin 文檔標準，提供統一的參考點。

---

## 📋 Quick Navigation

| 標準類別 | 核心規則 | 詳細文檔 |
|---------|---------|---------|
| **Mermaid 語法** | 使用 `<br/>` 非 `\n` | [.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md](.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md) |
| **命名規範** | 實體表名單數、類名駝峰 | [.agent/rules/foundation/01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md) |
| **架構規則** | Controller → Service → Manager → Dao | [.agent/rules/foundation/10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md) |
| **提交訊息** | `<type>(<scope>): <subject>` | [.agent/rules/workflows/17-commit-message-conventions.md](.agent/rules/workflows/17-commit-message-conventions.md) |
| **PostgreSQL 最佳實踐** | HikariCP 配置、N+1 檢測 | [.claude/skills/productivity/integration/postgresql-best-practices/](.claude/skills/productivity/integration/postgresql-best-practices/) |
| **並發安全** | Check-then-act 檢測、Risk rating | [.claude/skills/extended/quality/concurrency-safety-auditor/](.claude/skills/extended/quality/concurrency-safety-auditor/) |
| **異常處理** | ResponseDTO + log.error | [.agent/rules/technology/patterns/04-exception-logging.md](.agent/rules/technology/patterns/04-exception-logging.md) |
| **質量工具** | PMD, SpotBugs, Spotless | [.agent/rules/quality-tools/](.agent/rules/quality-tools/) |
| **文檔治理** | File size limits, Link validation | 本文檔 §3 |

---

## 1. Mermaid 圖表標準

### 換行符規則
❌ **禁止**: `\n` escape sequences
✅ **使用**: HTML `<br/>` tags

**原因**: SmartAdmin rendering environment requires HTML tags

**範例**:
```mermaid
graph TD
    A[Line 1<br/>Line 2]  ✅ 正確
    B["Line 1\nLine 2"]   ❌ 錯誤
```

**Pre-commit Hook**: Automatically validates Mermaid syntax before commit.

**詳細規範**: [.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md](.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md)

---

## 2. 命名規範

### 資料庫命名
- **表名**: 單數形式（`employee` NOT `employees`）
- **列名**: Snake case（`created_time` NOT `createdTime`）
- **Boolean 欄位**: `deleted` NOT `isDeleted`

### Java 類命名
- **Controller**: `{Entity}Controller` (e.g., `EmployeeController`)
- **Service**: `{Entity}Service` (e.g., `EmployeeService`)
- **Manager**: `{Entity}Manager` (e.g., `EmployeeManager`)
- **Dao**: `{Entity}Dao` (e.g., `EmployeeDao`)

### 文檔命名（iGaming 專用）
- **格式**: `XX-YY_{描述}.md`
- **範例**: `01-02_Wallet_Architecture.md`

**詳細規範**: [.agent/rules/foundation/01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md)

---

## 3. 文檔治理標準

### 文件大小限制
- **單檔最大**: 2500 lines
- **允許超大文件**: ≤5 個
- **檢測工具**: `scripts/check_file_size.{sh,ps1}`
- **CI/CD**: `.github/workflows/file-size-check.yml`

### 鏈接驗證
- **檢測工具**: `scripts/validate_links.{sh,ps1}`
- **CI/CD**: `.github/workflows/link-validation.yml`（Weekly schedule）
- **斷裂鏈接容忍度**: 0

### SSOT（Single Source of Truth）
- **概念定義**: 每個核心概念僅有一個權威定義
- **引用格式**: `<!-- SSOT: 01-02 §2.3 -->`
- **檢測工具**: `scripts/detect_ssot_violations.sh`

**詳細規範**: [docs/iGaming/SSOT_VALIDATION_REPORT.md](docs/iGaming/SSOT_VALIDATION_REPORT.md)

---

## 4. 架構規範

### 分層架構
```
Controller → Service → Manager → Dao → Entity
              ↓         ↓
            (Can call Dao directly for single-table CRUD)
            (Delegate to Manager when @Transactional needed)
```

### 關鍵規則
- ✅ Controller → Service ONLY (Controller CANNOT call Dao/Manager)
- ✅ Service → Dao ALLOWED (single-table CRUD without @Transactional)
- ✅ Service → Manager REQUIRED (when @Transactional or @Cacheable needed)
- ✅ `@Transactional` / `@Cacheable`: Manager layer ONLY
- ❌ `@Autowired` field injection: FORBIDDEN (use constructor injection)

**詳細規範**: [.agent/rules/foundation/10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md)

---

## 5. 質量工具標準

### PMD Suppressions
- `CallSuperInConstructor`: Empty constructors acceptable
- `AvoidReassigningParameters`: Create local copy
- `ShortClassName`: Use `@SuppressWarnings` for inner classes

### SpotBugs Exclusions
- `EI_EXPOSE_REP/EI_EXPOSE_REP2`: DTO/VO/Form/Config classes
- `NP_NULL_ON_SOME_PATH`: CompletableFuture.getNow(null) valid
- `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD`: @PostConstruct pattern

**詳細規範**:
- [.agent/rules/quality-tools/12-pmd-rules.md](.agent/rules/quality-tools/12-pmd-rules.md)
- [.agent/rules/quality-tools/13-spotbugs-rules.md](.agent/rules/quality-tools/13-spotbugs-rules.md)

---

## 6. Commit Message Conventions

### 格式
```
<type>(<scope>): <subject>

<body>

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code restructuring
- `docs`: Documentation only
- `test`: Adding tests
- `chore`: Maintenance

**詳細規範**: [.agent/rules/workflows/17-commit-message-conventions.md](.agent/rules/workflows/17-commit-message-conventions.md)

---

## 7. FAQ

### Q: 為什麼 Mermaid 必須使用 `<br/>` 而非 `\n`？
A: SmartAdmin 的渲染環境（Markdown-it + Mermaid plugin）不支持 `\n` escape sequences，必須使用 HTML tags。

### Q: 如何快速修復所有 Mermaid `\n` 問題？
A: 運行 `python scripts/fix_all_mermaid_newlines.py` 自動替換所有文件。

### Q: 文件超過 2500 行怎麼辦？
A: 拆分為多個邏輯章節，每個章節獨立成文件。參見 Week 6 Plan 的 Gateway 拆分範例 (1,131 lines → 3 files)。

### Q: 如何檢查 SSOT 違規？
A: 運行 `bash scripts/detect_ssot_violations.sh`，檢查概念是否重複定義 >3 次。

### Q: 為什麼 Service 層必須使用 Vavr Option 而非 Java Optional？
A: Vavr Option 提供更豐富的 functional programming API，且 SmartAdmin ArchUnit 測試強制執行此標準。

---

## 📚 完整規範索引

### Foundation Rules (.agent/rules/foundation/)
- [01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md)
- [09-manager-layer.md](.agent/rules/foundation/09-manager-layer.md)
- [10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md)

### Technology Patterns (.agent/rules/technology/patterns/)
- [04-exception-logging.md](.agent/rules/technology/patterns/04-exception-logging.md)
- [05-transaction-management.md](.agent/rules/technology/patterns/05-transaction-management.md)

### Quality Tools (.agent/rules/quality-tools/)
- [12-pmd-rules.md](.agent/rules/quality-tools/12-pmd-rules.md)
- [13-spotbugs-rules.md](.agent/rules/quality-tools/13-spotbugs-rules.md)
- [14-spotless-rules.md](.agent/rules/quality-tools/14-spotless-rules.md)

### Skills (.claude/skills/)
- [concurrency-safety-auditor/](.claude/skills/extended/quality/concurrency-safety-auditor/)
- [postgresql-best-practices/](.claude/skills/productivity/integration/postgresql-best-practices/)
- [igaming-multi-tenant-wallet-pm/](.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/)

---

**Maintained by**: Architecture Team
**Last Review**: 2026-02-04
**Next Review**: 2026-05-01
