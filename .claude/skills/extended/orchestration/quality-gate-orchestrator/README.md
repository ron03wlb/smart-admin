# Quality Gate Orchestrator - Skill Documentation

## Overview

The **quality-gate-orchestrator** skill automates quality gate workflows for SmartAdmin, orchestrating multiple static analysis tools (ArchUnit, Checkstyle, PMD, SpotBugs, JaCoCo, SonarQube) into unified validation pipelines.

## Quick Start

### Generate Sequential Quality Gate

```bash
# In your conversation with Claude Code:
"Generate a sequential quality gate for pre-commit validation"
```

Claude will:
1. Create `qualityGateSequential` Gradle task in `build.gradle.kts`
2. Configure fail-fast strategy (stop on first BLOCKER violation)
3. Generate execution summary with timings

### Generate CI/CD Pipeline

```bash
# For GitHub Actions:
"Create GitHub Actions quality gate workflow"

# For GitLab CI:
"Create GitLab CI quality gate pipeline"
```

Claude will:
1. Generate complete CI/CD configuration
2. Configure artifact uploads for all reports
3. Add quality gate summary comments on PRs

## File Structure

```
quality-gate-orchestrator/
├── SKILL.md                          # Main skill documentation
├── BASELINE-TEST.md                  # Validation test scenarios
├── README.md                         # This file
└── assets/
    └── templates/
        ├── quality-gate-sequential.gradle.kts   # Sequential Gradle task
        ├── quality-gate-github-actions.yml      # GitHub Actions workflow
        ├── quality-gate-gitlab-ci.yml           # GitLab CI pipeline
        └── pre-commit-hook.sh                   # Git pre-commit hook
```

## Core Patterns

### 1. Sequential Execution (Fail-Fast)
- **Use Case**: Local development, pre-commit hooks
- **Duration**: ~90 seconds
- **Stops on**: First BLOCKER/CRITICAL violation

### 2. Parallel Execution
- **Use Case**: CI/CD pipelines with multiple runners
- **Duration**: ~35 seconds (fastest job wins)
- **Requires**: GitHub Actions matrix or GitLab CI parallel jobs

### 3. Report Aggregation
- **Use Case**: Weekly quality reviews, management reporting
- **Output**: Unified HTML report combining all tool results
- **Includes**: Quality score (0-100), violation breakdown, recommendations

## Tool Configuration

### Checkstyle
- **Config**: `config/checkstyle/checkstyle.xml`
- **Severity**: BLOCKER
- **Duration**: ~10s

### ArchUnit
- **Location**: `ArchitectureTest.java`
- **Severity**: BLOCKER
- **Duration**: ~15s

### PMD
- **Config**: `config/pmd/ruleset.xml`
- **Severity**: CRITICAL (P1-P2), MAJOR (P3)
- **Duration**: ~20s

### SpotBugs
- **Config**: `config/spotbugs/exclude.xml`
- **Severity**: BLOCKER (Security), CRITICAL (Correctness)
- **Duration**: ~30s

### JaCoCo
- **Threshold**: 80% coverage
- **Severity**: MAJOR
- **Duration**: ~40s

### SonarQube
- **Runs**: main/master branches only
- **Severity**: CRITICAL
- **Duration**: ~120s

## Quality Gate Strategies

### Pre-Commit (Local)
```
Spotless → Checkstyle → ArchUnit → Commit
(15s total)
```

### Pre-Push (Local)
```
Spotless → Checkstyle → ArchUnit → PMD → SpotBugs → Push
(75s total)
```

### Pull Request (CI)
```
All tools + Coverage verification (no SonarQube)
(90s total)
```

### Main Branch (CI)
```
All tools + Coverage + SonarQube
(210s total)
```

## Success Metrics

| Metric                    | Target   | Measurement                   |
|---------------------------|----------|-------------------------------|
| Local execution time      | < 90s    | Sequential strategy           |
| CI execution time         | < 3min   | Sequential strategy           |
| Parallel execution time   | < 1min   | GitHub Actions matrix         |
| False positive rate       | < 5%     | Weekly review                 |
| Developer satisfaction    | ≥ 4.5/5  | Quarterly survey              |

## Related Documentation

- **Rules**: `.agent/foundation/10-architecture-rules.md` (ArchUnit)
- **Rules**: `.agent/rules/quality-tools/11-checkstyle-rules.md` (Code style)
- **Rules**: `.agent/rules/quality-tools/12-pmd-rules.md` (Code smells)
- **Rules**: `.agent/rules/quality-tools/13-spotbugs-rules.md` (Bug patterns)
- **Workflows**: `.agent/workflows/quality-gates-local-ci.md`

## Testing

### Run Baseline Test

```bash
cd .claude/skills/quality-gate-orchestrator
./run-baseline-tests.sh
```

Expected output: All 5 baseline tests pass in < 180 seconds

### Manual Validation

```bash
# Test sequential quality gate
cd smart-admin-api-java21-springboot3
./gradlew qualityGateSequential

# Test report generation
./gradlew generateQualityReport
open build/reports/quality-gate/quality-report.html
```

## Troubleshooting

### Issue: "Task 'qualityGateSequential' not found"

**Solution**: Ensure task is registered in root `build.gradle.kts`:
```bash
./gradlew tasks --group verification
```

### Issue: Reports not generated

**Solution**: Force report generation with `--rerun-tasks`:
```bash
./gradlew :smartadmin-app:checkstyleMain --rerun-tasks
```

### Issue: Coverage verification fails unexpectedly

**Solution**: Check exclusion patterns in `build.gradle.kts`:
```bash
./gradlew :smartadmin-app:jacocoTestCoverageVerification --info
```

## Version History

- **1.0.0** (2026-01-25): Initial release with 6 core patterns

## Maintainers

- SmartAdmin AI Agent System
- Quality Automation Team

## License

Part of SmartAdmin v4.0.0 project
