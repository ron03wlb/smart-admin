---
name: quality-gate-orchestrator
description: Orchestrates multi-tool quality gate checks (ArchUnit, Checkstyle, PMD, SpotBugs, JaCoCo) and generates unified reports
---

# Quality Gate Orchestrator

Orchestrates comprehensive quality gate checks for SmartAdmin project.

## Usage

```bash
/quality-gate check              # Run all quality checks
/quality-gate check --quick      # Run fast checks only (no SpotBugs)
/quality-gate report             # Generate quality report
```

## Capabilities

### 1. Quality Check Orchestration

Runs the following tools in sequence:

| Tool       | Command                      | Focus           |
| ---------- | ---------------------------- | --------------- |
| Spotless   | `./gradlew spotlessApply`    | Code formatting |
| Checkstyle | `./gradlew checkstyleMain`   | Code style      |
| PMD        | `./gradlew pmdMain`          | Code smells     |
| SpotBugs   | `./gradlew spotbugsMain`     | Bug patterns    |
| JaCoCo     | `./gradlew jacocoTestReport` | Test coverage   |

### 2. Pass Criteria

```yaml
Quality Gate Standards:
  ✅ Checkstyle:  0 errors
  ✅ PMD:         0 violations
  ✅ SpotBugs:    0 bugs
  ✅ Coverage:    ≥ 80% line, ≥ 70% branch
```

### 3. Report Generation

Generates unified report at `build/reports/quality-gate-report.md`:
- Tool-by-tool results
- Violation summaries
- Coverage metrics
- Actionable recommendations

## Workflow

```
1. Read user request for quality gate check
2. Run ./gradlew spotlessApply first to fix formatting
3. Run ./gradlew check to execute all quality tools
4. If any tool fails:
   a. Parse the error output
   b. Read corresponding rule file from .agent/rules/quality-tools/
   c. Provide specific fix recommendations
5. Generate summary report
```

## Related Rules

- [quality-tools/Q01-checkstyle-rules.md](../../rules/quality-tools/Q01-checkstyle-rules.md)
- [quality-tools/Q02-pmd-rules.md](../../rules/quality-tools/Q02-pmd-rules.md)
- [quality-tools/Q03-spotbugs-rules.md](../../rules/quality-tools/Q03-spotbugs-rules.md)
- [quality-tools/Q04-spotless-rules.md](../../rules/quality-tools/Q04-spotless-rules.md)
- [quality-tools/Q06-jacoco-coverage-rules.md](../../rules/quality-tools/Q06-jacoco-coverage-rules.md)

## Example Session

**User:** `/quality-gate check`

**AI Agent Actions:**
1. Run `./gradlew spotlessApply`
2. Run `./gradlew check`
3. If Checkstyle fails:
   - View `smartadmin-app/build/reports/checkstyle/main.html`
   - Read rule 11-checkstyle-rules.md
   - Fix violations following the patterns
4. Re-run until all pass
5. Generate summary
