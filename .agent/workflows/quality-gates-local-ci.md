---
trigger: on_demand
description: Local Quality Gate checks and GitLab CI/CD
tags: [ci-cd, local-checks, gitlab-ci, quality-gate, troubleshooting]
required_rules:
  - rules/foundation/F04-architecture-rules.md
  - rules/workflows/W01-sonarqube-rules.md
  - rules/quality-tools/Q01-checkstyle-rules.md
  - rules/quality-tools/Q02-pmd-rules.md
  - rules/quality-tools/Q03-spotbugs-rules.md
  - rules/quality-tools/Q04-spotless-rules.md
  - rules/quality-tools/Q05-error-prone-rules.md
  - rules/quality-tools/Q06-jacoco-coverage-rules.md
related_workflows:
  - workflows/github-actions-pipeline.md
last_updated: 2025-01-12
---

# Local Quality Gate Checks and GitLab CI/CD

> Pre-commit local validation + Alternative CI/CD solution

## I. Run CI Checks Locally

### Complete Flow (Recommended)

```bash
#!/bin/bash
# File: run-ci-checks.sh

echo "🔍 Running CI Checks Locally..."

cd smart-admin-api-java21-springboot3

# 1. Code style checks
echo "1️⃣ Checkstyle..."
./gradlew checkstyleMain checkstyleTest || exit 1

echo "2️⃣ PMD..."
./gradlew pmdMain pmdTest || exit 1

echo "3️⃣ SpotBugs..."
./gradlew spotbugsMain spotbugsTest || exit 1

# 2. Architecture tests
echo "4️⃣ ArchUnit Tests..."
./gradlew :smartadmin-app:test --tests ArchitectureTest || exit 1

# 3. Unit tests + coverage
echo "5️⃣ Unit Tests + Coverage..."
./gradlew clean check || exit 1

# 4. Check coverage threshold
echo "6️⃣ Coverage Threshold (≥80%)..."
./gradlew jacocoTestCoverageVerification || exit 1

# 5. SonarQube analysis (optional)
if [ -n "$SONAR_TOKEN" ]; then
  echo "7️⃣ SonarQube Scan..."
  ./gradlew sonar \
    -Dsonar.projectKey=smart-admin \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN
fi

echo "✅ All CI checks passed!"
```

**How to Run**:
```bash
chmod +x run-ci-checks.sh
./run-ci-checks.sh
```

### Quick Check (Core Only)

```bash
# Run core checks only
./gradlew clean check checkstyleMain

# View coverage report
open smartadmin-app/build/reports/jacoco/test/html/index.html
```

## II. Incremental vs Full Analysis

| Trigger Condition                | Analysis Type | Scope              | Check Items                           |
| ------------------------------- | ------------- | ------------------ | ------------------------------------- |
| PR opened/updated               | Incremental   | Changed files only | Checkstyle + unit tests               |
| Push to main/master             | Full          | Entire project     | All checks + SonarQube                |
| Scheduled task (daily)          | Full          | Entire project     | All checks + dependency security scan |
| Manual trigger (workflow_dispatch) | Optional   | Specified scope    | Configurable                          |

## III. Common Issues (FAQ)

### Q1: ArchUnit Test Failed

**Common Causes**:
- Service layer uses `java.util.Optional` (should use `io.vavr.control.Option`)
- Field injection (should use constructor injection)
- Included MySQL driver (should use PostgreSQL)

**Solution**:
```bash
./gradlew :smartadmin-app:test --tests ArchitectureTest
# Review test report and fix according to specifications
```

### Q2: Insufficient JaCoCo Coverage

**View Report**:
```bash
./gradlew jacocoTestReport
open smartadmin-app/build/reports/jacoco/test/html/index.html
```

**Strategies to Improve Coverage**:
- Add unit tests for Service layer
- Use Mockito to mock dependencies
- Cover exception branches

### Q3: SonarQube Quality Gate Failed

**Diagnosis Steps**:
1. Visit SonarQube Dashboard
2. View **Issues** tab
3. Fix by priority (Blocker > Critical > Major)

**Common Issues and Solutions**:
- **Code duplication > 3%**: Extract common methods
- **Cognitive complexity too high**: Refactor large functions
- **Test coverage < 80%**: Add more tests

### Q4: PostgreSQL Test Connection Failed

**Option 1: GitHub Actions Services (Recommended for CI)**
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

**Option 2: Testcontainers (Recommended for Local Development)**
```java
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### Q5: Gradle Dependency Download Slow

**Configure China Mirror** (`~/.gradle/init.gradle`):
```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        mavenCentral()
    }
}
```

## IV. GitLab CI/CD Configuration

### Complete .gitlab-ci.yml

```yaml
# .gitlab-ci.yml
image: eclipse-temurin:21-jdk

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

cache:
  paths:
    - .gradle/caches
    - .gradle/wrapper

stages:
  - quality
  - test
  - sonar
  - build

quality-check:
  stage: quality
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew checkstyleMain pmdMain spotbugsMain

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
    - ./gradlew clean check
  artifacts:
    reports:
      junit: smartadmin-app/build/test-results/test/TEST-*.xml
    paths:
      - smartadmin-app/build/reports/jacoco

sonarqube:
  stage: sonar
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew sonar -Dsonar.projectKey=smart-admin
  only:
    - main
    - develop

build:
  stage: build
  script:
    - cd smart-admin-api-java21-springboot3
    - ./gradlew clean build -x test
  artifacts:
    paths:
      - smart-admin-api-java21-springboot3/smartadmin-app/build/libs/*.jar
    expire_in: 30 days
  only:
    - main
```

### GitLab vs GitHub Actions Comparison

| Feature            | GitHub Actions                        | GitLab CI/CD          |
| ------------------ | ------------------------------------- | --------------------- |
| **Config File**    | `.github/workflows/*.yml`             | `.gitlab-ci.yml`      |
| **Services Define**| `services` (job level)                | `services` (job level)|
| **Cache**          | `actions/cache`                       | `cache` (built-in)    |
| **Artifacts**      | `actions/upload-artifact`             | `artifacts` (built-in)|
| **Conditional Execution** | `if: github.ref == 'refs/heads/main'` | `only: [main]` |

## V. Continuous Improvement Recommendations

### Regular Review
- **Weekly**: Review SonarQube technical debt trends
- **Monthly**: Update architecture test rules
- **Quarterly**: Evaluate if Quality Gate standards are reasonable

### Optimization Directions
- Increase test coverage (target > 85%)
- Reduce code duplication (target < 2%)
- Optimize build time (target < 5 minutes)
- Increase integration test coverage

### Team Collaboration
- Regularly share CI/CD best practices
- Document common issue solutions
- Establish code review checklist
- Automate more quality checks

## Related Workflows

- [workflows/github-actions-pipeline.md](/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/.agent/workflows/github-actions-pipeline.md) - GitHub Actions CI/CD pipeline configuration
