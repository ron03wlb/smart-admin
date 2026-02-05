---
trigger: on_demand
description: GitHub Actions CI/CD pipeline configuration
tags: [ci-cd, github-actions, sonarqube, archunit, jacoco]
required_rules:
  - rules/foundation/F04-architecture-rules.md
  - rules/workflows/W01-sonarqube-rules.md
  - rules/technology/database/D01-postgresql-basics.md
  - rules/technology/database/D04-mybatis-plus-core.md
last_updated: 2025-01-12
---

# GitHub Actions CI/CD Pipeline Configuration

> SmartAdmin Java 21 + Spring Boot 3.5.4 + PostgreSQL + Vavr Technology Stack

## I. Complete YAML Configuration

```yaml
name: Java CI/CD Pipeline

on:
  push:
    branches: [main, master, develop]
  pull_request:
    branches: [main, master, develop]

env:
  JAVA_VERSION: '21'
  JAVA_DISTRIBUTION: 'corretto'

jobs:
  # Stage 1: Code Quality Check
  quality-check:
    name: Code Quality Check
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # SonarQube needs full history

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'gradle'

      - name: Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest
        working-directory: ./smart-admin-api-java21-springboot3

      - name: PMD
        run: ./gradlew pmdMain pmdTest
        working-directory: ./smart-admin-api-java21-springboot3

      - name: SpotBugs
        run: ./gradlew spotbugsMain spotbugsTest
        working-directory: ./smart-admin-api-java21-springboot3

  # Stage 2: Tests (including Architecture Tests)
  test:
    name: Unit & Architecture Tests
    needs: quality-check
    runs-on: ubuntu-latest

    services:
      # PostgreSQL Test Database
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: test_db
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      # Redis Test Cache
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'gradle'

      - name: Run ArchUnit Tests
        run: ./gradlew :smartadmin-app:test --tests ArchitectureTest
        working-directory: ./smart-admin-api-java21-springboot3
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test_db
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test

      - name: Run Unit Tests with Coverage
        run: ./gradlew clean check
        working-directory: ./smart-admin-api-java21-springboot3
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test_db
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test
          SPRING_DATA_REDIS_HOST: localhost
          SPRING_DATA_REDIS_PORT: 6379

      - name: Generate JaCoCo Report
        run: ./gradlew jacocoTestReport
        working-directory: ./smart-admin-api-java21-springboot3

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./smart-admin-api-java21-springboot3/smartadmin-app/build/reports/jacoco/test/jacocoTestReport.xml
          flags: unittests
          name: codecov-smartadmin

      - name: Check Coverage Threshold
        run: |
          ./gradlew jacocoTestCoverageVerification
        working-directory: ./smart-admin-api-java21-springboot3

  # Stage 3: SonarQube Analysis
  sonar:
    name: SonarQube Analysis
    needs: test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'gradle'

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: |
          ./gradlew clean check sonar \
            -Dsonar.projectKey=smart-admin \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
            -Dsonar.login=${{ secrets.SONAR_TOKEN }} \
            -Dsonar.java.source=21 \
            -Dsonar.coverage.jacoco.xmlReportPaths=smartadmin-app/build/reports/jacoco/test/jacocoTestReport.xml
        working-directory: ./smart-admin-api-java21-springboot3

  # Stage 4: Quality Gate Check
  quality-gate:
    name: SonarQube Quality Gate
    needs: sonar
    runs-on: ubuntu-latest

    steps:
      - name: Check Quality Gate Status
        uses: sonarsource/sonarqube-quality-gate-action@master
        timeout-minutes: 5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}

      - name: Quality Gate Failed - Create Issue
        if: failure()
        uses: actions/github-script@v7
        with:
          script: |
            const { owner, repo } = context.repo;
            const sha = context.sha.substring(0, 7);

            await github.rest.issues.create({
              owner,
              repo,
              title: `⚠️ Quality Gate Failed - ${sha}`,
              body: `
              **Quality Gate Check Failed**

              - **Commit**: ${context.sha}
              - **Author**: ${context.actor}
              - **Branch**: ${context.ref}
              - **Workflow**: ${context.workflow}

              Please review the [SonarQube Dashboard](${{ secrets.SONAR_HOST_URL }}/dashboard?id=smart-admin) for details.

              **Common Issues:**
              - Coverage < 80%
              - Code duplicates > 3%
              - Blocker/Critical issues
              - Technical debt ratio > 5%
              `,
              labels: ['quality-gate-failed', 'ci-cd']
            });

      - name: Comment on PR
        if: failure() && github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            await github.rest.issues.createComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              body: '⚠️ **SonarQube Quality Gate Failed**\n\nPlease fix the quality issues before merging.'
            });

  # Stage 5: Build (main branch only)
  build:
    name: Build & Package
    needs: quality-gate
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'gradle'

      - name: Build JAR
        run: ./gradlew clean build -x test
        working-directory: ./smart-admin-api-java21-springboot3

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: smart-admin-${{ github.sha }}
          path: ./smart-admin-api-java21-springboot3/smartadmin-app/build/libs/*.jar
          retention-days: 30
```

## II. Branch Protection Rules

**Settings → Branches → Branch protection rules → Add rule**

**Branch**: `main` / `master`

**Required Status Checks**:
```yaml
required_status_checks:
  strict: true  # Require branch to be up to date
  contexts:
    - quality-check
    - test
    - quality-gate
```

**Merge Requirements**:
- At least 1 reviewer approval
- All CI checks passed
- All conversations resolved
- Branch in sync with base

## III. Quality Gate Standards

### SonarQube Quality Gate Configuration

```yaml
quality_gate:
  name: "SmartAdmin Quality Gate"

  conditions:
    # New code coverage
    - metric: coverage
      operator: LESS_THAN
      value: 80
      on_new_code: true

    # New code duplication rate
    - metric: duplicated_lines_density
      operator: GREATER_THAN
      value: 3
      on_new_code: true

    # Blocker level issues
    - metric: blocker_violations
      operator: GREATER_THAN
      value: 0
      on_new_code: false

    # Critical level issues
    - metric: critical_violations
      operator: GREATER_THAN
      value: 0
      on_new_code: true

    # Technical debt ratio
    - metric: sqale_debt_ratio
      operator: GREATER_THAN
      value: 5
      on_new_code: true
```

## IV. Core Process Summary

### CI/CD Stages
1. **Code Quality** → Checkstyle + PMD + SpotBugs
2. **Architecture Tests** → ArchUnit (layering, naming, Vavr standards)
3. **Unit Tests** → JUnit 5 + Mockito (coverage ≥ 80%)
4. **Static Analysis** → SonarQube (technical debt, code duplication)
5. **Quality Gate** → All standards must pass before merge

### Key Metrics
- Test coverage ≥ 80%
- Code duplication < 3%
- No blocker/critical level issues
- Technical debt ratio < 5%
- All ArchUnit tests pass

## Related Workflows

- [workflows/quality-gates-local-ci.md](/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.agent/workflows/quality-gates-local-ci.md) - Local Quality Gate checks and GitLab CI/CD
