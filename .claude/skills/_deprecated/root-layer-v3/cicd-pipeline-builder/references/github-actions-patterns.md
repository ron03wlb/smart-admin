# GitHub Actions Patterns for SmartAdmin

Complete guide to CI/CD patterns for SmartAdmin using GitHub Actions.

## Table of Contents

1. [Basic Workflow Structure](#basic-workflow-structure)
2. [Service Configuration](#service-configuration)
3. [Caching Strategies](#caching-strategies)
4. [Test Execution](#test-execution)
5. [Quality Gates](#quality-gates)
6. [Deployment Patterns](#deployment-patterns)

---

## Basic Workflow Structure

### Minimal SmartAdmin Workflow

```yaml
name: SmartAdmin CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'
      
      - name: Build with Gradle
        run: ./gradlew clean build
        working-directory: smart-admin-api-java21-springboot3
```

---

## Service Configuration

### PostgreSQL + Redis for Integration Tests

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_DB: smartadmin_test
      POSTGRES_USER: smartadmin
      POSTGRES_PASSWORD: smartadmin123
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
    ports:
      - 5432:5432
  
  redis:
    image: redis:7-alpine
    options: >-
      --health-cmd "redis-cli ping"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
    ports:
      - 6379:6379
```

**Environment variables for tests:**

```yaml
env:
  SPRING_PROFILES_ACTIVE: test
  DATABASE_URL: jdbc:postgresql://localhost:5432/smartadmin_test
  DATABASE_USERNAME: smartadmin
  DATABASE_PASSWORD: smartadmin123
  REDIS_HOST: localhost
  REDIS_PORT: 6379
```

---

## Caching Strategies

### Gradle Dependency Caching

```yaml
- name: Cache Gradle packages
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: |
      ${{ runner.os }}-gradle-
```

### Setup Java with Built-in Cache

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: 'gradle'  # Automatic caching
```

---

## Test Execution

### Unit Tests Only

```yaml
- name: Run Unit Tests
  run: ./gradlew :sa-admin:test -x integrationTest --no-daemon
  working-directory: smart-admin-api-java21-springboot3
```

### Integration Tests with Services

```yaml
- name: Run Integration Tests
  run: ./gradlew :sa-admin:integrationTest --no-daemon
  working-directory: smart-admin-api-java21-springboot3
  env:
    SPRING_PROFILES_ACTIVE: test
    DATABASE_URL: jdbc:postgresql://localhost:5432/smartadmin_test
    DATABASE_USERNAME: smartadmin
    DATABASE_PASSWORD: smartadmin123
    REDIS_HOST: localhost
    REDIS_PORT: 6379
```

### Architecture Tests

```yaml
- name: Run Architecture Tests
  run: ./gradlew :sa-admin:test --tests "*ArchitectureTest" --no-daemon
  working-directory: smart-admin-api-java21-springboot3
```

### Upload Test Results

```yaml
- name: Upload Test Results
  if: always()  # Run even if tests fail
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: |
      smart-admin-api-java21-springboot3/sa-admin/build/reports/tests/
      smart-admin-api-java21-springboot3/sa-admin/build/test-results/
    retention-days: 30
```

---

## Quality Gates

### SpotBugs Analysis

```yaml
- name: Run SpotBugs
  run: ./gradlew :sa-admin:spotbugsMain --no-daemon
  working-directory: smart-admin-api-java21-springboot3
  continue-on-error: true  # Don't fail build on warnings
```

### PMD Analysis

```yaml
- name: Run PMD
  run: ./gradlew :sa-admin:pmdMain --no-daemon
  working-directory: smart-admin-api-java21-springboot3
  continue-on-error: true
```

### Checkstyle

```yaml
- name: Run Checkstyle
  run: ./gradlew :sa-admin:checkstyleMain --no-daemon
  working-directory: smart-admin-api-java21-springboot3
  continue-on-error: true
```

### Upload Quality Reports

```yaml
- name: Upload Quality Reports
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: quality-reports
    path: |
      smart-admin-api-java21-springboot3/sa-admin/build/reports/spotbugs/
      smart-admin-api-java21-springboot3/sa-admin/build/reports/pmd/
      smart-admin-api-java21-springboot3/sa-admin/build/reports/checkstyle/
    retention-days: 30
```

---

## Deployment Patterns

### Build Docker Image

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Log in to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKER_USERNAME }}
    password: ${{ secrets.DOCKER_PASSWORD }}

- name: Build and push
  uses: docker/build-push-action@v5
  with:
    context: smart-admin-api-java21-springboot3
    push: true
    tags: smartadmin/api:latest
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### Environment-Based Deployment

```yaml
deploy-dev:
  name: Deploy to Development
  runs-on: ubuntu-latest
  needs: build-docker
  if: github.ref == 'refs/heads/develop'
  environment:
    name: development
    url: https://dev.smartadmin.example.com
  
  steps:
    - name: Deploy to Dev
      run: |
        # Deployment commands here
```

### Manual Production Deployment

```yaml
deploy-production:
  name: Deploy to Production
  runs-on: ubuntu-latest
  needs: deploy-staging
  if: github.ref == 'refs/heads/main'
  environment:
    name: production
    url: https://smartadmin.example.com
  
  steps:
    - name: Deploy to Production
      run: |
        # Production deployment
      # Manual approval required in environment settings
```

---

## Complete Multi-Job Workflow

### Matrix Strategy for Multiple Java Versions

```yaml
strategy:
  matrix:
    java: [ '21', '22' ]
    os: [ ubuntu-latest, macos-latest ]

steps:
  - uses: actions/setup-java@v4
    with:
      java-version: ${{ matrix.java }}
      distribution: 'temurin'
```

### Job Dependencies

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps: [...]
  
  test:
    needs: build  # Waits for build to complete
    runs-on: ubuntu-latest
    steps: [...]
  
  deploy:
    needs: [build, test]  # Waits for both
    runs-on: ubuntu-latest
    steps: [...]
```

---

## Best Practices

1. **Use `working-directory`** for Gradle commands when project is in subdirectory
2. **Enable caching** for Gradle dependencies and wrappers
3. **Use health checks** for service containers
4. **Upload artifacts** with `if: always()` to capture failure reports
5. **Use environments** for deployment approval workflows
6. **Set `--no-daemon`** for Gradle to avoid daemon issues in CI
7. **Use secrets** for sensitive data (Docker credentials, API keys)
8. **Separate quality jobs** to allow parallel execution
9. **Use `continue-on-error`** for non-critical quality checks
10. **Cache Docker layers** with `cache-from/cache-to` for faster builds

---

## Secrets Configuration

Required secrets in GitHub repository settings:

```
DOCKER_USERNAME       - Docker Hub username
DOCKER_PASSWORD       - Docker Hub password/token
K8S_CONFIG           - Kubernetes config for deployments (if using K8s)
DEPLOY_SSH_KEY       - SSH key for server deployments (if using SSH)
```

---

## Troubleshooting

### Issue: Tests fail with database connection errors

**Solution**: Ensure service containers have health checks and are ready:

```yaml
options: >-
  --health-cmd pg_isready
  --health-interval 10s
  --health-timeout 5s
  --health-retries 5
```

### Issue: Gradle build fails with "Permission denied"

**Solution**: Add execute permission before running gradlew:

```yaml
- name: Grant execute permission
  run: chmod +x gradlew
  working-directory: smart-admin-api-java21-springboot3
```

### Issue: Out of memory during build

**Solution**: Increase Gradle JVM memory:

```yaml
env:
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx2g -Dorg.gradle.daemon=false"
```
