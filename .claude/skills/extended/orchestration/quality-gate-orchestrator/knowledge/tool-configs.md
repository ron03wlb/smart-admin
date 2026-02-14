# Quality Gate Tool Configurations

> Extracted from SKILL.md to keep the main file focused on AI agent instructions.
> This file contains complete Gradle task templates, CI/CD pipeline configurations, and tool-specific settings.

---

## Pattern 1: Sequential Quality Checks (Fail-Fast) - Gradle Template

```kotlin
// build.gradle.kts
tasks.register("qualityGateSequential") {
    description = "Run quality checks sequentially (fail-fast)"
    group = "verification"

    doLast {
        println("Quality Gate: Sequential Validation")

        // Step 1: Auto-format (always fix first)
        exec { commandLine("./gradlew", "spotlessApply") }

        // Step 2: Code style
        exec {
            commandLine("./gradlew", ":smartadmin-app:checkstyleMain", ":smartadmin-app:checkstyleTest")
            isIgnoreExitValue = false
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

        println("All quality gates passed!")
    }
}
```

---

## Pattern 2: Parallel Quality Checks - GitHub Actions

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

---

## Pattern 3: Fail-Fast Strategy - Gradle Template

```kotlin
tasks.register("qualityGateFailFast") {
    description = "Run quality checks with fail-fast on BLOCKER/CRITICAL"
    group = "verification"

    val severityThreshold = "CRITICAL"

    doLast {
        val toolResults = mutableMapOf<String, Int>()

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
                throw GradleException("Quality gate failed: $task (severity: $severity)")
            }
        }

        toolResults.filterValues { it != 0 }.forEach { (task, code) ->
            println("Warning: $task exited with code $code")
        }
    }
}
```

---

## Pattern 4: Report Aggregation - Gradle Template

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
            <head><title>SmartAdmin Quality Gate Report</title></head>
            <body>
                <h1>Quality Gate Report</h1>
                <section id="summary"><!-- Aggregate metrics --></section>
                <section id="checkstyle"><!-- Parse build/reports/checkstyle/main.xml --></section>
                <section id="pmd"><!-- Parse build/reports/pmd/main.xml --></section>
                <section id="spotbugs"><!-- Parse build/reports/spotbugs/main.xml --></section>
                <section id="archunit"><!-- Parse build/test-results/test/*.xml --></section>
                <section id="jacoco"><!-- Parse build/reports/jacoco/test/jacocoTestReport.xml --></section>
            </body>
            </html>
        """.trimIndent())

        println("Quality report generated: ${htmlReport.absolutePath}")
    }
}
```

---

## Pattern 5: GitHub Actions Integration - Complete Workflow

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
          fetch-depth: 0

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: 1. Code Formatting
        run: ./gradlew spotlessCheck
        working-directory: smart-admin-api-java21-springboot3

      - name: 2. Checkstyle
        run: ./gradlew :smartadmin-app:checkstyleMain :smartadmin-app:checkstyleTest
        working-directory: smart-admin-api-java21-springboot3

      - name: 3. Architecture Tests (CRITICAL)
        run: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
        working-directory: smart-admin-api-java21-springboot3

      - name: 4. PMD
        run: ./gradlew :smartadmin-app:pmdMain
        working-directory: smart-admin-api-java21-springboot3

      - name: 5. SpotBugs
        run: ./gradlew :smartadmin-app:spotbugsMain
        working-directory: smart-admin-api-java21-springboot3

      - name: 6. Unit Tests + Coverage
        run: ./gradlew :smartadmin-app:test :smartadmin-app:jacocoTestReport
        working-directory: smart-admin-api-java21-springboot3

      - name: 7. Coverage Verification (>=80%)
        run: ./gradlew :smartadmin-app:jacocoTestCoverageVerification
        working-directory: smart-admin-api-java21-springboot3

      - name: 8. SonarQube Analysis
        if: github.event_name == 'push'
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
          path: smart-admin-api-java21-springboot3/smartadmin-app/build/reports/
          retention-days: 30
```

---

## Pattern 6: GitLab CI Integration - Complete Pipeline

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

---

## Tool-Specific Configurations

### Checkstyle

**Location**: `smart-admin-api-java21-springboot3/config/checkstyle/checkstyle.xml`

**Key Rules**: AvoidStarImport (BLOCKER), NeedBraces (CRITICAL), WhitespaceAround (MAJOR), TypeName (MAJOR).

```kotlin
configure<CheckstyleExtension> {
    toolVersion = "10.12.5"
    isIgnoreFailures = false
    maxWarnings = 0
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
```

### PMD

**Location**: `smart-admin-api-java21-springboot3/config/pmd/ruleset.xml`

**Enabled Rulesets**: bestpractices.xml (P1-3), errorprone.xml (P1-3), performance.xml (P1-2), security.xml (All).

**Critical Rules**: LooseCoupling (CRITICAL), UnusedAssignment (MAJOR), AvoidDuplicateLiterals (MAJOR).

```kotlin
configure<PmdExtension> {
    toolVersion = "7.0.0"
    isIgnoreFailures = false
    ruleSetFiles = files("${rootProject.projectDir}/config/pmd/ruleset.xml")
    ruleSets = listOf()
}
```

### SpotBugs

**Location**: `smart-admin-api-java21-springboot3/config/spotbugs/exclude.xml`

**Effort Level**: MAX, **Confidence**: LOW, **Plugins**: FindSecBugs 1.13.0.

**Critical Bug Patterns**: NP_NULL_ON_SOME_PATH (BLOCKER), SQL_INJECTION (BLOCKER), HARD_CODE_PASSWORD (CRITICAL), OBL_UNSATISFIED_OBLIGATION (CRITICAL).

**Exclusions** (documented in `.agent/rules/quality-tools/Q03-spotbugs-rules.md`):
```xml
<FindBugsFilter>
    <Match>
        <Or>
            <Class name="~.*VO"/>
            <Class name="~.*DTO"/>
            <Class name="~.*Form"/>
        </Or>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
    </Match>
    <Match>
        <Class name="net.lab1024.sa.common.mq.kafka.core.KafkaProducerServiceImpl"/>
        <Bug code="NP"/>
    </Match>
</FindBugsFilter>
```

### ArchUnit

**Location**: `smartadmin-app/src/test/java/net/lab1024/sa/app/ArchitectureTest.java`

**Critical Rules (BLOCKER)**: Layer dependency violations, @Transactional in Service, field injection, boolean naming, Service using java.util.Optional.

```bash
./gradlew :smartadmin-app:test --tests "*ArchitectureTest"
```

### JaCoCo

```kotlin
configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
}

tasks.withType<JacocoCoverageVerification> {
    violationRules {
        rule { limit { minimum = "0.80".toBigDecimal() } }
        rule {
            element = "CLASS"
            limit { minimum = "0.60".toBigDecimal() }
        }
    }
}
```

**Exclusions**: Generated code (*MapperImpl), Configuration classes (*Config), Domain objects (DTO/VO/Entity).

### SonarQube

```properties
sonar.projectKey=smartadmin-v4
sonar.projectName=SmartAdmin v4.0
sonar.qualitygate.wait=true
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
sonar.exclusions=**/generated/**,**/dto/**,**/vo/**,**/entity/**
sonar.coverage.minimum=80
sonar.duplicated_lines_density.maximum=3
```

**Quality Gate Metrics**: Coverage >= 80%, Duplicated Lines <= 3%, Maintainability/Reliability/Security Rating: A, Code Smells: 0 Blocker/Critical.

---

**Version**: 1.0.0
**Extracted From**: SKILL.md v1.0.0
**Last Updated**: 2026-02-06
