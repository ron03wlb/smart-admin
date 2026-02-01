## Plan Identification Mechanism

### Discovery Process

Batch Plan Executor 使用多階段方案識別流程。

#### Stage 1: Directory Scanning

```bash
# 默認掃描目錄
~/.claude/plans/           # Claude Code Plans
.claude/skills/*/phases/   # Skills Phase Docs
docs/plans/                # Project Plans
```

**掃描規則**:
- 遞歸掃描（最大深度: 3）
- 僅掃描 `.md` 文件
- 排除 `archive/`、`deprecated/`、`.git/`

#### Stage 2: Plan Type Detection

**檢測優先級**:

1. **Priority 1: File Path Pattern** (最高優先級)
   - 基於文件路徑判斷類型
   - 準確率: 95%+

2. **Priority 2: YAML Frontmatter**
   - 分析 YAML metadata
   - 準確率: 90%+

3. **Priority 3: Content Analysis**
   - 標題格式、關鍵詞分析
   - 準確率: 80%+

**示例**:

```python
# File Path Pattern (Priority 1)
~/.claude/plans/product.md          → CLAUDE_CODE_PLAN
.claude/skills/crud/phases/phase-1.md → SKILL_PHASE_DOC
docs/plans/liteflow/migration.md    → PROJECT_PLAN

# YAML Frontmatter (Priority 2)
---
name: product-crud
type: crud
---
→ CLAUDE_CODE_PLAN

# Content Analysis (Priority 3)
# Phase 2: Frontend Implementation
→ SKILL_PHASE_DOC
```

#### Stage 3: Metadata Extraction

從方案中提取關鍵信息：

| Field | Source | Required |
|-------|--------|----------|
| `name` | YAML frontmatter or filename | ✅ |
| `type` | YAML frontmatter or inferred | ✅ |
| `title` | First H1 heading | ✅ |
| `skill` | YAML or mapped | ⚠ Recommended |
| `dependencies` | YAML frontmatter | ❌ Optional |
| `priority` | YAML frontmatter | ❌ Optional |

#### Stage 4: Validation

驗證方案完整性和正確性：

**驗證規則**:
- ✅ 文件存在且可讀
- ✅ 包含有效的標題（H1 heading）
- ⚠ 有 Skill 映射（警告級別）
- ⚠ 內容長度 > 100 字符（警告級別）

**驗證報告**:

```
╔══════════════════════════════════════════════════════════════════
║ Plan Validation Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans: 8
║ Valid Plans: 7
║ Invalid Plans: 1
╠══════════════════════════════════════════════════════════════════
║ ✗ docs/plans/tenant/multi-tenant.md
║     - ERROR: Missing plan title (H1 heading)
║ ⚠ docs/plans/custom/custom-feature.md
║     - WARNING: No skill mapping found - manual execution required
╚══════════════════════════════════════════════════════════════════
```

---

