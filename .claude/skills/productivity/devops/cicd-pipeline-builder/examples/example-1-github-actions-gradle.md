# Example 1: GitHub Actions CI/CD Pipeline

## Scenario
生成 GitHub Actions workflow for SmartAdmin Gradle 項目

## Input
```bash
User: "Generate GitHub Actions pipeline for SmartAdmin"
```

## Generated Output
**File:** `.github/workflows/ci.yml`

```yaml
name: SmartAdmin CI/CD

on:
  push:
    branches: [master, develop]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run ArchUnit Tests
        run: ./gradlew :smartadmin-app:test --tests ArchitectureTest

      - name: Quality Gate
        run: ./gradlew checkstyleMain pmdMain spotbugsMain

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/test-results
```

## Expected Result
- 自動化構建和測試
- PR 合併前質量檢查
- 測試結果可視化
