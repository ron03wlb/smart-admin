# Quality Gate Orchestrator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: quality-gate-orchestrator (P1 - Extended/Orchestration)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| Sequential (Local) | Pre-commit validation (fail-fast) | ~90s |
| Parallel (CI/CD) | GitHub Actions parallel execution | ~35s |
| Report Aggregation | Generate unified HTML report | ~5min |
| GitHub Actions | Generate CI/CD workflow | Instant |
| GitLab CI | Generate CI/CD pipeline | Instant |

### Rapid Execution Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Select Strategy | Choose sequential/parallel/fail-fast | ~1 min |
| 2. Configure Tools | Select tools and severity thresholds | ~2 min |
| 3. Generate Config | Create Gradle task or CI/CD config | ~5 min |
| 4. First Run | Execute quality gate baseline | 1-3 min |
| 5. Fix Violations | Apply quick fixes for failures | Variable |

---

## Quality Tool Matrix (Decision Guide)

### Tool 1: Checkstyle (Code Style)

**Priority**: BLOCKER
**Duration**: ~10s
**Use When**: Enforcing code style standards

**Quick Config**:
```kotlin
checkstyle {
    toolVersion = "10.12.5"
    isIgnoreFailures = false
    maxWarnings = 0
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
```

**Common Violations**:
- Missing braces on if/else
- Unused imports
- Incorrect whitespace
- Type/method naming violations

**Time to Fix**: 2-5 min (automated with Spotless)

---

### Tool 2: ArchUnit (Architecture Rules)

**Priority**: BLOCKER
**Duration**: ~15s
**Use When**: Enforcing SmartAdmin layered architecture

**Quick Config**:
```bash
./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
```

**Critical Rules**:
- Controller → Service → Manager → Dao
- @Transactional ONLY in Manager
- Constructor injection (no @Autowired fields)
- Service must use Vavr Option (not Optional)

**Time to Fix**: 5-20 min (depends on violation type)

---

### Tool 3: PMD (Code Smells)

**Priority**: CRITICAL (P1-P2), MAJOR (P3)
**Duration**: ~20s
**Use When**: Detecting code smells and bad practices

**Quick Config**:
```kotlin
pmd {
    toolVersion = "7.0.0"
    isIgnoreFailures = false
    ruleSetFiles = files("config/pmd/ruleset.xml")
}
```

**Common Violations**:
- Unused variables/parameters
- Duplicated string literals
- Overly complex methods
- Loose coupling violations

**Time to Fix**: 5-15 min per violation

---

### Tool 4: SpotBugs (Bug Patterns + Security)

**Priority**: BLOCKER (Security), CRITICAL (Correctness)
**Duration**: ~30s
**Use When**: Finding bugs and security vulnerabilities

**Quick Config**:
```kotlin
spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}
```

**Critical Patterns**:
- Null pointer dereference
- SQL injection
- Hard-coded passwords
- Resource leak

**Time to Fix**: 10-30 min per violation

---

### Tool 5: JaCoCo (Code Coverage)

**Priority**: MAJOR
**Duration**: ~40s
**Use When**: Enforcing test coverage thresholds

**Quick Config**:
```kotlin
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { minimum = "0.80".toBigDecimal() } // 80%
        }
    }
}
```

**Thresholds**:
- Project: ≥80%
- Class: ≥60%
- Method: ≥50%

**Time to Fix**: 30-60 min (write missing tests)

---

### Tool 6: SonarQube (Aggregate Quality)

**Priority**: CRITICAL
**Duration**: ~120s
**Use When**: Main/master branch merges, release validation

**Quick Config**:
```properties
sonar.projectKey=smartadmin-v4
sonar.qualitygate.wait=true
sonar.coverage.minimum=80
```

**Quality Gate Metrics**:
- Coverage: ≥80%
- Duplicated Lines: ≤3%
- Maintainability: A
- Reliability: A
- Security: A

**Time to Fix**: Review SonarQube dashboard for recommendations

---

## Execution Strategy Selection Matrix

### Strategy 1: Sequential (Fail-Fast)

**Use When**:
- Local development
- Pre-commit hooks
- Fast feedback needed (<2 min)

**Execution Order** (stop on first failure):
1. Spotless (auto-format) - 5s
2. Checkstyle (code style) - 10s
3. ArchUnit (architecture) - 15s
4. PMD (code smells) - 20s
5. SpotBugs (bugs) - 30s
6. JaCoCo (coverage) - 40s

**Total Time**: ~90s (if all pass)

**Gradle Task**:
```kotlin
tasks.register("qualityGateSequential") {
    description = "Sequential quality gate (fail-fast)"
    doLast {
        exec { commandLine("./gradlew", "spotlessApply") }
        exec { commandLine("./gradlew", ":smartadmin-app:checkstyleMain") }
        exec { commandLine("./gradlew", ":smartadmin-app:test", "--tests", "*ArchitectureTest") }
        exec { commandLine("./gradlew", ":smartadmin-app:pmdMain") }
        exec { commandLine("./gradlew", ":smartadmin-app:spotbugsMain") }
        exec { commandLine("./gradlew", ":smartadmin-app:jacocoTestCoverageVerification") }
    }
}
```

---

### Strategy 2: Parallel (CI/CD)

**Use When**:
- GitHub Actions with matrix
- GitLab CI parallel stages
- Maximum throughput needed

**Execution**: All tools run concurrently

**Total Time**: ~35s (max of all tools)

**GitHub Actions Example**:
```yaml
jobs:
  checkstyle:
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew :smartadmin-app:checkstyleMain

  pmd:
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew :smartadmin-app:pmdMain

  spotbugs:
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew :smartadmin-app:spotbugsMain

  archunit:
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
```

---

### Strategy 3: Fail-Fast (Resource-Constrained)

**Use When**:
- CI/CD with limited resources
- Large codebase (>100k LOC)
- Need to optimize CI/CD costs

**Severity Mapping**:
- BLOCKER → Stop immediately
- CRITICAL → Stop immediately
- MAJOR → Continue (report only)
- MINOR/INFO → Ignore

**Implementation**:
```kotlin
val failFastTools = mapOf(
    "checkstyleMain" to "BLOCKER",
    "test --tests *ArchitectureTest" to "BLOCKER",
    "pmdMain" to "CRITICAL",
    "spotbugsMain" to "CRITICAL"
)

failFastTools.forEach { (task, severity) ->
    val result = exec {
        commandLine("./gradlew", ":smartadmin-app:$task")
        isIgnoreExitValue = true
    }
    if (result.exitValue != 0 && severity in listOf("BLOCKER", "CRITICAL")) {
        throw GradleException("Quality gate failed: $task")
    }
}
```

---

## Common Errors and Quick Fixes

### Error 1: Checkstyle Violations (BLOCKER)

**Symptom**: Build fails with "Checkstyle rule violations"

**Quick Fix**:
```bash
# Auto-format (fixes most violations)
./gradlew spotlessApply

# Run Checkstyle again
./gradlew :smartadmin-app:checkstyleMain
```

**Time to Fix**: 1-2 minutes

---

### Error 2: ArchUnit Failures (BLOCKER)

**Symptom**: "@Transactional in Service layer" or "Field injection detected"

**Quick Fix**: See spring-pattern-checker skill
```bash
/spring  # Diagnose violations
# Apply quick fixes from spring-pattern-checker
./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
```

**Time to Fix**: 5-20 minutes

---

### Error 3: Coverage Below Threshold (MAJOR)

**Symptom**: "Coverage is 72%, required minimum is 80%"

**Quick Fix**:
1. Identify uncovered classes: Check `build/reports/jacoco/test/html/index.html`
2. Write missing tests for critical classes
3. Re-run coverage: `./gradlew :smartadmin-app:test :smartadmin-app:jacocoTestReport`

**Time to Fix**: 30-60 minutes

---

### Error 4: SpotBugs High Priority Bugs (CRITICAL)

**Symptom**: "Null pointer dereference at line 45"

**Quick Fix**: See concurrency-safety-auditor skill for concurrency issues
```bash
# Review SpotBugs report
open smartadmin-app/build/reports/spotbugs/main.html

# Apply fixes based on pattern type
# Re-run: ./gradlew :smartadmin-app:spotbugsMain
```

**Time to Fix**: 10-30 minutes per bug

---

### Error 5: SonarQube Quality Gate Failed (CRITICAL)

**Symptom**: "Quality Gate failed: Coverage is 75% (required 80%)"

**Quick Fix**:
1. Review SonarQube dashboard for top issues
2. Fix blocker/critical issues first
3. Address code smells and duplications

**Time to Fix**: 60-120 minutes (depends on issues)

---

## CI/CD Integration Quick Templates

### GitHub Actions (Complete Workflow)

```yaml
name: Quality Gate
on: [pull_request, push]

jobs:
  quality-gate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Spotless
        run: ./gradlew spotlessCheck

      - name: Checkstyle
        run: ./gradlew :smartadmin-app:checkstyleMain

      - name: ArchUnit
        run: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"

      - name: PMD
        run: ./gradlew :smartadmin-app:pmdMain

      - name: SpotBugs
        run: ./gradlew :smartadmin-app:spotbugsMain

      - name: Coverage
        run: ./gradlew :smartadmin-app:test :smartadmin-app:jacocoTestCoverageVerification

      - name: Upload Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: quality-reports
          path: smartadmin-app/build/reports/
```

---

### GitLab CI (Complete Pipeline)

```yaml
stages:
  - format
  - style
  - architecture
  - analysis
  - coverage

spotless:
  stage: format
  script: ./gradlew spotlessCheck

checkstyle:
  stage: style
  script: ./gradlew :smartadmin-app:checkstyleMain
  artifacts:
    paths: [smartadmin-app/build/reports/checkstyle/]

archunit:
  stage: architecture
  script: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"

pmd:
  stage: analysis
  script: ./gradlew :smartadmin-app:pmdMain
  artifacts:
    paths: [smartadmin-app/build/reports/pmd/]

spotbugs:
  stage: analysis
  script: ./gradlew :smartadmin-app:spotbugsMain
  artifacts:
    paths: [smartadmin-app/build/reports/spotbugs/]

jacoco:
  stage: coverage
  script: ./gradlew :smartadmin-app:test :smartadmin-app:jacocoTestCoverageVerification
  coverage: '/Total.*?([0-9]{1,3})%/'
```

---

## Time Estimates (Production Data)

| Strategy | Tools | Total Time | Use Case |
|----------|-------|------------|----------|
| Sequential | 6 tools | ~90s | Local pre-commit |
| Parallel | 6 tools | ~35s | CI/CD with runners |
| Fail-Fast | 4 tools | ~45s | Resource-constrained |
| Report Only | 6 tools | ~5min | Weekly reviews |

**Breakdown (Sequential)**:
- Spotless: 5s
- Checkstyle: 10s
- ArchUnit: 15s
- PMD: 20s
- SpotBugs: 30s
- JaCoCo: 40s

---

## Verification Checklist

Before committing quality gate setup:

- [ ] All tool configurations present (checkstyle.xml, pmd ruleset, spotbugs exclude)
- [ ] ArchitectureTest.java includes all critical rules
- [ ] JaCoCo coverage threshold set (≥80%)
- [ ] Gradle task or CI/CD config created
- [ ] Dry-run executed successfully
- [ ] All tools return exit code 0 (no violations)
- [ ] Reports generated in `build/reports/`
- [ ] CI/CD artifacts uploaded (if applicable)

---

**See Also**:
- [Checkstyle Rules](../../../../.agent/rules/quality-tools/11-checkstyle-rules.md)
- [PMD Rules](../../../../.agent/rules/quality-tools/12-pmd-rules.md)
- [SpotBugs Rules](../../../../.agent/rules/quality-tools/13-spotbugs-rules.md)
- [ArchUnit Test Generator](../../foundation/backend/archunit-test-generator/)
- [Spring Pattern Checker](../../quality/spring-pattern-checker/)
- [Concurrency Safety Auditor](../../quality/concurrency-safety-auditor/)
