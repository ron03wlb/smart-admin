# CI/CD Pipeline Builder - Examples

## 範例 1: 完整 GitHub Actions 工作流

```yaml
name: SmartAdmin CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '21'
  GRADLE_OPTS: '-Xmx4g'

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: smartadmin
          POSTGRES_PASSWORD: smartadmin
          POSTGRES_DB: smartadmin_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: gradle

      - name: Build
        run: ./gradlew build -x test

      - name: Unit Tests
        run: ./gradlew test

      - name: Integration Tests
        run: ./gradlew integrationTest
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/smartadmin_test

      - name: Architecture Tests
        run: ./gradlew test --tests ArchitectureTest

  quality-gates:
    runs-on: ubuntu-latest
    needs: build-and-test
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: SpotBugs
        run: ./gradlew spotbugsMain

      - name: PMD
        run: ./gradlew pmdMain

      - name: Checkstyle
        run: ./gradlew checkstyleMain

      - name: JaCoCo Coverage
        run: ./gradlew jacocoTestReport

  deploy-dev:
    runs-on: ubuntu-latest
    needs: [build-and-test, quality-gates]
    if: github.ref == 'refs/heads/develop'
    steps:
      - name: Deploy to Dev
        run: echo "Deploying to dev environment"
```

---

## 範例 2: GitLab CI 管線

```yaml
stages:
  - build
  - test
  - quality
  - deploy

variables:
  GRADLE_OPTS: "-Xmx4g"

build:
  stage: build
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew build -x test
  artifacts:
    paths:
      - build/

test:
  stage: test
  image: eclipse-temurin:21-jdk
  services:
    - name: postgres:15
      alias: postgres
  variables:
    POSTGRES_DB: smartadmin_test
    POSTGRES_USER: smartadmin
    POSTGRES_PASSWORD: smartadmin
  script:
    - ./gradlew test integrationTest

quality:
  stage: quality
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew spotbugsMain pmdMain checkstyleMain
  allow_failure: true

deploy_staging:
  stage: deploy
  only:
    - main
  script:
    - echo "Deploying to staging"
```
