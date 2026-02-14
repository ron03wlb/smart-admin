---
name: quality-gate-orchestrator
description: [P1 - Extended] Automate quality gates orchestrating ArchUnit, Checkstyle, PMD, SpotBugs, JaCoCo, SonarQube. Use when running comprehensive quality checks.
---

# quality-gate-orchestrator

**Priority**: P1 (Critical Infrastructure)
**Status**: Active
**Version**: 1.0.0
**Last Updated**: 2026-01-25

## TL;DR

Automate quality gates orchestrating ArchUnit, Checkstyle, PMD, SpotBugs, JaCoCo, and SonarQube. Generate pre-merge quality checks, CI/CD pipelines, and violation reports with configurable severity thresholds.

---

## Quick Start

### Common Scenarios

```bash
# Scenario 1: Generate complete pre-commit quality check
generate-quality-gate --type local --strategy sequential --fail-fast

# Scenario 2: Create GitHub Actions quality pipeline
generate-quality-gate --type github-actions --strategy parallel --with-sonar

# Scenario 3: Generate GitLab CI quality stages
generate-quality-gate --type gitlab-ci --strategy sequential --coverage-threshold 80

# Scenario 4: Generate aggregated quality report
generate-quality-gate --type report --format html --include-all-tools

# Scenario 5: Pre-merge quality gate (fail-fast)
generate-quality-gate --type pre-merge --fail-on blocker,critical
```

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "quality gate" - Quality gate setup and orchestration
- "quality orchestration" - Orchestrate multiple quality tools
- "pre-commit checks" - Pre-commit quality validation setup
- "code quality checks" - Comprehensive code quality validation

**Secondary Keywords** (Medium confidence):
- "ArchUnit integration" - Integrate ArchUnit with quality pipeline
- "Checkstyle pipeline" - Checkstyle automation in CI/CD
- "PMD automation" - Automated PMD checks
- "SpotBugs workflow" - SpotBugs integration workflow
- "SonarQube integration" - SonarQube quality metrics
- "static analysis pipeline" - Multi-tool static analysis
- "quality report aggregation" - Unified quality reporting

**Note**: This skill can also be manually invoked via `/quality-gate-orchestrator` command. Supports execution strategies: `sequential`, `parallel`, `hybrid`.

---

## Core Patterns

### Pattern 1: Sequential Quality Checks (Fail-Fast)

**Use Case**: Pre-merge validation requiring fast feedback.

**Execution Order** (stop on first failure):
1. **Spotless** (auto-format) - 5s
2. **Checkstyle** (code style) - 10s
3. **ArchUnit** (architecture rules) - 15s
4. **PMD** (code smells) - 20s
5. **SpotBugs** (bug patterns) - 30s
6. **JaCoCo** (coverage threshold) - 40s

**When to Use**: Local pre-commit hooks, developer workstation, fast feedback loops (< 2 minutes).

See [tool-configs.md](knowledge/tool-configs.md#pattern-1-sequential-quality-checks-fail-fast---gradle-template) for complete Gradle task template.

### Pattern 2: Parallel Quality Checks (Concurrent Execution)

**Use Case**: CI/CD pipelines with multiple runners.

**Execution** (all tools run concurrently):
```
| Checkstyle (10s) | PMD (20s) | SpotBugs (30s) | ArchUnit (15s) |
                    -> wait for all -> Aggregate Results
```

**When to Use**: GitHub Actions with multiple runners, GitLab CI parallel stages, max throughput (< 1 minute total).

See [tool-configs.md](knowledge/tool-configs.md#pattern-2-parallel-quality-checks---github-actions) for complete GitHub Actions template.

### Pattern 3: Fail-Fast Strategy (Stop on Critical)

**Use Case**: Prevent wasting resources on known failures.

**Severity Mapping**:
- BLOCKER -> Exit code 1 (immediate stop)
- CRITICAL -> Exit code 1 (immediate stop)
- MAJOR -> Continue (report only)
- MINOR -> Continue (report only)
- INFO -> Ignore

**When to Use**: Resource-constrained CI environments, large codebases (> 100k LOC), pre-merge branch validation.

See [tool-configs.md](knowledge/tool-configs.md#pattern-3-fail-fast-strategy---gradle-template) for complete Gradle task template.

### Pattern 4: Report Aggregation (Unified Dashboard)

**Use Case**: Generate single HTML report combining all tool outputs.

**Report Structure**: Executive Summary (Quality Score, Blocker/Critical Issues, Coverage) -> Tool Details (Checkstyle, PMD, SpotBugs, ArchUnit, JaCoCo) -> Recommendations.

**When to Use**: Weekly team quality reviews, management reporting, quality trend tracking.

See [tool-configs.md](knowledge/tool-configs.md#pattern-4-report-aggregation---gradle-template) for complete Gradle task template.

### Pattern 5: GitHub Actions Integration (CI/CD Pipeline)

**Use Case**: Production-ready quality gate for pull requests.

Complete sequential workflow: Spotless -> Checkstyle -> ArchUnit -> PMD -> SpotBugs -> Tests + Coverage -> SonarQube (main only).

See [tool-configs.md](knowledge/tool-configs.md#pattern-5-github-actions-integration---complete-workflow) for complete GitHub Actions YAML.

### Pattern 6: GitLab CI Integration (Alternative CI/CD)

**Use Case**: GitLab-hosted projects and self-hosted runners.

Complete pipeline with stages: format -> style -> architecture -> analysis -> coverage -> sonar.

See [tool-configs.md](knowledge/tool-configs.md#pattern-6-gitlab-ci-integration---complete-pipeline) for complete GitLab CI YAML.

---

## Tool-Specific Configurations

See [tool-configs.md](knowledge/tool-configs.md#tool-specific-configurations) for complete configuration blocks for all tools. Summary:

| Tool | Location | Key Config |
|------|----------|-----------|
| **Checkstyle** | `config/checkstyle/checkstyle.xml` | v10.12.5, maxWarnings=0, ignoreFailures=false |
| **PMD** | `config/pmd/ruleset.xml` | v7.0.0, custom ruleset only |
| **SpotBugs** | `config/spotbugs/exclude.xml` | MAX effort, LOW confidence, FindSecBugs 1.13.0 |
| **ArchUnit** | `ArchitectureTest.java` | Layer violations, @Transactional placement, field injection |
| **JaCoCo** | build.gradle.kts | v0.8.11, 80% overall, 60% per class |
| **SonarQube** | sonar-project.properties | Coverage >=80%, Duplication <=3%, Rating A |

---

## Violation Severity Mapping

| Tool | Priority/Category | Severity | Action |
|------|-------------------|----------|--------|
| Checkstyle | All violations | BLOCKER | Fail build |
| ArchUnit | All test failures | BLOCKER | Fail build |
| PMD | Priority 1 | BLOCKER | Fail build |
| PMD | Priority 2 | CRITICAL | Fail build |
| PMD | Priority 3 | MAJOR | Report only |
| SpotBugs | SECURITY | BLOCKER | Fail build |
| SpotBugs | CORRECTNESS | CRITICAL | Fail build |
| SpotBugs | BAD_PRACTICE | MAJOR | Report only |
| SpotBugs | PERFORMANCE | MINOR | Report only |
| JaCoCo | < 80% coverage | MAJOR | Fail build |
| SonarQube | Quality Gate fail | CRITICAL | Block merge |

---

## Common Mistakes

### Mistake 1: Running Tools in Wrong Order
Run fast tools first (Checkstyle 10s) before slow tools (SpotBugs 30s). If Checkstyle fails, you save 30s.

### Mistake 2: Ignoring Tool Failures in CI
Never use `continue-on-error: true` for quality tools -- issues accumulate unnoticed.

### Mistake 3: Running All Tools on Every Commit
SonarQube only on main/master (2+ minutes, unnecessary for feature branches).

### Mistake 4: Duplicate Tool Coverage
Checkstyle handles imports, PMD handles logic. Disable overlapping rules.

### Mistake 5: Not Caching Tool Results
Use Gradle build cache and `actions/cache@v4` for `~/.gradle/caches`.

---

## Rationalization Table

| Tool | Purpose | Unique Value | Keep? |
|------|---------|-------------|-------|
| Checkstyle | Code style | Fast (10s), standard enforcement | Yes |
| PMD | Code smells | Java-specific patterns | Yes |
| SpotBugs | Bug patterns | Security (FindSecBugs plugin) | Yes |
| ArchUnit | Architecture rules | Layer violations | Yes |
| JaCoCo | Code coverage | Test quality metric | Yes |
| SonarQube | Aggregate quality | Trend tracking, dashboards | Yes |

**Decision**: Keep all tools, but disable overlapping rules between Checkstyle/PMD, run SonarQube on main only.

---

## Success Metrics

### Local Development
- Pre-commit hook: < 60s
- Developer feedback: 100% violations caught before push

### CI/CD Pipeline
- Sequential: < 3 minutes
- Parallel: < 1 minute
- False positive rate: < 5%

### Code Quality Targets
- Checkstyle/ArchUnit violations: 0 (always)
- PMD P1/P2 violations: 0
- SpotBugs BLOCKER/CRITICAL: 0
- Code coverage: >= 80%
- SonarQube Quality Gate: PASSED

---

## Related Rules

- `.agent/rules/foundation/F04-architecture-rules.md` - ArchUnit enforcement
- `.agent/rules/quality-tools/Q01-checkstyle-rules.md` - Code style standards
- `.agent/rules/quality-tools/Q02-pmd-rules.md` - Code smell detection
- `.agent/rules/quality-tools/Q03-spotbugs-rules.md` - Bug pattern detection

### Coordinated Quality Skills

- **[concurrency-safety-auditor](../../extended/quality/concurrency-safety-auditor/SKILL.md)** - Concurrency safety audit (SpotBugs custom detectors)
- **[spring-pattern-checker](../../extended/quality/spring-pattern-checker/SKILL.md)** - Spring pattern validation (@Transactional placement)
- **[naming-convention-checker](../../extended/quality/naming-convention-checker/SKILL.md)** - Naming convention checking (SmartAdmin standard)
- **[archunit-test-generator](../../foundation/backend/archunit-test-generator/SKILL.md)** - Architecture test generation

### Coordination Modes

- **Sequential**: Checkstyle -> PMD -> SpotBugs -> ArchUnit (fail-fast)
- **Parallel**: 4 tools concurrent (CI/CD, max throughput)
- **Selective**: Execute based on Git diff scope (incremental checks)

**Skill Positioning**: This is a **Quality Gate Orchestrator** -- coordinates tool execution, does not perform checks itself.

---

## Template Files

1. **quality-gate-github-actions.yml** - Complete GitHub Actions workflow
2. **quality-gate-gitlab-ci.yml** - Complete GitLab CI pipeline
3. **quality-gate-sequential.gradle.kts** - Sequential Gradle task
4. **quality-gate-parallel.gradle.kts** - Parallel Gradle task
5. **pre-commit-hook.sh** - Git pre-commit hook script
6. **quality-report-template.html** - HTML report template

**Location**: `.claude/skills/quality-gate-orchestrator/assets/templates/`

---

## Version History

- **1.0.0** (2026-01-25): Initial release - Sequential, parallel, fail-fast patterns
