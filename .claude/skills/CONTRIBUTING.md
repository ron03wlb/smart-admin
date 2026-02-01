# Contributing to SmartAdmin Skills

## Skill Dependency Management

### Overview

SmartAdmin skills use a **dependency graph** to ensure proper execution order and avoid circular dependencies.

### Adding Skill Dependencies

When creating or updating skills that depend on other skills:

1. **Declare dependencies in `depends_on` field**:
   ```yaml
   my-skill:
     path: "category/my-skill"
     depends_on: ["dependency-skill-1", "dependency-skill-2"]
     depended_by: []  # Leave empty, validated automatically
   ```

2. **Run validation**:
   ```bash
   python3 .claude/scripts/validate-dependency-graph.py
   ```

3. **Check for errors**:
   - Circular dependency detection (A → B → A)
   - Non-existent skill references
   - Inconsistent `depended_by` declarations

### Dependency Graph Rules

**TIER 0 (Foundation)**:
- Skills with `depends_on: []`
- No dependencies on other skills
- Examples: `archunit-test-generator`, `security-hardening-pro`

**TIER 1 (Domain/Composite)**:
- Skills that depend on Tier 0 skills
- Examples: `smartadmin-testing-suite`, `smartadmin-performance-suite`

**TIER 2 (Orchestrators)**:
- Skills that depend on Tier 1 skills
- Examples: `batch-plan-executor`

### Validation Messages

**✅ Success**:
```
✅ Dependency graph is VALID
✅ Topological sort successful (33 skills sorted)
```

**❌ Circular Dependency**:
```
❌ Circular dependency detected: skill-a → skill-b → skill-a
```

**⚠️ Mismatch Warning**:
```
⚠️ skill-a: depended_by includes 'skill-b', but 'skill-b' doesn't depend on 'skill-a'
```

Fix: Update `skill-b.depends_on` to include `"skill-a"`, or remove from `skill-a.depended_by`.

### Testing Your Changes

Before committing:

```bash
# 1. Validate dependency graph
python3 .claude/scripts/validate-dependency-graph.py

# 2. Check for skill registry syntax errors
python3 -c "import yaml; yaml.safe_load(open('.claude/skills/skill-registry.yml'))"

# 3. Commit with descriptive message
git add .claude/skills/skill-registry.yml
git commit -m "feat(skills): add my-skill with dependencies on skill-a, skill-b"
```

### Common Issues

**Issue**: "Circular dependency detected"
- **Cause**: Skill A depends on B, and B depends on A
- **Fix**: Redesign dependencies to be one-way only

**Issue**: "Non-existent skill reference"
- **Cause**: `depends_on` lists a skill that doesn't exist in registry
- **Fix**: Check skill name spelling, or add missing skill first

**Issue**: "depended_by mismatch"
- **Cause**: `depended_by` field doesn't match actual dependencies
- **Fix**: Update `depends_on` in dependent skills, or remove from `depended_by`

## Skill Configuration Standards

### Required Fields

All skills must have a `config.yml` with:

```yaml
name: "skill-name"
version: "1.0.0"
priority: "P0|P1|P2"  # P0=Foundation, P1=Extended, P2=Productivity
type: "atomic|composite|orchestrator"
category: "backend|full-stack|testing|domain|orchestration|quality|devops|integration|composite|analysis|refactoring"
status: "stable|experimental|deprecated"
description: "Clear description of what this skill does"

triggers:
  keywords: ["keyword1", "keyword2"]
  patterns: ["pattern1", "pattern2"]
  exclude_keywords: ["exclude1"]

execution:
  type: "single-shot|phase-based|mode-based"
  timeout_minutes: 30

depends_on: []  # List of skill dependencies
depended_by: []  # Auto-derived, leave empty
```

### Skill Types

**Atomic**: Single, focused capability
- Example: `test-fixture-generator`, `archunit-test-generator`

**Composite**: Combines multiple atomic skills
- Example: `smartadmin-testing-suite`, `smartadmin-performance-suite`

**Orchestrator**: Coordinates multiple composite/atomic skills
- Example: `batch-plan-executor`

### Priority Levels

**P0 (Foundation)**: Critical infrastructure skills
- Backend: ArchUnit, Security, Vavr
- Full-stack: CRUD generator, Integration tests
- Testing: Test fixtures

**P1 (Extended)**: Domain-specific and quality skills
- Domain: iGaming features, LiteFlow rules
- Orchestration: Batch planning, Quality gates
- Quality: Concurrency safety, Spring patterns

**P2 (Productivity)**: Developer productivity tools
- DevOps: APM, CI/CD, DB migration
- Integration: Cache, Search, i18n, MQ
- Analysis: Performance profiling
- Refactoring: Manager extraction, Vavr migration

## Testing Guidelines

### Unit Tests

Skills should include unit tests for:
- Configuration parsing
- Input validation
- Core logic functions

### Integration Tests

For skills that interact with external systems:
- Mock external dependencies
- Test error handling
- Verify output format

### Manual Testing

Before submitting:
1. Test skill invocation via keyword triggers
2. Verify output quality
3. Check execution time
4. Validate error messages

## Documentation Requirements

### SKILL.md

Every skill must have a `SKILL.md` with:

1. **Overview**: What the skill does
2. **Triggers**: Keywords, patterns that invoke it
3. **Execution**: Type, phases/modes, timeout
4. **Dependencies**: Required skills, files, tools
5. **Examples**: Sample inputs and outputs
6. **Troubleshooting**: Common issues and fixes

### README.md

Update the main [README.md](README.md) when:
- Adding a new skill category
- Changing skill hierarchy
- Updating priority levels
- Modifying dependency rules

## Code Style

### YAML Style

- Use 2-space indentation
- Quote strings with special characters
- Keep lists inline for 1-3 items, multiline for 4+
- Add comments for non-obvious configurations

### Python Style

For validation scripts:
- Follow PEP 8
- Use type hints
- Add docstrings for functions
- Keep functions under 50 lines

### Documentation Style

- Use active voice
- Start with verbs ("Generate", "Validate", "Build")
- Keep sentences concise
- Use code blocks for examples
- Add emojis sparingly (✅ ❌ ⚠️ only)

## Git Workflow

### Branch Naming

- `feat/skill-name` - New skill
- `fix/skill-name` - Bug fix
- `docs/skill-name` - Documentation update
- `refactor/skill-migration` - Refactoring

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(skills): add postgresql-best-practices skill

- HikariCP connection pool analysis
- N+1 query detection via P6Spy
- EXPLAIN ANALYZE automation
- Index recommendations

Closes #123
```

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] New skill
- [ ] Bug fix
- [ ] Documentation update
- [ ] Refactoring

## Checklist
- [ ] Dependency validation passes
- [ ] config.yml follows standards
- [ ] SKILL.md documentation complete
- [ ] No circular dependencies
- [ ] Tests added/updated
- [ ] README.md updated (if needed)

## Testing
Describe how you tested the changes
```

## Release Process

### Version Numbering

Follow [Semantic Versioning](https://semver.org/):
- **Major**: Breaking changes (2.0.0 → 3.0.0)
- **Minor**: New skills, features (3.0.0 → 3.1.0)
- **Patch**: Bug fixes, docs (3.1.0 → 3.1.1)

### Changelog

Update [README.md](README.md) Version History section:

```markdown
### 3.1.0 (2026-01-31) - New Skill Addition
**New Skills**:
- ✅ **postgresql-best-practices**: Database performance optimization
- ✅ **smartadmin-manager-extractor**: Auto-extract @Transactional methods

**Improvements**:
- Fixed dependency validation script
- Updated skill registry documentation
```

## Getting Help

- **Questions**: Open a GitHub issue with `question` label
- **Bug Reports**: Include skill name, error message, and steps to reproduce
- **Feature Requests**: Describe use case and expected behavior
- **Documentation**: Clarify what's confusing and suggest improvements

## Code of Conduct

- Be respectful and constructive
- Focus on the code, not the person
- Welcome diverse perspectives
- Help newcomers learn
- Give credit where it's due
