---
trigger: on_demand
description: GitHub Actions CI/CD 管道配置
tags: [ci-cd, github-actions, sonarqube, archunit, jacoco]
required_rules:
  - rules/10-architecture-rules.md
  - rules/06-sonarqube-rules.md
  - rules/05-postgresql-basics.md
  - rules/09-mybatis-plus-core.md
last_updated: 2025-01-12
---

# GitHub Actions CI/CD 管道配置

> SmartAdmin Java 21 + Spring Boot 3.5.4 + PostgreSQL + Vavr 技術棧

## 一、完整 YAML 配置

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
  # 階段 1: 代碼質量檢查
  quality-check:
    name: Code Quality Check
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # SonarQube 需要完整歷史

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          cache: 'maven'

      - name: Checkstyle
        run: mvn checkstyle:check
        working-directory: ./smart-admin-api-java21-springboot3

      - name: PMD
        run: mvn pmd:check
        working-directory: ./smart-admin-api-java21-springboot3

      - name: SpotBugs
        run: mvn spotbugs:check
        working-directory: ./smart-admin-api-java21-springboot3

  # 階段 2: 測試（包含架構測試）
  test:
    name: Unit & Architecture Tests
    needs: quality-check
    runs-on: ubuntu-latest

    services:
      # PostgreSQL 測試數據庫
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

      # Redis 測試緩存
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
          cache: 'maven'

      - name: Run ArchUnit Tests
        run: mvn test -Dtest=ArchitectureTest
        working-directory: ./smart-admin-api-java21-springboot3
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test_db
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test

      - name: Run Unit Tests with Coverage
        run: mvn clean verify
        working-directory: ./smart-admin-api-java21-springboot3
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test_db
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test
          SPRING_DATA_REDIS_HOST: localhost
          SPRING_DATA_REDIS_PORT: 6379

      - name: Generate JaCoCo Report
        run: mvn jacoco:report
        working-directory: ./smart-admin-api-java21-springboot3

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./smart-admin-api-java21-springboot3/target/site/jacoco/jacoco.xml
          flags: unittests
          name: codecov-smartadmin

      - name: Check Coverage Threshold
        run: |
          mvn jacoco:check -Djacoco.minimum=0.80
        working-directory: ./smart-admin-api-java21-springboot3

  # 階段 3: SonarQube 分析
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
          cache: 'maven'

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: |
          mvn clean verify sonar:sonar \
            -Dsonar.projectKey=smart-admin \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
            -Dsonar.login=${{ secrets.SONAR_TOKEN }} \
            -Dsonar.java.source=21 \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
        working-directory: ./smart-admin-api-java21-springboot3

  # 階段 4: Quality Gate 檢查
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

  # 階段 5: 構建 (僅 main 分支)
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
          cache: 'maven'

      - name: Build JAR
        run: mvn clean package -DskipTests
        working-directory: ./smart-admin-api-java21-springboot3

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: smart-admin-${{ github.sha }}
          path: ./smart-admin-api-java21-springboot3/sa-admin/target/*.jar
          retention-days: 30
```

## 二、Branch Protection Rules

**Settings → Branches → Branch protection rules → Add rule**

**分支**: `main` / `master`

**必須通過的檢查**:
```yaml
required_status_checks:
  strict: true  # 要求分支為最新
  contexts:
    - quality-check
    - test
    - quality-gate
```

**合併要求**:
- 至少 1 位 Reviewer 批准
- 所有 CI 檢查通過
- 所有對話已解決
- 分支與 base 同步

## 三、Quality Gate 標準

### SonarQube 質量門檻配置

```yaml
quality_gate:
  name: "SmartAdmin Quality Gate"

  conditions:
    # 新代碼覆蓋率
    - metric: coverage
      operator: LESS_THAN
      value: 80
      on_new_code: true

    # 新代碼重複率
    - metric: duplicated_lines_density
      operator: GREATER_THAN
      value: 3
      on_new_code: true

    # 阻塞級別問題
    - metric: blocker_violations
      operator: GREATER_THAN
      value: 0
      on_new_code: false

    # 嚴重級別問題
    - metric: critical_violations
      operator: GREATER_THAN
      value: 0
      on_new_code: true

    # 技術債務比率
    - metric: sqale_debt_ratio
      operator: GREATER_THAN
      value: 5
      on_new_code: true
```

## 四、核心流程總結

### CI/CD 階段
1. **代碼質量** → Checkstyle + PMD + SpotBugs
2. **架構測試** → ArchUnit（分層、命名、Vavr 規範）
3. **單元測試** → JUnit 5 + Mockito（覆蓋率 ≥ 80%）
4. **靜態分析** → SonarQube（技術債務、重複代碼）
5. **Quality Gate** → 所有標準通過才能合併

### 關鍵指標
- 測試覆蓋率 ≥ 80%
- 代碼重複率 < 3%
- 無阻塞/嚴重級別問題
- 技術債務比率 < 5%
- ArchUnit 測試全部通過

## 相關 Workflows

- [workflows/quality-gates-local-ci.md](/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.agent/workflows/quality-gates-local-ci.md) - 本地 Quality Gate 檢查與 GitLab CI/CD
