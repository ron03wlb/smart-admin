# Phase 1: Plan Discovery and Classification

**Phase**: 1/4
**Status**: ✅ Production Ready (v1.0.0)
**Duration**: 8-12 hours
**Complexity**: Medium

---

## Overview

This phase implements the **Plan Discovery and Classification** system, which automatically identifies, classifies, and validates implementation plans from multiple sources.

### Objectives

1. ✅ Scan multiple directories for plan documents
2. ✅ Classify plans into three types (Claude Code Plans, Skills Docs, Project Plans)
3. ✅ Extract plan metadata and content structure
4. ✅ Validate plan format and completeness
5. ✅ Generate plan inventory report

### Success Criteria

- [ ] Detect 95%+ of plans in configured directories
- [ ] Correctly classify plan types with 90%+ accuracy
- [ ] Extract metadata from all supported formats
- [ ] Generate comprehensive plan inventory

---

## Plan Type Classification

### Type 1: Claude Code Plans

**Location**: `~/.claude/plans/*.md`

**Identification Patterns**:

1. **File Path Pattern** (Priority 1):
   ```regex
   ^~/.claude/plans/[^/]+\.md$
   ```

2. **YAML Frontmatter** (Priority 2):
   ```yaml
   ---
   name: product-crud
   type: crud
   created_at: 2026-01-28
   skill: smartadmin-crud-generator  # Optional, for direct mapping
   ---
   ```

3. **Title Format** (Priority 3):
   ```markdown
   # {Project Name} {Feature} Implementation Plan
   # Example: Product CRUD Module Implementation Plan
   ```

**Extraction Logic**:

```python
def extract_claude_code_plan(file_path: str) -> ClaudeCodePlan:
    """
    Extract Claude Code Plan metadata and content.

    Args:
        file_path: Absolute path to the plan file

    Returns:
        ClaudeCodePlan object with extracted metadata
    """
    content = read_file(file_path)

    # Extract YAML frontmatter
    frontmatter = extract_yaml_frontmatter(content)

    # Extract title
    title = extract_first_h1_heading(content)

    # Extract plan type from frontmatter or infer from content
    plan_type = frontmatter.get('type') or infer_plan_type(content)

    # Extract skill mapping (if specified)
    skill = frontmatter.get('skill') or map_plan_type_to_skill(plan_type)

    return ClaudeCodePlan(
        file_path=file_path,
        name=frontmatter.get('name') or sanitize_filename(title),
        type=plan_type,
        title=title,
        skill=skill,
        created_at=frontmatter.get('created_at'),
        metadata=frontmatter,
        content=content
    )
```

**Plan Type Inference**:

```python
def infer_plan_type(content: str) -> str:
    """
    Infer plan type from content when not explicitly specified.

    Returns:
        One of: crud, testing, refactoring, migration, integration, custom
    """
    keywords = {
        'crud': ['entity', 'dao', 'controller', 'service', 'crud'],
        'testing': ['test', 'junit', 'testcontainers', 'integration test'],
        'refactoring': ['refactor', 'vavr', 'option', 'try', 'either'],
        'migration': ['migrate', 'migration', 'upgrade', 'liteflow', 'kafka'],
        'integration': ['integration', 'api', 'third-party', 'external'],
        'security': ['security', 'encryption', 'masking', 'audit'],
        'performance': ['performance', 'cache', 'optimize', 'monitoring']
    }

    content_lower = content.lower()
    scores = {}

    for plan_type, keywords_list in keywords.items():
        score = sum(1 for keyword in keywords_list if keyword in content_lower)
        scores[plan_type] = score

    # Return type with highest score (or 'custom' if all scores are 0)
    max_score = max(scores.values())
    if max_score == 0:
        return 'custom'

    return max(scores, key=scores.get)
```

### Type 2: Skills Phase Documentation

**Location**: `.claude/skills/*/phases/phase-*.md`

**Identification Patterns**:

1. **File Path Pattern** (Priority 1):
   ```regex
   ^\.claude/skills/([^/]+)/phases/phase-(\d+)-([^/]+)\.md$
   ```

2. **Title Format** (Priority 2):
   ```markdown
   # Phase {N}: {Phase Name}
   # Example: Phase 2: Frontend Component Implementation
   ```

3. **Parent Skill Detection** (Priority 3):
   - Extract skill name from file path: `.claude/skills/{skill-name}/phases/`

**Extraction Logic**:

```python
def extract_skill_phase_doc(file_path: str) -> SkillPhaseDoc:
    """
    Extract Skills Phase Documentation metadata and content.

    Args:
        file_path: Absolute path to the phase document

    Returns:
        SkillPhaseDoc object with extracted metadata
    """
    content = read_file(file_path)

    # Extract skill name from path
    skill_name = extract_skill_name_from_path(file_path)

    # Extract phase number and name from filename
    match = re.search(r'phase-(\d+)-([^/]+)\.md$', file_path)
    phase_number = int(match.group(1))
    phase_slug = match.group(2)

    # Extract title
    title = extract_first_h1_heading(content)

    # Parse phase name from title
    phase_name = parse_phase_name_from_title(title) or phase_slug.replace('-', ' ').title()

    # Extract YAML frontmatter (if exists)
    frontmatter = extract_yaml_frontmatter(content)

    return SkillPhaseDoc(
        file_path=file_path,
        skill_name=skill_name,
        phase_number=phase_number,
        phase_name=phase_name,
        title=title,
        metadata=frontmatter,
        content=content,
        # Direct mapping to parent skill
        skill=skill_name
    )
```

**Skill Name Extraction**:

```python
def extract_skill_name_from_path(file_path: str) -> str:
    """
    Extract skill name from file path.

    Example:
        .claude/skills/smartadmin-crud-generator/phases/phase-1.md
        -> smartadmin-crud-generator
    """
    match = re.search(r'\.claude/skills/([^/]+)/phases/', file_path)
    if match:
        return match.group(1)
    raise ValueError(f"Cannot extract skill name from path: {file_path}")
```

### Type 3: Project Plans

**Location**: `docs/plans/{module}/*.md`

**Identification Patterns**:

1. **File Path Pattern** (Priority 1):
   ```regex
   ^docs/plans/([^/]+)/([^/]+)\.md$
   ```

2. **YAML Frontmatter** (Priority 2):
   ```yaml
   ---
   module: liteflow
   feature: rule-migration
   type: migration
   priority: high
   dependencies:
     - liteflow-setup
     - evrete-analysis
   skill: liteflow-rule-builder  # Optional, for direct mapping
   ---
   ```

3. **Title Format** (Priority 3):
   ```markdown
   # {Module} - {Feature} Implementation Plan
   # Example: LiteFlow - Rule Migration Implementation Plan
   ```

**Extraction Logic**:

```python
def extract_project_plan(file_path: str) -> ProjectPlan:
    """
    Extract Project Plan metadata and content.

    Args:
        file_path: Absolute path to the plan file

    Returns:
        ProjectPlan object with extracted metadata
    """
    content = read_file(file_path)

    # Extract YAML frontmatter
    frontmatter = extract_yaml_frontmatter(content)

    # Extract module from path or frontmatter
    module = frontmatter.get('module') or extract_module_from_path(file_path)

    # Extract feature name
    feature = frontmatter.get('feature') or extract_feature_from_filename(file_path)

    # Extract title
    title = extract_first_h1_heading(content)

    # Extract dependencies
    dependencies = frontmatter.get('dependencies', [])

    # Extract or infer skill mapping
    skill = frontmatter.get('skill') or map_module_to_skill(module)

    return ProjectPlan(
        file_path=file_path,
        module=module,
        feature=feature,
        title=title,
        type=frontmatter.get('type', 'custom'),
        priority=frontmatter.get('priority', 'medium'),
        dependencies=dependencies,
        skill=skill,
        metadata=frontmatter,
        content=content
    )
```

**Module-to-Skill Mapping**:

```python
# Default module-to-skill mappings
MODULE_SKILL_MAPPINGS = {
    'liteflow': 'liteflow-rule-builder',
    'job': 'scheduled-task-manager',
    'tenant': None,  # Requires manual execution
    'kafka': 'message-queue-pattern-generator',
    'cache': 'cache-strategy-generator',
    'websocket': 'websocket-sse-realtime-generator',
    'security': 'security-hardening-pro',
    'migration': 'db-migration-manager',
}

def map_module_to_skill(module: str) -> Optional[str]:
    """
    Map module name to corresponding skill.

    Returns:
        Skill name or None if manual execution required
    """
    return MODULE_SKILL_MAPPINGS.get(module.lower())
```

---

## Discovery Algorithm

### Step 1: Directory Scanning

```python
def scan_plan_directories(config: Config) -> List[str]:
    """
    Scan configured directories for plan files.

    Args:
        config: Configuration object with directory settings

    Returns:
        List of absolute file paths to plan documents
    """
    plan_files = []

    # Scan Claude Code Plans directory
    if config.scan_claude_code_plans:
        plans_dir = expand_path(config.claude_code_plans_dir)
        plan_files.extend(glob_files(
            path=plans_dir,
            pattern=config.claude_code_plans_pattern,
            max_depth=config.max_depth,
            exclude=config.exclude_patterns
        ))

    # Scan Skills Phase Docs directories
    if config.scan_skill_phase_docs:
        skills_dir = expand_path(config.skill_phase_docs_dir)
        plan_files.extend(glob_files(
            path=skills_dir,
            pattern=config.skill_phase_docs_pattern,
            max_depth=config.max_depth,
            exclude=config.exclude_patterns
        ))

    # Scan Project Plans directory
    if config.scan_project_plans:
        plans_dir = expand_path(config.project_plans_dir)
        plan_files.extend(glob_files(
            path=plans_dir,
            pattern=config.project_plans_pattern,
            max_depth=config.max_depth,
            exclude=config.exclude_patterns
        ))

    return sorted(set(plan_files))  # Remove duplicates and sort
```

### Step 2: Plan Type Detection

```python
def detect_plan_type(file_path: str) -> PlanType:
    """
    Detect plan type based on file path and content.

    Returns:
        One of: CLAUDE_CODE_PLAN, SKILL_PHASE_DOC, PROJECT_PLAN, UNKNOWN
    """
    # Priority 1: File path pattern matching
    if re.match(r'^~/.claude/plans/', expand_path(file_path)):
        return PlanType.CLAUDE_CODE_PLAN

    if re.search(r'\.claude/skills/[^/]+/phases/phase-\d+', file_path):
        return PlanType.SKILL_PHASE_DOC

    if re.match(r'^docs/plans/', file_path):
        return PlanType.PROJECT_PLAN

    # Priority 2: Content analysis
    content = read_file(file_path)
    frontmatter = extract_yaml_frontmatter(content)

    # Check for skill reference in frontmatter
    if frontmatter and 'skill' in frontmatter:
        # Likely a Project Plan or Claude Code Plan
        if 'module' in frontmatter:
            return PlanType.PROJECT_PLAN
        return PlanType.CLAUDE_CODE_PLAN

    # Priority 3: Title format analysis
    title = extract_first_h1_heading(content)
    if re.match(r'^Phase \d+:', title):
        return PlanType.SKILL_PHASE_DOC

    # Fallback: Unknown type (requires user confirmation)
    return PlanType.UNKNOWN
```

### Step 3: Plan Extraction

```python
def extract_plan(file_path: str, plan_type: PlanType) -> Plan:
    """
    Extract plan based on detected type.

    Args:
        file_path: Absolute path to plan file
        plan_type: Detected plan type

    Returns:
        Plan object (ClaudeCodePlan, SkillPhaseDoc, or ProjectPlan)
    """
    extractors = {
        PlanType.CLAUDE_CODE_PLAN: extract_claude_code_plan,
        PlanType.SKILL_PHASE_DOC: extract_skill_phase_doc,
        PlanType.PROJECT_PLAN: extract_project_plan,
    }

    extractor = extractors.get(plan_type)
    if not extractor:
        raise ValueError(f"Unknown plan type: {plan_type}")

    return extractor(file_path)
```

---

## Plan Validation

### Validation Rules

```python
def validate_plan(plan: Plan) -> ValidationResult:
    """
    Validate plan completeness and correctness.

    Returns:
        ValidationResult with errors and warnings
    """
    errors = []
    warnings = []

    # Rule 1: File must exist and be readable
    if not os.path.isfile(plan.file_path):
        errors.append(f"File not found: {plan.file_path}")
        return ValidationResult(valid=False, errors=errors, warnings=warnings)

    # Rule 2: Must have a title
    if not plan.title or plan.title.strip() == '':
        errors.append("Missing plan title (H1 heading)")

    # Rule 3: Skill mapping validation
    if plan.skill is None:
        warnings.append("No skill mapping found - manual execution required")
    elif plan.skill not in AVAILABLE_SKILLS:
        warnings.append(f"Skill not found: {plan.skill}")

    # Rule 4: Type-specific validation
    if isinstance(plan, ClaudeCodePlan):
        if not plan.metadata:
            warnings.append("Missing YAML frontmatter - recommended for Claude Code Plans")

    if isinstance(plan, ProjectPlan):
        if plan.dependencies and not all(dep in KNOWN_MODULES for dep in plan.dependencies):
            warnings.append("Unknown dependencies detected - may cause execution issues")

    # Rule 5: Content validation
    if len(plan.content) < 100:
        warnings.append("Plan content is suspiciously short - may be incomplete")

    return ValidationResult(
        valid=len(errors) == 0,
        errors=errors,
        warnings=warnings
    )
```

### Validation Report

```python
def generate_validation_report(plans: List[Plan]) -> str:
    """
    Generate validation report for all plans.
    """
    total_plans = len(plans)
    valid_plans = sum(1 for p in plans if validate_plan(p).valid)
    invalid_plans = total_plans - valid_plans

    report = f"""
╔══════════════════════════════════════════════════════════════════
║ Plan Validation Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans: {total_plans}
║ Valid Plans: {valid_plans}
║ Invalid Plans: {invalid_plans}
╠══════════════════════════════════════════════════════════════════
"""

    # List invalid plans with errors
    for plan in plans:
        result = validate_plan(plan)
        if not result.valid:
            report += f"║ ✗ {plan.file_path}\n"
            for error in result.errors:
                report += f"║     - ERROR: {error}\n"
        elif result.warnings:
            report += f"║ ⚠ {plan.file_path}\n"
            for warning in result.warnings:
                report += f"║     - WARNING: {warning}\n"

    report += "╚══════════════════════════════════════════════════════════════════\n"
    return report
```

---

## Plan Inventory Report

### Inventory Format

```python
def generate_inventory_report(plans: List[Plan]) -> str:
    """
    Generate comprehensive plan inventory report.
    """
    # Classify plans by type
    claude_code_plans = [p for p in plans if isinstance(p, ClaudeCodePlan)]
    skill_phase_docs = [p for p in plans if isinstance(p, SkillPhaseDoc)]
    project_plans = [p for p in plans if isinstance(p, ProjectPlan)]

    report = f"""
╔══════════════════════════════════════════════════════════════════
║ Plan Discovery - Inventory Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans Discovered: {len(plans)}
║
║ Plan Types:
║   - Claude Code Plans: {len(claude_code_plans)}
║   - Skills Phase Docs: {len(skill_phase_docs)}
║   - Project Plans: {len(project_plans)}
╠══════════════════════════════════════════════════════════════════
║ Claude Code Plans ({len(claude_code_plans)}):
"""

    for plan in claude_code_plans:
        skill_info = f"→ {plan.skill}" if plan.skill else "⚠ Manual"
        report += f"║   [{plan.type}] {plan.name} {skill_info}\n"
        report += f"║       Path: {plan.file_path}\n"

    report += f"""╠══════════════════════════════════════════════════════════════════
║ Skills Phase Docs ({len(skill_phase_docs)}):
"""

    # Group by skill
    skill_groups = {}
    for plan in skill_phase_docs:
        if plan.skill_name not in skill_groups:
            skill_groups[plan.skill_name] = []
        skill_groups[plan.skill_name].append(plan)

    for skill_name, docs in sorted(skill_groups.items()):
        report += f"║   [{skill_name}]\n"
        for doc in sorted(docs, key=lambda d: d.phase_number):
            report += f"║     - Phase {doc.phase_number}: {doc.phase_name}\n"

    report += f"""╠══════════════════════════════════════════════════════════════════
║ Project Plans ({len(project_plans)}):
"""

    # Group by module
    module_groups = {}
    for plan in project_plans:
        if plan.module not in module_groups:
            module_groups[plan.module] = []
        module_groups[plan.module].append(plan)

    for module, plans_list in sorted(module_groups.items()):
        report += f"║   [{module}]\n"
        for plan in plans_list:
            skill_info = f"→ {plan.skill}" if plan.skill else "⚠ Manual"
            priority = plan.priority.upper()
            report += f"║     - {plan.feature} ({priority}) {skill_info}\n"

    report += "╚══════════════════════════════════════════════════════════════════\n"
    return report
```

---

## Example Output

### Inventory Report Example

```
╔══════════════════════════════════════════════════════════════════
║ Plan Discovery - Inventory Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans Discovered: 12
║
║ Plan Types:
║   - Claude Code Plans: 4
║   - Skills Phase Docs: 5
║   - Project Plans: 3
╠══════════════════════════════════════════════════════════════════
║ Claude Code Plans (4):
║   [crud] product-crud → smartadmin-crud-generator
║       Path: ~/.claude/plans/product-crud.md
║   [crud] order-crud → smartadmin-crud-generator
║       Path: ~/.claude/plans/order-crud.md
║   [refactoring] service-vavr-migration → vavr-refactoring-assistant
║       Path: ~/.claude/plans/service-vavr-migration.md
║   [testing] integration-tests → smartadmin-testing-suite
║       Path: ~/.claude/plans/integration-tests.md
╠══════════════════════════════════════════════════════════════════
║ Skills Phase Docs (5):
║   [smartadmin-crud-generator]
║     - Phase 2: Frontend Component Implementation
║     - Phase 3: API Documentation
║   [liteflow-rule-builder]
║     - Phase 1: LiteFlow Setup
║     - Phase 2: Rule DSL Generation
║   [scheduled-task-manager]
║     - Phase 1: XXL-Job Integration
╠══════════════════════════════════════════════════════════════════
║ Project Plans (3):
║   [liteflow]
║     - rule-migration (HIGH) → liteflow-rule-builder
║     - performance-tuning (MEDIUM) → liteflow-rule-builder
║   [tenant]
║     - multi-tenant-setup (HIGH) ⚠ Manual
╚══════════════════════════════════════════════════════════════════
```

---

## Implementation Checklist

### Core Functions

- [ ] `scan_plan_directories()` - Directory scanning
- [ ] `detect_plan_type()` - Plan type detection
- [ ] `extract_claude_code_plan()` - Claude Code Plan extraction
- [ ] `extract_skill_phase_doc()` - Skills Phase Doc extraction
- [ ] `extract_project_plan()` - Project Plan extraction
- [ ] `validate_plan()` - Plan validation
- [ ] `generate_inventory_report()` - Inventory report generation
- [ ] `generate_validation_report()` - Validation report generation

### Helper Functions

- [ ] `extract_yaml_frontmatter()` - YAML parsing
- [ ] `extract_first_h1_heading()` - Title extraction
- [ ] `infer_plan_type()` - Type inference
- [ ] `map_plan_type_to_skill()` - Skill mapping
- [ ] `map_module_to_skill()` - Module-to-skill mapping
- [ ] `glob_files()` - File globbing with exclusions

### Data Structures

- [ ] `PlanType` enum
- [ ] `ClaudeCodePlan` class
- [ ] `SkillPhaseDoc` class
- [ ] `ProjectPlan` class
- [ ] `ValidationResult` class

---

## Testing

### Unit Tests

```python
def test_detect_plan_type_claude_code_plan():
    assert detect_plan_type("~/.claude/plans/test.md") == PlanType.CLAUDE_CODE_PLAN

def test_detect_plan_type_skill_phase_doc():
    path = ".claude/skills/smartadmin-crud-generator/phases/phase-1-setup.md"
    assert detect_plan_type(path) == PlanType.SKILL_PHASE_DOC

def test_detect_plan_type_project_plan():
    assert detect_plan_type("docs/plans/liteflow/migration.md") == PlanType.PROJECT_PLAN

def test_infer_plan_type_crud():
    content = "Create Entity, Dao, Service, Controller for Product module"
    assert infer_plan_type(content) == 'crud'

def test_extract_skill_name_from_path():
    path = ".claude/skills/liteflow-rule-builder/phases/phase-2.md"
    assert extract_skill_name_from_path(path) == "liteflow-rule-builder"
```

### Integration Tests

```python
def test_scan_and_classify_all_plans():
    config = load_config()
    plan_files = scan_plan_directories(config)

    plans = []
    for file_path in plan_files:
        plan_type = detect_plan_type(file_path)
        plan = extract_plan(file_path, plan_type)
        plans.append(plan)

    assert len(plans) > 0
    assert all(isinstance(p, (ClaudeCodePlan, SkillPhaseDoc, ProjectPlan)) for p in plans)
```

---

## Next Steps

After completing Phase 1, proceed to:
- **[Phase 2: Conflict Detection](phase-2-conflict-detection.md)** - Implement three-layer conflict detection
- **[Phase 3: Execution Planning](phase-3-execution-planning.md)** - Generate optimal execution plan
- **[Phase 4: Parallel Execution](phase-4-parallel-execution.md)** - Execute plans with parallelization

---

**Phase Status**: ✅ Complete
**Documentation Version**: v1.0.0
**Last Updated**: 2026-01-29
