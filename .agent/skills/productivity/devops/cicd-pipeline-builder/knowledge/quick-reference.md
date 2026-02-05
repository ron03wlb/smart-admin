# CI/CD Pipeline Builder - Quick Reference

## GitHub Actions 基本結構

```yaml
name: SmartAdmin CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build
        run: ./gradlew build
```

## 服務容器配置

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
      POSTGRES_DB: smartadmin_test
    ports:
      - 5432:5432

  redis:
    image: redis:7-alpine
    ports:
      - 6379:6379
```

## 品質門檻整合

```yaml
- name: SpotBugs
  run: ./gradlew spotbugsMain

- name: PMD
  run: ./gradlew pmdMain

- name: Checkstyle
  run: ./gradlew checkstyleMain

- name: Architecture Test
  run: ./gradlew test --tests ArchitectureTest
```

## 部署策略

| 分支 | 環境 | 觸發條件 |
|------|------|----------|
| develop | dev | 自動部署 |
| main | staging | 自動部署 |
| release/* | production | 手動審批 |

## Docker 建置

```yaml
- name: Build and Push Docker
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: registry/smartadmin:${{ github.sha }}
```
