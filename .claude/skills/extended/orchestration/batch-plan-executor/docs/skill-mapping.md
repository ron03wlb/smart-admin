## Skill Mapping

### Overview

Batch Plan Executor 自動將方案映射到對應的 Skill。

**映射方式**:
1. **Explicit Mapping**: 方案中明確指定 `skill:` 字段（優先級最高）
2. **Type-Based Mapping**: 基於方案類型自動映射（Claude Code Plans）
3. **Module-Based Mapping**: 基於模組名稱映射（Project Plans）
4. **Content Analysis**: 基於內容關鍵詞推斷（Fallback）

### Type-Based Mapping (Claude Code Plans)

| Plan Type | Skill | Confidence |
|-----------|-------|------------|
| `crud` | smartadmin-crud-generator | 1.0 |
| `testing` | smartadmin-testing-suite | 1.0 |
| `refactoring` | vavr-refactoring-assistant | 0.9 |
| `migration` | (Module-dependent) | 0.7 |
| `integration` | (Content-dependent) | 0.6 |
| `security` | security-hardening-pro | 0.9 |
| `performance` | smartadmin-performance-suite | 0.9 |

### Module-Based Mapping (Project Plans)

| Module | Skill | Confidence |
|--------|-------|------------|
| `liteflow` | liteflow-rule-builder | 1.0 |
| `job` | scheduled-task-manager | 1.0 |
| `kafka` | message-queue-pattern-generator | 1.0 |
| `cache` | cache-strategy-generator | 1.0 |
| `websocket` | websocket-sse-realtime-generator | 1.0 |
| `security` | security-hardening-pro | 1.0 |
| `migration` | db-migration-manager | 1.0 |
| `i18n` | i18n-generator | 1.0 |
| `report` | report-generator-skill | 1.0 |
| `search` | full-text-search-integration | 1.0 |
| `apm` | apm-integration-skill | 1.0 |
| `tenant` | None (Manual) | 0.0 |
| `custom` | None (Manual) | 0.0 |

### Skill Phase Docs (Direct Mapping)

**規則**: Skills Phase Docs 直接映射到父 Skill。

**示例**:
- `.claude/skills/smartadmin-crud-generator/phases/phase-2-frontend.md`
  → Skill: `smartadmin-crud-generator` (Confidence: 1.0)

### Content-Based Mapping (Fallback)

當無法通過類型或模組映射時，使用內容分析。

**關鍵詞匹配**:

```python
keywords_mapping = {
    'smartadmin-crud-generator': ['entity', 'dao', 'controller', 'service', 'crud'],
    'liteflow-rule-builder': ['liteflow', 'rule', 'chain', 'node', 'el expression'],
    'scheduled-task-manager': ['xxl-job', 'snail-job', 'scheduled', 'cron', 'task'],
    'vavr-refactoring-assistant': ['vavr', 'option', 'try', 'either', 'functional'],
    # ... more mappings
}

def content_based_mapping(content: str) -> Tuple[str, float]:
    scores = {}
    for skill, keywords in keywords_mapping.items():
        score = sum(1 for kw in keywords if kw.lower() in content.lower())
        if score > 0:
            scores[skill] = score / len(keywords)  # Normalize to 0.0-1.0

    if scores:
        best_skill = max(scores, key=scores.get)
        confidence = scores[best_skill]
        return best_skill, confidence

    return None, 0.0
```

### Mapping Confidence Scores

| Confidence Range | Interpretation | Action |
|------------------|----------------|--------|
| 1.0 | Exact match | Auto-execute |
| 0.8 - 0.99 | High confidence | Auto-execute |
| 0.7 - 0.79 | Medium confidence | Auto-execute (with warning) |
| 0.5 - 0.69 | Low confidence | Ask user confirmation |
| < 0.5 | Very low confidence | Suggest manual execution |

### Manual Mapping Override

**方法 1: YAML Frontmatter**

```yaml
---
name: custom-feature
type: custom
skill: smartadmin-crud-generator  # Explicit override
---
```

**方法 2: Configuration File**

```yaml
# config.yml
skill_mapping:
  custom_mappings:
    'docs/plans/custom/feature-a.md': 'smartadmin-crud-generator'
    'docs/plans/custom/feature-b.md': 'liteflow-rule-builder'
```

**詳細文檔**: 參考 [references/skill-mapping.md](references/skill-mapping.md)

---

