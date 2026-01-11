---
description: Java CI/CD Pipeline 工作流
---

## GitHub Actions 完整配置
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  # 1. 靜態分析
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'corretto'
          cache: maven
      
      - name: Checkstyle
        run: mvn checkstyle:check
      
      - name: PMD
        run: mvn pmd:check
      
      - name: SpotBugs
        run: mvn spotbugs:check

  # 2. 單元測試
  test:
    needs: analyze
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'corretto'
          cache: maven
      
      - name: Run Tests with Coverage
        run: mvn verify
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: target/site/jacoco/jacoco.xml

  # 3. SonarQube 分析
  sonar:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'corretto'
          cache: maven
      
      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          mvn -B verify sonar:sonar \
            -Dsonar.projectKey=${{ github.repository_owner }}_${{ github.event.repository.name }} \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }}

  # 4. Quality Gate 檢查
  quality-gate:
    needs: sonar
    runs-on: ubuntu-latest
    steps:
      - name: Check Quality Gate
        uses: sonarsource/sonarqube-quality-gate-action@master
        timeout-minutes: 5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      
      - name: Gate Failed - Create Issue
        if: failure()
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.create({
              owner: context.repo.owner,
              repo: context.repo.repo,
              title: '⚠️ Quality Gate Failed',
              body: `Quality Gate failed for commit ${context.sha}`,
              labels: ['quality-gate-failed']
            })
```

## PR 合併前必須通過的檢查清單
```yaml
# .github/branch-protection-rules.md
required_checks:
  - analyze          # 靜態分析全部通過
  - test             # 測試全部通過
  - quality-gate     # SonarQube Quality Gate 通過

merge_requirements:
  - 至少 1 位 Reviewer 批准
  - 所有對話已解決
  - 分支為最新狀態
```

## 增量 vs 全量分析

| 觸發條件     | 分析類型 | 範圍       |
| ------------ | -------- | ---------- |
| PR 開啟/更新 | 增量分析 | 僅變更文件 |
| Push 到 main | 全量分析 | 整個項目   |
| 手動觸發     | 可選     | 指定範圍   |