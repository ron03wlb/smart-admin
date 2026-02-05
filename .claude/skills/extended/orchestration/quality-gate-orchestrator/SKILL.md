---
name: quality-gate-orchestrator
description: [P1 - Extended] Automate quality gates orchestrating ArchUnit, Checkstyle, PMD, SpotBugs, JaCoCo, SonarQube. Use when running comprehensive quality checks.
---

﻿# quality-gate-orchestrator

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
- "ArchUnit integration" - Context: integrate ArchUnit with quality pipeline
- "Checkstyle pipeline" - Context: Checkstyle automation in CI/CD
- "PMD automation" - Context: automated PMD checks
- "SpotBugs workflow" - Context: SpotBugs integration workflow
- "SonarQube integration" - Context: SonarQube quality metrics
- "static analysis pipeline" - Context: multi-tool static analysis
- "multi-tool validation" - Context: orchestrating multiple quality tools
- "quality report aggregation" - Context: unified quality reporting

**Phrase Patterns**:
- "Setup [quality tool] pipeline" - Example: "Setup ArchUnit quality gate pipeline"
- "Generate [CI/CD] quality checks" - Example: "Generate GitHub Actions quality checks"
- "Orchestrate [quality tools]" - Example: "Orchestrate Checkstyle, PMD, and SpotBugs"

**Example User Requests**:
```
User: "Setup quality gate with ArchUnit, Checkstyle, and PMD"
User: "Generate pre-commit checks for SmartAdmin project"
User: "Create CI/CD quality pipeline with parallel execution"
User: "Orchestrate all quality tools with fail-fast strategy"
User: "Generate quality report aggregation for pull requests"
```

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

**Gradle Task Template**:
```kotlin
// build.gradle.kts
tasks.register("qualityGateSequential") {
    description = "Run quality checks sequentially (fail-fast)"
    group = "verification"

    doLast {
        println("🔍 Quality Gate: Sequential Validation")

        // Step 1: Auto-format (always fix first)
        exec {
            commandLine("./gradlew", "spotlessApply")
        }

        // Step 2: Code style
        exec {
            commandLine("./gradlew", ":smartadmin-app:checkstyleMain", ":smartadmin-app:checkstyleTest")
            isIgnoreExitValue = false  // Fail-fast
        }

        // Step 3: Architecture rules (CRITICAL)
        exec {
            commandLine("./gradlew", ":smartadmin-app:test", "--tests", "*ArchitectureTest")
            isIgnoreExitValue = false
        }

        // Step 4: Code smells
        exec {
            commandLine("./gradlew", ":smartadmin-app:pmdMain")
            isIgnoreExitValue = false
        }

        // Step 5: Bug patterns
        exec {
            commandLine("./gradlew", ":smartadmin-app:spotbugsMain")
            isIgnoreExitValue = false
        }

        // Step 6: Coverage threshold
        exec {
            commandLine("./gradlew", ":smartadmin-app:jacocoTestCoverageVerification")
            isIgnoreExitValue = false
        }

        println("✅ All quality gates passed!")
    }
}
```

**When to Use**:
- Local pre-commit hooks
- Developer workstation validation
- Fast feedback loops (< 2 minutes)

### Pattern 2: Parallel Quality Checks (Concurrent Execution)

**Use Case**: CI/CD pipelines with multiple runners.

**Execution** (all tools run concurrently):
```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Checkstyle  │    PMD      │  SpotBugs   │   ArchUnit  │
│   (10s)     │   (20s)     │   (30s)     │   (15s)     │
└─────────────┴─────────────┴─────────────┴─────────────┘
                    ↓ (wait for all)
              ┌──────────────┐
              │ Aggregate    │
              │ Results      │
              └──────────────┘
```

**GitHub Actions Template**:
```yaml
name: Quality Gate - Parallel

jobs:
  checkstyle:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Checkstyle
        run: ./gradlew :smartadmin-app:checkstyleMain
      - uses: actions/upload-artifact@v4
        with:
          name: checkstyle-report
          path: smartadmin-app/build/reports/checkstyle/

  pmd:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run PMD
        run: ./gradlew :smartadmin-app:pmdMain
      - uses: actions/upload-artifact@v4
        with:
          name: pmd-report
          path: smartadmin-app/build/reports/pmd/

  spotbugs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run SpotBugs
        run: ./gradlew :smartadmin-app:spotbugsMain
      - uses: actions/upload-artifact@v4
        with:
          name: spotbugs-report
          path: smartadmin-app/build/reports/spotbugs/

  archunit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run ArchUnit Tests
        run: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
      - uses: actions/upload-artifact@v4
        with:
          name: archunit-report
          path: smartadmin-app/build/reports/tests/

  aggregate:
    needs: [checkstyle, pmd, spotbugs, archunit]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
      - name: Generate Aggregated Report
        run: ./generate-quality-report.sh
```

**When to Use**:
- GitHub Actions with multiple runners
- GitLab CI parallel stages
- Maximum throughput (< 1 minute total)

### Pattern 3: Fail-Fast Strategy (Stop on Critical)

**Use Case**: Prevent wasting resources on known failures.

**Severity Mapping**:
```
BLOCKER    → Exit code 1 (immediate stop)
CRITICAL   → Exit code 1 (immediate stop)
MAJOR      → Continue (report only)
MINOR      → Continue (report only)
INFO       → Ignore
```

**Implementation**:
```kotlin
tasks.register("qualityGateFailFast") {
    description = "Run quality checks with fail-fast on BLOCKER/CRITICAL"
    group = "verification"

    val severityThreshold = "CRITICAL"  // Configurable

    doLast {
        val toolResults = mutableMapOf<String, Int>()

        // Run each tool and capture exit code
        listOf(
            "checkstyleMain" to "BLOCKER",
            "test --tests *ArchitectureTest" to "BLOCKER",
            "pmdMain" to "CRITICAL",
            "spotbugsMain" to "CRITICAL",
            "jacocoTestCoverageVerification" to "MAJOR"
        ).forEach { (task, severity) ->
            val result = exec {
                commandLine("./gradlew", ":smartadmin-app:$task")
                isIgnoreExitValue = true
            }

            toolResults[task] = result.exitValue

            if (result.exitValue != 0 && severity in listOf("BLOCKER", "CRITICAL")) {
                throw GradleException("❌ Quality gate failed: $task (severity: $severity)")
            }
        }

        // Report non-critical failures
        toolResults.filterValues { it != 0 }.forEach { (task, code) ->
            println("⚠️  Warning: $task exited with code $code")
        }
    }
}
```

**When to Use**:
- Resource-constrained CI environments
- Large codebases (> 100k LOC)
- Pre-merge branch validation

### Pattern 4: Report Aggregation (Unified Dashboard)

**Use Case**: Generate single HTML report combining all tool outputs.

**Report Structure**:
```
quality-report.html
├── Executive Summary
│   ├── Quality Score: 85/100
│   ├── Blocker Issues: 0
│   ├── Critical Issues: 2
│   └── Coverage: 78%
├── Tool Details
│   ├── Checkstyle (15 violations)
│   ├── PMD (8 violations)
│   ├── SpotBugs (3 violations)
│   ├── ArchUnit (0 violations)
│   └── JaCoCo (78% coverage)
└── Recommendations
    └── Fix critical PMD issues first
```

**Gradle Task Template**:
```kotlin
tasks.register("generateQualityReport") {
    description = "Generate aggregated quality report"
    group = "reporting"

    dependsOn(
        ":smartadmin-app:checkstyleMain",
        ":smartadmin-app:pmdMain",
        ":smartadmin-app:spotbugsMain",
        ":smartadmin-app:test",
        ":smartadmin-app:jacocoTestReport"
    )

    doLast {
        val reportDir = file("build/reports/quality-gate")
        reportDir.mkdirs()

        val htmlReport = reportDir.resolve("quality-report.html")

        htmlReport.writeText("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>SmartAdmin Quality Gate Report</title>
                <style>
                    /* CSS from assets/templates/report-template.css */
                </style>
            </head>
            <body>
                <h1>Quality Gate Report</h1>
                <section id="summary">
                    <!-- Aggregate metrics -->
                </section>
                <section id="checkstyle">
                    <!-- Parse build/reports/checkstyle/main.xml -->
                </section>
                <section id="pmd">
                    <!-- Parse build/reports/pmd/main.xml -->
                </section>
                <section id="spotbugs">
                    <!-- Parse build/reports/spotbugs/main.xml -->
                </section>
                <section id="archunit">
                    <!-- Parse build/test-results/test/*.xml -->
                </section>
                <section id="jacoco">
                    <!-- Parse build/reports/jacoco/test/jacocoTestReport.xml -->
                </section>
            </body>
            </html>
        """.trimIndent())

        println("✅ Quality report generated: ${htmlReport.absolutePath}")
    }
}
```

**When to Use**:
- Weekly team quality reviews
- Management reporting
- Quality trend tracking

### Pattern 5: GitHub Actions Integration (CI/CD Pipeline)

**Use Case**: Production-ready quality gate for pull requests.

**Complete Workflow** (see `assets/templates/quality-gate-github-actions.yml`):
```yaml
name: Quality Gate

on:
  pull_request:
    branches: [main, master, develop]
  push:
    branches: [main, master]

jobs:
  quality-gate:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for SonarQube

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      # Sequential execution (fail-fast)
      - name: 1️⃣ Code Formatting
        run: ./gradlew spotlessCheck
        working-directory: smart-admin-api-java21-springboot3

      - name: 2️⃣ Checkstyle
        run: ./gradlew :smartadmin-app:checkstyleMain :smartadmin-app:checkstyleTest
        working-directory: smart-admin-api-java21-springboot3

      - name: 3️⃣ Architecture Tests (CRITICAL)
        run: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
        working-directory: smart-admin-api-java21-springboot3

      - name: 4️⃣ PMD
        run: ./gradlew :smartadmin-app:pmdMain
        working-directory: smart-admin-api-java21-springboot3

      - name: 5️⃣ SpotBugs
        run: ./gradlew :smartadmin-app:spotbugsMain
        working-directory: smart-admin-api-java21-springboot3

      - name: 6️⃣ Unit Tests + Coverage
        run: ./gradlew :smartadmin-app:test :smartadmin-app:jacocoTestReport
        working-directory: smart-admin-api-java21-springboot3

      - name: 7️⃣ Coverage Verification (≥80%)
        run: ./gradlew :smartadmin-app:jacocoTestCoverageVerification
        working-directory: smart-admin-api-java21-springboot3

      - name: 8️⃣ SonarQube Analysis
        if: github.event_name == 'push'  # Only on main/master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: ./gradlew :smartadmin-app:sonar
        working-directory: smart-admin-api-java21-springboot3

      - name: Upload Quality Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: quality-reports
          path: |
            smart-admin-api-java21-springboot3/smartadmin-app/build/reports/
          retention-days: 30

      - name: Comment PR with Results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            // Parse reports and comment on PR
            const fs = require('fs');
            const checkstyle = fs.readFileSync('smartadmin-app/build/reports/checkstyle/main.xml');
            // ... aggregate and format results
```

**When to Use**:
- All pull requests
- Main/master branch protection
- Release quality validation

### Pattern 6: GitLab CI Integration (Alternative CI/CD)

**Complete Pipeline** (see `assets/templates/quality-gate-gitlab-ci.yml`):
```yaml
image: gradle:8.11-jdk21

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false -Dorg.gradle.caching=true"

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .gradle/wrapper
    - .gradle/caches

stages:
  - format
  - style
  - architecture
  - analysis
  - coverage
  - sonar

spotless:
  stage: format
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew spotlessCheck

checkstyle:
  stage: style
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew :smartadmin-app:checkstyleMain :smartadmin-app:checkstyleTest
  artifacts:
    reports:
      junit: smart-admin-api-java21-springboot3/smartadmin-app/build/reports/checkstyle/*.xml
    paths:
      - smart-admin-api-java21-springboot3/smartadmin-app/build/reports/checkstyle/
    expire_in: 1 week

archunit:
  stage: architecture
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
  artifacts:
    when: always
    reports:
      junit: smart-admin-api-java21-springboot3/smartadmin-app/build/test-results/test/*.xml

pmd:
  stage: analysis
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew :smartadmin-app:pmdMain
  artifacts:
    paths:
      - smart-admin-api-java21-springboot3/smartadmin-app/build/reports/pmd/

spotbugs:
  stage: analysis
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew :smartadmin-app:spotbugsMain
  artifacts:
    paths:
      - smart-admin-api-java21-springboot3/smartadmin-app/build/reports/spotbugs/

jacoco:
  stage: coverage
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew :smartadmin-app:test :smartadmin-app:jacocoTestReport
    - ./gradlew :smartadmin-app:jacocoTestCoverageVerification
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      coverage_report:
        coverage_format: cobertura
        path: smart-admin-api-java21-springboot3/smartadmin-app/build/reports/jacoco/test/jacocoTestReport.xml

sonarqube:
  stage: sonar
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew :smartadmin-app:sonar -Dsonar.projectKey=smart-admin
  only:
    - main
    - master
    - develop
```

**When to Use**:
- GitLab-hosted projects
- Self-hosted GitLab runners
- Enterprise GitLab deployments

---

## Tool-Specific Configurations

### Checkstyle Configuration

**Location**: `smart-admin-api-java21-springboot3/config/checkstyle/checkstyle.xml`

**Key Rules**:
- `AvoidStarImport` (BLOCKER)
- `NeedBraces` (CRITICAL)
- `WhitespaceAround` (MAJOR)
- `TypeName` (MAJOR)

**Gradle Integration**:
```kotlin
configure<CheckstyleExtension> {
    toolVersion = "10.12.5"
    isIgnoreFailures = false
    maxWarnings = 0
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
```

### PMD Configuration

**Location**: `smart-admin-api-java21-springboot3/config/pmd/ruleset.xml`

**Enabled Rulesets**:
- `category/java/bestpractices.xml` (Priority 1-3)
- `category/java/errorprone.xml` (Priority 1-3)
- `category/java/performance.xml` (Priority 1-2)
- `category/java/security.xml` (All)

**Critical Rules**:
- `LooseCoupling` (P3 → CRITICAL)
- `UnusedAssignment` (P3 → MAJOR)
- `AvoidDuplicateLiterals` (P3 → MAJOR)

**Gradle Integration**:
```kotlin
configure<PmdExtension> {
    toolVersion = "7.0.0"
    isIgnoreFailures = false
    ruleSetFiles = files("${rootProject.projectDir}/config/pmd/ruleset.xml")
    ruleSets = listOf()  // Use custom ruleset only
}
```

### SpotBugs Configuration

**Location**: `smart-admin-api-java21-springboot3/config/spotbugs/exclude.xml`

**Effort Level**: `MAX`
**Confidence**: `LOW`
**Plugins**: FindSecBugs 1.13.0

**Critical Bug Patterns**:
- `NP_NULL_ON_SOME_PATH` (BLOCKER)
- `SQL_INJECTION` (BLOCKER)
- `HARD_CODE_PASSWORD` (CRITICAL)
- `OBL_UNSATISFIED_OBLIGATION` (CRITICAL)

**Exclusions** (documented in `.agent/rules/quality-tools/13-spotbugs-rules.md`):
```xml
<FindBugsFilter>
    <!-- DTO/VO pattern exclusions -->
    <Match>
        <Or>
            <Class name="~.*VO"/>
            <Class name="~.*DTO"/>
            <Class name="~.*Form"/>
        </Or>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
    </Match>

    <!-- Kafka legitimate null usage -->
    <Match>
        <Class name="net.lab1024.sa.common.mq.kafka.core.KafkaProducerServiceImpl"/>
        <Bug code="NP"/>
    </Match>
</FindBugsFilter>
```

### ArchUnit Configuration

**Location**: `smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/net/lab1024/sa/ArchitectureTest.java`

**Critical Rules** (BLOCKER severity):
- Layer dependency violations (Controller → Service → Manager → Dao)
- `@Transactional` in Service layer (MUST be in Manager only)
- Field injection (MUST use constructor injection)
- Boolean field naming (`isDeleted` → `deleted`)
- Service layer using `java.util.Optional` (MUST use `io.vavr.control.Option`)

**Execution**:
```bash
./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
```

### JaCoCo Configuration

**Coverage Thresholds**:
```kotlin
configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
}

tasks.withType<JacocoCoverageVerification> {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()  // 80% minimum
            }
        }
        rule {
            element = "CLASS"
            limit {
                minimum = "0.60".toBigDecimal()  // 60% per class
            }
        }
    }
}
```

**Exclusions**:
- Generated code (`*MapperImpl`)
- Configuration classes (`*Config`)
- Domain objects (DTO/VO/Entity)

### SonarQube Configuration

**Quality Gate Conditions**:
```properties
# sonar-project.properties
sonar.projectKey=smartadmin-v4
sonar.projectName=SmartAdmin v4.0

# Quality Gate
sonar.qualitygate.wait=true
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml

# Exclusions
sonar.exclusions=**/generated/**,**/dto/**,**/vo/**,**/entity/**

# Thresholds
sonar.coverage.minimum=80
sonar.duplicated_lines_density.maximum=3
```

**Quality Gate Metrics**:
- Coverage: ≥ 80%
- Duplicated Lines: ≤ 3%
- Maintainability Rating: A
- Reliability Rating: A
- Security Rating: A
- Code Smells: 0 Blocker/Critical

---

## Violation Severity Mapping

| Tool       | Priority/Category | Severity  | Action         |
|------------|-------------------|-----------|----------------|
| Checkstyle | All violations    | BLOCKER   | Fail build     |
| ArchUnit   | All test failures | BLOCKER   | Fail build     |
| PMD        | Priority 1        | BLOCKER   | Fail build     |
| PMD        | Priority 2        | CRITICAL  | Fail build     |
| PMD        | Priority 3        | MAJOR     | Report only    |
| SpotBugs   | SECURITY          | BLOCKER   | Fail build     |
| SpotBugs   | CORRECTNESS       | CRITICAL  | Fail build     |
| SpotBugs   | BAD_PRACTICE      | MAJOR     | Report only    |
| SpotBugs   | PERFORMANCE       | MINOR     | Report only    |
| JaCoCo     | < 80% coverage    | MAJOR     | Fail build     |
| SonarQube  | Quality Gate fail | CRITICAL  | Block merge    |

---

## Common Mistakes

### Mistake 1: Running Tools in Wrong Order

**Problem**:
```bash
# ❌ Wrong - SpotBugs runs first (slowest)
./gradlew spotbugsMain checkstyleMain
```

**Why it's wrong**: SpotBugs takes 30s, Checkstyle takes 10s. If Checkstyle fails, you wasted 30s.

**Solution**:
```bash
# ✅ Correct - Fast tools first
./gradlew checkstyleMain pmdMain spotbugsMain
```

### Mistake 2: Ignoring Tool Failures in CI

**Problem**:
```yaml
# ❌ Wrong - continue-on-error hides failures
- name: Run PMD
  run: ./gradlew pmdMain
  continue-on-error: true
```

**Why it's wrong**: Quality issues accumulate unnoticed.

**Solution**:
```yaml
# ✅ Correct - Fail on violation
- name: Run PMD
  run: ./gradlew pmdMain
  # No continue-on-error
```

### Mistake 3: Running All Tools on Every Commit

**Problem**:
```yaml
# ❌ Wrong - SonarQube on every PR (slow + expensive)
- name: SonarQube
  run: ./gradlew sonar
```

**Why it's wrong**: SonarQube takes 2+ minutes, unnecessary for feature branches.

**Solution**:
```yaml
# ✅ Correct - SonarQube only on main/master
- name: SonarQube
  if: github.ref == 'refs/heads/main'
  run: ./gradlew sonar
```

### Mistake 4: Duplicate Tool Coverage

**Problem**:
```kotlin
// ❌ Wrong - Both Checkstyle and PMD check unused imports
checkstyle.xml: <module name="UnusedImports"/>
pmd-ruleset.xml: <rule ref="category/java/codestyle.xml/UnusedImports"/>
```

**Why it's wrong**: Redundant checks slow down build.

**Solution**:
```kotlin
// ✅ Correct - Checkstyle handles imports, PMD handles logic
checkstyle.xml: <module name="UnusedImports"/>
pmd-ruleset.xml: Exclude UnusedImports rule
```

### Mistake 5: Not Caching Tool Results

**Problem**:
```yaml
# ❌ Wrong - No caching, re-analyze unchanged files
- name: SpotBugs
  run: ./gradlew spotbugsMain
```

**Why it's wrong**: SpotBugs re-analyzes all files on every run.

**Solution**:
```yaml
# ✅ Correct - Use Gradle build cache
- name: Cache Gradle packages
  uses: actions/cache@v4
  with:
    path: ~/.gradle/caches
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
```

---

## Rationalization Table

| Tool       | Purpose                  | Overlap with Others | Unique Value                     | Keep? |
|------------|--------------------------|---------------------|----------------------------------|-------|
| Checkstyle | Code style               | None                | Fast (10s), standard enforcement | ✅ Yes |
| PMD        | Code smells              | Some with SpotBugs  | Java-specific patterns           | ✅ Yes |
| SpotBugs   | Bug patterns             | Some with PMD       | Security (FindSecBugs plugin)    | ✅ Yes |
| Error Prone| Compile-time bugs        | Some with SpotBugs  | Catches at compile time          | ✅ Yes |
| ArchUnit   | Architecture rules       | None                | Layer violations                 | ✅ Yes |
| JaCoCo     | Code coverage            | None                | Test quality metric              | ✅ Yes |
| SonarQube  | Aggregate quality        | All of above        | Trend tracking, dashboards       | ✅ Yes |

**Decision**: Keep all tools, but:
- Checkstyle/PMD: Disable overlapping rules
- SpotBugs/PMD: SpotBugs for security, PMD for code smells
- SonarQube: Run on main/master only (not every PR)

---

## Success Metrics

### Local Development
- Pre-commit hook execution: < 60s
- Developer feedback: 100% of violations caught before push

### CI/CD Pipeline
- Total quality gate duration: < 3 minutes (sequential)
- Total quality gate duration: < 1 minute (parallel)
- False positive rate: < 5%

### Code Quality Trends
- Checkstyle violations: 0 (always)
- ArchUnit violations: 0 (always)
- PMD P1/P2 violations: 0
- SpotBugs BLOCKER/CRITICAL: 0
- Code coverage: ≥ 80%
- SonarQube Quality Gate: PASSED

---

## Related Rules

- `.agent/foundation/10-architecture-rules.md` - ArchUnit enforcement
- `.agent/rules/quality-tools/11-checkstyle-rules.md` - Code style standards
- `.agent/rules/quality-tools/12-pmd-rules.md` - Code smell detection
- `.agent/rules/quality-tools/13-spotbugs-rules.md` - Bug pattern detection
- `.agent/rules/quality-tools/16-jacoco-coverage-rules.md` - Coverage thresholds
- `.agent/workflows/quality-gates-local-ci.md` - Local validation workflow

### 協調的質量技能

本技能作為質量門協調器，整合以下質量檢查技能：

- **[concurrency-safety-auditor](./../../extended/quality/concurrency-safety-auditor/SKILL.md)** - 並發安全審計（SpotBugs 定制檢測器）
- **[spring-pattern-checker](./../../extended/quality/spring-pattern-checker/SKILL.md)** - Spring 模式驗證（@Transactional 位置檢查）
- **[naming-convention-checker](./../../extended/quality/naming-convention-checker/SKILL.md)** - 命名規範檢查（SmartAdmin 標準）
- **[archunit-test-generator](./../../foundation/backend/archunit-test-generator/SKILL.md)** - 架構測試生成（分層架構驗證）

### 協調模式

- **Sequential**: Checkstyle → PMD → SpotBugs → ArchUnit（順序執行，快速失敗）
- **Parallel**: 4 工具並行執行（CI/CD 環境，最大化吞吐量）
- **Selective**: 依據變更範圍選擇性執行（Git diff 增量檢查）

**技能定位**: 本技能是**質量門協調器**（Quality Gate Orchestrator），不執行檢查，僅協調質量工具執行。

---

## Template Files

### Available Templates

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
