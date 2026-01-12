---
trigger: on_demand
description: 本地 Quality Gate 檢查與 GitLab CI/CD
tags: [ci-cd, local-checks, gitlab-ci, quality-gate, troubleshooting]
required_rules:
  - rules/10-architecture-rules.md
  - rules/06-sonarqube-rules.md
related_workflows:
  - workflows/github-actions-pipeline.md
last_updated: 2025-01-12
---

# 本地 Quality Gate 檢查與 GitLab CI/CD

> 提交前本地驗證 + 替代 CI/CD 方案

## 一、本地運行 CI 檢查腳本

### 完整流程（推薦）

```bash
#!/bin/bash
# 文件: run-ci-checks.sh

echo "🔍 Running CI Checks Locally..."

cd smart-admin-api-java21-springboot3

# 1. 代碼風格檢查
echo "1️⃣ Checkstyle..."
mvn checkstyle:check || exit 1

echo "2️⃣ PMD..."
mvn pmd:check || exit 1

echo "3️⃣ SpotBugs..."
mvn spotbugs:check || exit 1

# 2. 架構測試
echo "4️⃣ ArchUnit Tests..."
mvn test -Dtest=ArchitectureTest || exit 1

# 3. 單元測試 + 覆蓋率
echo "5️⃣ Unit Tests + Coverage..."
mvn clean verify || exit 1

# 4. 檢查覆蓋率門檻
echo "6️⃣ Coverage Threshold (≥80%)..."
mvn jacoco:check -Djacoco.minimum=0.80 || exit 1

# 5. SonarQube 分析（可選）
if [ -n "$SONAR_TOKEN" ]; then
  echo "7️⃣ SonarQube Scan..."
  mvn sonar:sonar \
    -Dsonar.projectKey=smart-admin \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN
fi

echo "✅ All CI checks passed!"
```

**運行方式**:
```bash
chmod +x run-ci-checks.sh
./run-ci-checks.sh
```

### 快速檢查（僅核心）

```bash
# 僅運行核心檢查
mvn clean verify checkstyle:check

# 查看覆蓋率報告
open target/site/jacoco/index.html
```

## 二、增量 vs 全量分析

| 觸發條件              | 分析類型 | 範圍              | 檢查項目                   |
|-----------------------|----------|-------------------|----------------------------|
| PR 開啟/更新          | 增量分析 | 僅變更文件        | Checkstyle + 單元測試      |
| Push 到 main/master   | 全量分析 | 整個項目          | 全部檢查 + SonarQube       |
| 定時任務 (每日)       | 全量分析 | 整個項目          | 全部檢查 + 依賴安全掃描    |
| 手動觸發 (workflow_dispatch) | 可選 | 指定範圍          | 可配置                     |

## 三、常見問題 (FAQ)

### Q1: ArchUnit 測試失敗

**常見原因**:
- Service 層使用了 `java.util.Optional`（應使用 `io.vavr.control.Option`）
- 字段注入（應使用構造函數注入）
- 引入了 MySQL 驅動（應使用 PostgreSQL）

**解決方式**:
```bash
mvn test -Dtest=ArchitectureTest
# 查看測試報告，按規範修復
```

### Q2: JaCoCo 覆蓋率不足

**查看報告**:
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

**提升覆蓋率策略**:
- 為 Service 層添加單元測試
- 使用 Mockito 模擬依賴
- 覆蓋異常分支

### Q3: SonarQube Quality Gate 失敗

**診斷步驟**:
1. 訪問 SonarQube Dashboard
2. 查看 **Issues** 標籤
3. 按優先級修復（Blocker > Critical > Major）

**常見問題及解決**:
- **代碼重複 > 3%**: 提取公共方法
- **認知複雜度過高**: 重構大函數
- **測試覆蓋率 < 80%**: 補充測試

### Q4: PostgreSQL 測試連接失敗

**方案一: GitHub Actions Services（CI 環境推薦）**
```yaml
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_DB: test_db
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - 5432:5432
```

**方案二: Testcontainers（本地開發推薦）**
```java
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### Q5: Maven 依賴下載慢

**配置國內鏡像** (`~/.m2/settings.xml`):
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

## 四、GitLab CI/CD 配置

### 完整 .gitlab-ci.yml

```yaml
# .gitlab-ci.yml
image: maven:3.9-eclipse-temurin-21

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository

stages:
  - quality
  - test
  - sonar
  - build

quality-check:
  stage: quality
  script:
    - cd smart-admin-api-java21-springboot3
    - mvn checkstyle:check pmd:check spotbugs:check

test:
  stage: test
  services:
    - postgres:16-alpine
  variables:
    POSTGRES_DB: test_db
    POSTGRES_USER: test
    POSTGRES_PASSWORD: test
  script:
    - cd smart-admin-api-java21-springboot3
    - mvn clean verify
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml
    paths:
      - target/site/jacoco

sonarqube:
  stage: sonar
  script:
    - cd smart-admin-api-java21-springboot3
    - mvn sonar:sonar -Dsonar.projectKey=smart-admin
  only:
    - main
    - develop

build:
  stage: build
  script:
    - cd smart-admin-api-java21-springboot3
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - smart-admin-api-java21-springboot3/sa-admin/target/*.jar
    expire_in: 30 days
  only:
    - main
```

### GitLab vs GitHub Actions 對比

| 功能                  | GitHub Actions              | GitLab CI/CD               |
|-----------------------|-----------------------------|----------------------------|
| **配置文件**          | `.github/workflows/*.yml`   | `.gitlab-ci.yml`           |
| **Services 定義**     | `services` (job 級別)       | `services` (job 級別)      |
| **Cache**             | `actions/cache`             | `cache` (內建)             |
| **Artifacts**         | `actions/upload-artifact`   | `artifacts` (內建)         |
| **條件執行**          | `if: github.ref == 'refs/heads/main'` | `only: [main]` |

## 五、持續改進建議

### 定期審查
- **每週**: 查看 SonarQube 技術債務趨勢
- **每月**: 更新架構測試規則
- **每季**: 評估 Quality Gate 標準是否合理

### 優化方向
- 提升測試覆蓋率（目標 > 85%）
- 減少代碼重複（目標 < 2%）
- 優化構建時間（目標 < 5 分鐘）
- 增加集成測試覆蓋

### 團隊協作
- 定期分享 CI/CD 最佳實踐
- 記錄常見問題解決方案
- 建立代碼審查 Checklist
- 自動化更多質量檢查

## 相關 Workflows

- [workflows/github-actions-pipeline.md](/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.agent/workflows/github-actions-pipeline.md) - GitHub Actions CI/CD 管道配置
