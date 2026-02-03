# Skill Mapping Reference

**Version**: v1.0.0
**Last Updated**: 2026-01-29
**Status**: ✅ Production Ready

---

## Overview

此文檔定義了 Batch Plan Executor 的完整 Skill 映射規則，涵蓋三種方案類型的映射策略、信心分數計算、以及手動映射配置。

### Mapping Strategies

Batch Plan Executor 使用分層映射策略：

1. **Explicit Mapping** (Priority 1): 方案中明確指定 `skill:` 字段
2. **Type-Based Mapping** (Priority 2): 基於方案類型自動映射（Claude Code Plans）
3. **Module-Based Mapping** (Priority 3): 基於模組名稱映射（Project Plans）
4. **Path-Based Mapping** (Priority 4): 基於文件路徑直接映射（Skills Phase Docs）
5. **Content Analysis** (Priority 5): 基於內容關鍵詞推斷（Fallback）

---

## Type 1: Claude Code Plans Mapping

### Overview

Claude Code Plans 基於 `type` 字段進行映射。

**路徑**: `~/.claude/plans/*.md`

**映射算法**:

```python
def map_claude_code_plan(plan: ClaudeCodePlan) -> Tuple[str, float]:
    """
    Map Claude Code Plan to skill based on type field.

    Returns:
        (skill_name, confidence_score)
    """
    # Priority 1: Explicit skill in frontmatter
    if plan.metadata and 'skill' in plan.metadata:
        return plan.metadata['skill'], 1.0

    # Priority 2: Type-based mapping
    plan_type = plan.metadata.get('type', 'custom')
    skill = TYPE_SKILL_MAPPINGS.get(plan_type)

    if skill:
        return skill, 1.0

    # Priority 3: Content-based fallback
    return content_based_mapping(plan.content)
```

### Type-to-Skill Mappings

| Plan Type | Skill | Confidence | Use Case |
|-----------|-------|------------|----------|
| `crud` | smartadmin-crud-generator | 1.0 | Complete CRUD module generation |
| `testing` | smartadmin-testing-suite | 1.0 | Integration test generation |
| `test-fixture` | test-fixture-generator | 1.0 | Test data builder generation |
| `refactoring` | vavr-refactoring-assistant | 0.9 | Vavr Option/Try/Either migration |
| `migration` | (Content-dependent) | 0.7 | Technology migration (requires analysis) |
| `integration` | (Content-dependent) | 0.6 | API/third-party integration |
| `security` | security-hardening-pro | 0.9 | Security hardening, encryption, masking |
| `performance` | smartadmin-performance-suite | 0.9 | Performance optimization, caching |
| `quality-gate` | quality-gate-orchestrator | 1.0 | Multi-tool quality checks |
| `archunit` | archunit-test-generator | 1.0 | Architecture test generation |
| `fraud-detection` | fraud-detection-pattern-generator | 1.0 | iGaming fraud detection, KYC/AML |
| `custom` | None (Manual) | 0.0 | Requires user selection |

### Migration Type Sub-Mapping

當 `type: migration` 時，需要進一步分析內容以確定具體 Skill。

| Migration Keyword | Skill | Confidence |
|-------------------|-------|------------|
| `liteflow`, `rule engine` | liteflow-rule-builder | 0.9 |
| `kafka`, `message queue` | message-queue-pattern-generator | 0.9 |
| `database`, `flyway`, `liquibase` | db-migration-manager | 0.9 |
| `xxl-job`, `snail-job`, `scheduler` | scheduled-task-manager | 0.9 |
| (No specific keywords) | None (Manual) | 0.5 |

**Algorithm**:

```python
def map_migration_type(content: str) -> Tuple[str, float]:
    """
    Map migration type based on content analysis.
    """
    migration_keywords = {
        'liteflow-rule-builder': ['liteflow', 'rule engine', 'el expression', 'chain'],
        'message-queue-pattern-generator': ['kafka', 'rocketmq', 'message queue', 'event-driven'],
        'db-migration-manager': ['flyway', 'liquibase', 'database migration', 'schema change'],
        'scheduled-task-manager': ['xxl-job', 'snail-job', 'scheduled task', 'cron'],
    }

    content_lower = content.lower()
    scores = {}

    for skill, keywords in migration_keywords.items():
        score = sum(1 for kw in keywords if kw in content_lower)
        if score > 0:
            scores[skill] = score / len(keywords)

    if scores:
        best_skill = max(scores, key=scores.get)
        confidence = scores[best_skill]
        return best_skill, min(confidence + 0.5, 0.9)  # Boost confidence but cap at 0.9

    return None, 0.5  # Requires manual selection
```

### Integration Type Sub-Mapping

當 `type: integration` 時，分析內容以確定集成類型。

| Integration Keyword | Skill | Confidence |
|---------------------|-------|------------|
| `elasticsearch`, `search` | full-text-search-integration | 0.9 |
| `websocket`, `sse`, `real-time` | websocket-sse-realtime-generator | 0.9 |
| `minio`, `s3`, `object storage` | (Future Skill) | 0.7 |
| `i18n`, `internationalization` | i18n-generator | 0.9 |
| `report`, `export`, `excel`, `pdf` | report-generator | 0.9 |
| (No specific keywords) | None (Manual) | 0.5 |

---

## Type 2: Skills Phase Docs Mapping

### Overview

Skills Phase Docs 直接映射到父 Skill（基於文件路徑）。

**路徑**: `.claude/skills/{skill-name}/phases/phase-*.md`

**映射算法**:

```python
def map_skill_phase_doc(plan: SkillPhaseDoc) -> Tuple[str, float]:
    """
    Map Skills Phase Doc to parent skill.

    Direct mapping with 100% confidence.
    """
    # Extract skill name from path
    skill_name = extract_skill_name_from_path(plan.file_path)
    return skill_name, 1.0
```

### Path Extraction

```python
import re

def extract_skill_name_from_path(file_path: str) -> str:
    """
    Extract skill name from file path.

    Example:
        .claude/skills/smartadmin-crud-generator/phases/phase-2-frontend.md
        -> smartadmin-crud-generator
    """
    match = re.search(r'\.claude/skills/([^/]+)/phases/', file_path)
    if match:
        return match.group(1)

    raise ValueError(f"Cannot extract skill name from path: {file_path}")
```

### Supported Skills (Phase Docs)

| Skill | Phase Docs Supported | Example Path |
|-------|---------------------|--------------|
| smartadmin-crud-generator | ✅ Yes | `.claude/skills/smartadmin-crud-generator/phases/phase-2-frontend.md` |
| smartadmin-testing-suite | ✅ Yes | `.claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md` |
| liteflow-rule-builder | ✅ Yes | `.claude/skills/liteflow-rule-builder/phases/phase-1-setup.md` |
| scheduled-task-manager | ✅ Yes | `.claude/skills/scheduled-task-manager/phases/phase-1-xxl-job-integration.md` |
| security-hardening-pro | ✅ Yes | `.claude/skills/security-hardening-pro/phases/phase-1-encryption.md` |
| vavr-refactoring-assistant | ⚠ Limited | `.claude/skills/vavr-refactoring-assistant/phases/phase-1-service-layer.md` |
| (All other skills) | ✅ Yes | (Follow same pattern) |

---

## Type 3: Project Plans Mapping

### Overview

Project Plans 基於 `module` 字段進行映射。

**路徑**: `docs/plans/{module}/*.md`

**映射算法**:

```python
def map_project_plan(plan: ProjectPlan) -> Tuple[str, float]:
    """
    Map Project Plan to skill based on module field.

    Returns:
        (skill_name, confidence_score)
    """
    # Priority 1: Explicit skill in frontmatter
    if plan.metadata and 'skill' in plan.metadata:
        return plan.metadata['skill'], 1.0

    # Priority 2: Module-based mapping
    module = plan.module.lower()
    skill = MODULE_SKILL_MAPPINGS.get(module)

    if skill:
        return skill, 1.0

    # Priority 3: Content-based fallback
    return content_based_mapping(plan.content)
```

### Module-to-Skill Mappings

| Module | Skill | Confidence | Use Case |
|--------|-------|------------|----------|
| `liteflow` | liteflow-rule-builder | 1.0 | LiteFlow rule engine setup, migration, DSL generation |
| `job` | scheduled-task-manager | 1.0 | XXL-Job, Snail-Job integration, scheduled tasks |
| `kafka` | message-queue-pattern-generator | 1.0 | Kafka, RocketMQ integration, event-driven patterns |
| `cache` | cache-strategy-generator | 1.0 | Multi-level caching (Caffeine L1 + Redis L2) |
| `websocket` | websocket-sse-realtime-generator | 1.0 | WebSocket, SSE real-time communication |
| `security` | security-hardening-pro | 1.0 | SM2/SM3/SM4 encryption, data masking, audit logging |
| `migration` | db-migration-manager | 1.0 | Flyway, Liquibase database migration |
| `database` | db-migration-manager | 1.0 | Database schema changes, migrations |
| `i18n` | i18n-generator | 1.0 | Internationalization, multi-language support |
| `report` | report-generator | 1.0 | Excel, PDF, CSV report generation |
| `search` | full-text-search-integration | 1.0 | Elasticsearch integration, full-text search |
| `elasticsearch` | full-text-search-integration | 1.0 | Elasticsearch setup, indexing, search APIs |
| `apm` | apm-integration | 1.0 | Skywalking, Micrometer, Grafana monitoring |
| `monitoring` | apm-integration | 1.0 | APM, performance monitoring |
| `performance` | smartadmin-performance-suite | 1.0 | Performance optimization, profiling |
| `testing` | smartadmin-testing-suite | 1.0 | Integration tests, E2E tests |
| `quality` | quality-gate-orchestrator | 1.0 | Checkstyle, PMD, SpotBugs, ArchUnit |
| `archunit` | archunit-test-generator | 1.0 | Architecture test generation |
| `fraud` | fraud-detection-pattern-generator | 1.0 | iGaming fraud detection, risk control |
| `igaming` | igame-feature-builder | 1.0 | iGaming domain features (VIP, Wallet, Bonus) |
| `cicd` | cicd-pipeline-builder | 1.0 | GitHub Actions, GitLab CI pipeline setup |
| `tenant` | None (Manual) | 0.0 | Multi-tenant architecture (no dedicated skill) |
| `custom` | None (Manual) | 0.0 | Custom features requiring manual execution |

### Module Aliases

某些模組有多個別名，都映射到同一個 Skill。

| Aliases | Canonical Module | Skill |
|---------|------------------|-------|
| `xxl-job`, `snail-job`, `scheduler` | `job` | scheduled-task-manager |
| `rocketmq`, `message-queue`, `mq` | `kafka` | message-queue-pattern-generator |
| `flyway`, `liquibase`, `db-migration` | `migration` | db-migration-manager |
| `redis`, `caffeine`, `caching` | `cache` | cache-strategy-generator |
| `sse`, `real-time`, `push` | `websocket` | websocket-sse-realtime-generator |
| `encryption`, `masking`, `audit` | `security` | security-hardening-pro |
| `excel`, `pdf`, `csv`, `export` | `report` | report-generator |
| `elk`, `full-text`, `lucene` | `search` | full-text-search-integration |
| `skywalking`, `micrometer`, `grafana` | `apm` | apm-integration |
| `checkstyle`, `pmd`, `spotbugs` | `quality` | quality-gate-orchestrator |
| `kyc`, `aml`, `risk` | `fraud` | fraud-detection-pattern-generator |
| `github-actions`, `gitlab-ci`, `pipeline` | `cicd` | cicd-pipeline-builder |

**Algorithm**:

```python
MODULE_ALIASES = {
    'xxl-job': 'job',
    'snail-job': 'job',
    'scheduler': 'job',
    'rocketmq': 'kafka',
    'message-queue': 'kafka',
    'mq': 'kafka',
    # ... more aliases
}

def normalize_module(module: str) -> str:
    """
    Normalize module name by resolving aliases.
    """
    module_lower = module.lower()
    return MODULE_ALIASES.get(module_lower, module_lower)
```

---

## Content-Based Mapping (Fallback)

### Overview

當無法通過類型或模組映射時，使用內容關鍵詞分析。

**觸發條件**:
- Claude Code Plan 的 `type: custom`
- Project Plan 的 `module: custom`
- 無 YAML frontmatter

**信心分數**: 0.5-0.8（取決於關鍵詞匹配度）

### Keyword Mappings

```python
CONTENT_KEYWORDS = {
    'smartadmin-crud-generator': [
        'entity', 'dao', 'controller', 'service', 'crud',
        'mapper', 'form', 'vo', 'query', 'create', 'update', 'delete'
    ],
    'liteflow-rule-builder': [
        'liteflow', 'rule', 'chain', 'node', 'el expression',
        'qlexpress', 'workflow', 'orchestration', 'evrete'
    ],
    'scheduled-task-manager': [
        'xxl-job', 'snail-job', 'scheduled', 'cron', 'task',
        'job handler', 'periodic', 'batch processing'
    ],
    'vavr-refactoring-assistant': [
        'vavr', 'option', 'try', 'either', 'functional',
        'optional', 'refactor', 'functional programming'
    ],
    'security-hardening-pro': [
        'security', 'encryption', 'sm2', 'sm3', 'sm4',
        'masking', 'pii', 'audit log', 'xss', 'csrf', 'sql injection'
    ],
    'smartadmin-testing-suite': [
        'test', 'junit', 'testcontainers', 'integration test',
        'e2e', 'mock', 'fixture', 'test data'
    ],
    'test-fixture-generator': [
        'test fixture', 'test data builder', 'builder pattern',
        'test data', 'faker', 'random data'
    ],
    'message-queue-pattern-generator': [
        'kafka', 'rocketmq', 'event-driven', 'message queue',
        'producer', 'consumer', 'cqrs', 'event sourcing', 'saga'
    ],
    'cache-strategy-generator': [
        'cache', 'caffeine', 'redis', 'caching',
        'cache-aside', 'write-through', 'write-behind', 'invalidation'
    ],
    'websocket-sse-realtime-generator': [
        'websocket', 'sse', 'server-sent events', 'real-time',
        'stomp', 'push notification', 'live update'
    ],
    'db-migration-manager': [
        'flyway', 'liquibase', 'database migration', 'schema',
        'ddl', 'alter table', 'create table', 'version control'
    ],
    'i18n-generator': [
        'i18n', 'internationalization', 'multi-language', 'locale',
        'translation', 'message source', 'rtl'
    ],
    'report-generator': [
        'report', 'export', 'excel', 'pdf', 'csv',
        'poi', 'itext', 'easyexcel', 'scheduled report'
    ],
    'full-text-search-integration': [
        'elasticsearch', 'search', 'full-text', 'lucene',
        'indexing', 'aggregation', 'elk', 'kibana'
    ],
    'apm-integration': [
        'apm', 'skywalking', 'micrometer', 'grafana',
        'monitoring', 'tracing', 'metrics', 'prometheus'
    ],
    'smartadmin-performance-suite': [
        'performance', 'optimize', 'n+1', 'slow query',
        'profiling', 'jvm tuning', 'memory leak', 'cpu'
    ],
    'quality-gate-orchestrator': [
        'quality gate', 'checkstyle', 'pmd', 'spotbugs',
        'code quality', 'static analysis', 'archunit'
    ],
    'archunit-test-generator': [
        'archunit', 'architecture test', 'layer dependency',
        'package structure', 'naming convention'
    ],
    'fraud-detection-pattern-generator': [
        'fraud detection', 'kyc', 'aml', 'risk control',
        'suspicious activity', 'igaming', 'compliance'
    ],
    'igame-feature-builder': [
        'igaming', 'vip', 'wallet', 'bonus', 'rake',
        'player', 'turnover', 'wagering', 'payout'
    ],
    'cicd-pipeline-builder': [
        'ci/cd', 'github actions', 'gitlab ci', 'pipeline',
        'deployment', 'automation', 'quality gate', 'integration'
    ],
}
```

### Matching Algorithm

```python
def content_based_mapping(content: str) -> Tuple[str, float]:
    """
    Map plan to skill based on content keyword analysis.

    Returns:
        (skill_name, confidence_score) or (None, 0.0)
    """
    content_lower = content.lower()
    scores = {}

    for skill, keywords in CONTENT_KEYWORDS.items():
        # Count keyword matches
        matches = sum(1 for kw in keywords if kw in content_lower)

        if matches > 0:
            # Normalize score (0.0-1.0)
            score = matches / len(keywords)
            scores[skill] = score

    if not scores:
        return None, 0.0

    # Find best matching skill
    best_skill = max(scores, key=scores.get)
    raw_score = scores[best_skill]

    # Calculate confidence (cap at 0.8 for content-based)
    confidence = min(raw_score * 1.5, 0.8)

    return best_skill, confidence
```

### Confidence Score Calculation

| Raw Score Range | Confidence Score | Interpretation |
|-----------------|------------------|----------------|
| 0.00 - 0.10 | 0.00 - 0.15 | Very low confidence |
| 0.10 - 0.20 | 0.15 - 0.30 | Low confidence |
| 0.20 - 0.40 | 0.30 - 0.60 | Medium confidence |
| 0.40 - 0.60 | 0.60 - 0.80 | High confidence (capped) |
| 0.60+ | 0.80 | High confidence (capped) |

**Note**: Content-based mapping confidence is capped at 0.8 to indicate uncertainty.

---

## Confidence Thresholds

### Default Thresholds

```yaml
# config.yml
skill_mapping:
  auto_mapping:
    confidence_threshold: 0.7  # Minimum confidence for auto-execution
```

| Confidence Range | Action | User Interaction |
|------------------|--------|------------------|
| 1.0 | Auto-execute | None |
| 0.8 - 0.99 | Auto-execute | None |
| 0.7 - 0.79 | Auto-execute | Warning logged |
| 0.5 - 0.69 | Ask user confirmation | Interactive prompt |
| < 0.5 | Suggest manual execution | Mark as "Manual Required" |

### Threshold Configuration

**Strict Mode** (High confidence required):

```yaml
skill_mapping:
  auto_mapping:
    confidence_threshold: 0.8
```

**Permissive Mode** (Lower confidence accepted):

```yaml
skill_mapping:
  auto_mapping:
    confidence_threshold: 0.5
```

---

## Manual Mapping Override

### Method 1: YAML Frontmatter

**最推薦的方法**: 在方案中明確指定 `skill:` 字段。

**Example**:

```yaml
---
name: custom-feature
type: custom
skill: smartadmin-crud-generator  # Explicit override
---

# Custom Feature Implementation Plan
...
```

**Confidence**: 1.0 (Explicit mapping always has highest confidence)

### Method 2: Configuration File

**適用於**: 批量配置多個方案的映射規則。

**Example**:

```yaml
# .claude/skills/batch-plan-executor/config.yml
skill_mapping:
  custom_mappings:
    # File path -> Skill name
    'docs/plans/custom/feature-a.md': 'smartadmin-crud-generator'
    'docs/plans/custom/feature-b.md': 'liteflow-rule-builder'

    # Wildcard patterns
    'docs/plans/custom/*.md': 'smartadmin-crud-generator'
    'docs/plans/experimental/**/*.md': 'vavr-refactoring-assistant'
```

**Priority**: Custom mappings override all other mapping strategies.

### Method 3: Interactive Selection

**適用於**: Dry-run 或 Interactive 模式下臨時選擇 Skill。

**Example**:

```
╔══════════════════════════════════════════════════════════════════
║ Skill Mapping Required
╠══════════════════════════════════════════════════════════════════
║ Plan: custom-feature
║ Type: custom
║ Module: custom
║ Content-based confidence: 0.45 (LOW)
╠══════════════════════════════════════════════════════════════════
║ Available Skills:
║   1. smartadmin-crud-generator
║   2. smartadmin-testing-suite
║   3. liteflow-rule-builder
║   4. vavr-refactoring-assistant
║   5. security-hardening-pro
║   6. (More...)
║   M. Manual execution (no skill)
╠══════════════════════════════════════════════════════════════════
║ Select skill (1-6, M): _
╚══════════════════════════════════════════════════════════════════
```

---

## Mapping Validation

### Validation Process

```python
def validate_skill_mapping(plan: Plan) -> ValidationResult:
    """
    Validate that plan can be mapped to a valid skill.

    Returns:
        ValidationResult with status and warnings
    """
    skill, confidence = map_plan_to_skill(plan)

    # Check 1: Skill exists
    if skill is None:
        return ValidationResult(
            valid=False,
            warnings=["No skill mapping found - manual execution required"]
        )

    # Check 2: Skill is available
    if skill not in AVAILABLE_SKILLS:
        return ValidationResult(
            valid=False,
            warnings=[f"Skill not found: {skill}"]
        )

    # Check 3: Confidence threshold
    if confidence < CONFIG.skill_mapping.confidence_threshold:
        return ValidationResult(
            valid=True,
            warnings=[f"Low mapping confidence ({confidence:.2f}) - user confirmation recommended"]
        )

    # Valid mapping
    return ValidationResult(valid=True, warnings=[])
```

### Available Skills List

```python
AVAILABLE_SKILLS = [
    # P0 Skills (Critical)
    'smartadmin-crud-generator',
    'smartadmin-integration-test',
    'test-fixture-generator',
    'vavr-refactoring-assistant',
    'archunit-test-generator',
    'security-hardening-pro',

    # P1 Skills (Important)
    'liteflow-rule-builder',
    'quality-gate-orchestrator',
    'fraud-detection-pattern-generator',

    # P2 Skills (Nice-to-have)
    'smartadmin-performance-suite',
    'smartadmin-testing-suite',
    'db-migration-manager',
    'igame-feature-builder',
    'cicd-pipeline-builder',
    'cache-strategy-generator',
    'message-queue-pattern-generator',
    'websocket-sse-realtime-generator',
    'i18n-generator',
    'report-generator',
    'full-text-search-integration',
    'apm-integration',
    'scheduled-task-manager',
]
```

---

## Mapping Examples

### Example 1: Claude Code Plan (CRUD)

**Input Plan**:

```yaml
---
name: product-crud
type: crud
created_at: 2026-01-28
---

# Product CRUD Module Implementation Plan
...
```

**Mapping Process**:

```python
# Step 1: Detect plan type
plan_type = detect_plan_type(file_path)  # CLAUDE_CODE_PLAN

# Step 2: Extract metadata
plan = extract_claude_code_plan(file_path)
# plan.type = 'crud'

# Step 3: Map to skill
skill, confidence = map_claude_code_plan(plan)
# skill = 'smartadmin-crud-generator'
# confidence = 1.0 (type-based mapping)
```

**Result**: `smartadmin-crud-generator` (confidence: 1.0)

### Example 2: Skills Phase Doc

**Input Plan**:

```
# Phase 2: Integration Testing with Testcontainers

**Phase**: 2/4
**Status**: ✅ Production Ready
...
```

**File Path**: `.claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md`

**Mapping Process**:

```python
# Step 1: Detect plan type
plan_type = detect_plan_type(file_path)  # SKILL_PHASE_DOC

# Step 2: Extract metadata
plan = extract_skill_phase_doc(file_path)
# plan.skill_name = 'smartadmin-testing-suite' (from path)

# Step 3: Map to skill
skill, confidence = map_skill_phase_doc(plan)
# skill = 'smartadmin-testing-suite'
# confidence = 1.0 (path-based mapping)
```

**Result**: `smartadmin-testing-suite` (confidence: 1.0)

### Example 3: Project Plan (LiteFlow)

**Input Plan**:

```yaml
---
module: liteflow
feature: rule-migration
type: migration
priority: high
---

# LiteFlow Rule Migration Implementation Plan
...
```

**Mapping Process**:

```python
# Step 1: Detect plan type
plan_type = detect_plan_type(file_path)  # PROJECT_PLAN

# Step 2: Extract metadata
plan = extract_project_plan(file_path)
# plan.module = 'liteflow'

# Step 3: Map to skill
skill, confidence = map_project_plan(plan)
# skill = MODULE_SKILL_MAPPINGS['liteflow'] = 'liteflow-rule-builder'
# confidence = 1.0 (module-based mapping)
```

**Result**: `liteflow-rule-builder` (confidence: 1.0)

### Example 4: Content-Based Fallback

**Input Plan**:

```yaml
---
name: custom-optimization
type: custom
---

# Custom Performance Optimization Plan

## Overview
Optimize N+1 query issues, implement caching strategy, and tune JVM parameters.

## Implementation
- Detect N+1 queries using profiling
- Add Caffeine L1 cache
- Add Redis L2 cache
- Tune JVM memory settings
...
```

**Mapping Process**:

```python
# Step 1: Type-based mapping fails
skill = TYPE_SKILL_MAPPINGS.get('custom')  # None

# Step 2: Content-based fallback
skill, confidence = content_based_mapping(plan.content)

# Keywords matched:
# - 'cache' (smartadmin-performance-suite, cache-strategy-generator)
# - 'caffeine' (cache-strategy-generator)
# - 'redis' (cache-strategy-generator)
# - 'jvm' (smartadmin-performance-suite)
# - 'n+1' (smartadmin-performance-suite)
# - 'profiling' (smartadmin-performance-suite)

# Scores:
# smartadmin-performance-suite: 4/10 keywords = 0.40
# cache-strategy-generator: 3/8 keywords = 0.375

# Best match: smartadmin-performance-suite
# Raw score: 0.40
# Confidence: min(0.40 * 1.5, 0.8) = 0.60
```

**Result**: `smartadmin-performance-suite` (confidence: 0.60)

**Action**: User confirmation recommended (confidence < 0.7)

### Example 5: Manual Execution Required

**Input Plan**:

```yaml
---
module: tenant
feature: multi-tenant-setup
type: custom
priority: high
---

# Multi-tenant Architecture Setup
...
```

**Mapping Process**:

```python
# Step 1: Module-based mapping
skill = MODULE_SKILL_MAPPINGS.get('tenant')  # None

# Step 2: Content-based fallback
skill, confidence = content_based_mapping(plan.content)
# No strong keyword matches
# skill = None, confidence = 0.3
```

**Result**: `None` (Manual execution required)

**Report**:

```
⚠ Plan: multi-tenant-setup
  - No skill mapping found
  - Confidence: 0.0
  - Action: Manual execution required
```

---

## Troubleshooting

### Issue 1: Wrong Skill Mapped

**Symptom**: Plan is mapped to incorrect skill.

**Cause**: Type or module name is ambiguous, content keywords misleading.

**Solution**:

1. **Add explicit skill field**:
   ```yaml
   ---
   skill: correct-skill-name
   ---
   ```

2. **Adjust content keywords**:
   - Add more specific keywords for target skill
   - Remove misleading keywords

3. **Use custom mapping**:
   ```yaml
   # config.yml
   skill_mapping:
     custom_mappings:
       'docs/plans/ambiguous-plan.md': 'correct-skill-name'
   ```

### Issue 2: Low Confidence Score

**Symptom**: Mapping confidence < 0.7, user confirmation required.

**Cause**: Insufficient or ambiguous keywords.

**Solution**:

1. **Add more keywords**:
   - Include skill-specific terminology
   - Use exact phrases from skill documentation

2. **Lower confidence threshold** (if acceptable):
   ```yaml
   skill_mapping:
     auto_mapping:
       confidence_threshold: 0.5
   ```

3. **Explicit skill field**:
   ```yaml
   ---
   skill: target-skill-name
   ---
   ```

### Issue 3: No Skill Mapped

**Symptom**: `skill = None`, manual execution required.

**Cause**: Custom plan type, no matching module, no content keywords.

**Solution**:

1. **Specify skill explicitly**:
   ```yaml
   ---
   skill: appropriate-skill-name
   ---
   ```

2. **Add custom mapping**:
   ```yaml
   skill_mapping:
     custom_mappings:
       'docs/plans/custom/*.md': 'default-skill-name'
   ```

3. **Accept manual execution**:
   - Some plans genuinely require manual implementation
   - Not all tasks have corresponding skills

---

## Summary

**Skill 映射策略總結**:

1. **Explicit Mapping** (Confidence: 1.0)
   - YAML frontmatter `skill:` field
   - Custom mappings in config.yml

2. **Type-Based Mapping** (Confidence: 0.9-1.0)
   - Claude Code Plans: `type` → Skill
   - High accuracy for standard types (crud, testing, etc.)

3. **Module-Based Mapping** (Confidence: 1.0)
   - Project Plans: `module` → Skill
   - Comprehensive module-to-skill mappings

4. **Path-Based Mapping** (Confidence: 1.0)
   - Skills Phase Docs: Extract from file path
   - Guaranteed accuracy

5. **Content-Based Mapping** (Confidence: 0.5-0.8)
   - Fallback mechanism
   - Keyword matching with scoring

**Confidence Thresholds**:
- ≥ 0.7: Auto-execute
- 0.5 - 0.69: User confirmation
- < 0.5: Manual execution

**Best Practices**:
- Always specify `skill:` for custom plans
- Use standard types and modules when possible
- Include skill-specific keywords in content
- Validate mappings with dry-run mode

---

**Last Updated**: 2026-01-29
**Documentation Version**: v1.0.0
**Status**: ✅ Production Ready
