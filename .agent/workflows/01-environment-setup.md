---
trigger: on_demand
description: SmartAdmin development environment initialization
tags: [setup, environment, java-21, postgresql, vavr, docker]
required_rules:
  - rules/technology/database/D01-postgresql-basics.md
  - rules/technology/functional/P01-vavr-fundamentals.md
  - rules/technology/database/D04-mybatis-plus-core.md

execution_order:
  - step: verify_prerequisites
    description: Verify prerequisites
    commands:
      - java -version
      - ./gradlew --version
      - docker --version
    expected_output: Java 21, Gradle 8.x+, Docker installed
    validation: All commands return correct versions

  - step: start_databases
    description: Start PostgreSQL and Redis
    commands:
      - cd .agent/configs && docker-compose up -d postgres redis
    expected_output: 2 containers running
    validation: docker ps shows postgres and redis

  - step: init_database
    description: Initialize database schema
    commands:
      - docker exec -i smartadmin-postgres psql -U smartadmin -d smart_admin_v3 < .agent/configs/init-scripts/01-init.sql
    expected_output: SQL executed successfully
    validation: Table structure created

  - step: compile_project
    description: Compile project
    commands:
      - cd smart-admin-api-java21-springboot3 && ./gradlew clean compileJava
    expected_output: BUILD SUCCESSFUL
    validation: build/ directory created

  - step: run_arch_tests
    description: Run architecture tests
    commands:
      - ./gradlew :smartadmin-app:test --tests ArchitectureTest
    expected_output: Tests run, 0 failures
    validation: All ArchUnit rules passed

last_updated: 2025-01-13
---

# SmartAdmin Development Environment Initialization

This guide will walk you through setting up the SmartAdmin (Java 21 + Spring Boot 3.5.4 + PostgreSQL) development environment.

---

## 🤖 AI Execution Guide

### When to Apply This Workflow
- ✅ User first-time using the project
- ✅ User says "environment setup" / "initialization"
- ✅ Environment not ready before executing other workflows
- ✅ Development environment damaged and needs reset

### Execution Checklist
Automate all steps, confirm after each step:
- [ ] Java 21 installed and configured
- [ ] Gradle recognizes Java 21
- [ ] Docker service running normally
- [ ] PostgreSQL container started successfully
- [ ] Redis container started successfully
- [ ] Database connection working
- [ ] Project compilation successful
- [ ] ArchUnit tests passed

### Execution Flow Decision Tree
```
User request: "initialize development environment"
  ├─ 1️⃣ Check prerequisites
  │   ├─ Java 21 installed?
  │   │   ├─ NO → Prompt installation commands
  │   │   └─ YES → Continue
  │   ├─ Gradle installed? (or use Gradle Wrapper)
  │   │   ├─ NO → Use ./gradlew wrapper
  │   │   └─ YES → Continue
  │   └─ Docker installed?
  │       ├─ NO → Prompt installation commands
  │       └─ YES → Continue
  │
  ├─ 2️⃣ Start database services
  │   ├─ Execute: docker-compose up -d
  │   ├─ Wait: PostgreSQL ready (max 30 seconds)
  │   └─ Verify: docker ps shows running
  │
  ├─ 3️⃣ Initialize database
  │   ├─ Connection test: psql -c "SELECT 1"
  │   ├─ Execute initialization script
  │   └─ Verify: table structure created successfully
  │
  ├─ 4️⃣ Compile project
  │   ├─ Execute: ./gradlew clean compileJava
  │   ├─ Verify dependencies: Vavr, PostgreSQL Driver
  │   └─ Confirm: BUILD SUCCESSFUL
  │
  └─ 5️⃣ Run architecture tests
      ├─ Execute: ./gradlew :smartadmin-app:test --tests ArchitectureTest
      ├─ Verify: layered architecture correct
      ├─ Verify: Vavr dependencies correct
      └─ Confirm: 0 failures
```

### Error Handling
If any step fails:
- 🔍 Locate failed step
- 📋 View detailed logs
- 📖 Reference [java-failure-recovery.md](./java-failure-recovery.md)
- 🔧 Re-execute after fix

---

---

## I. Prerequisites Check

### 1. Verify Java Version
```bash
java -version
```
**Expected Output**: Java 21 or higher

### 2. Check Gradle Installation
```bash
./gradlew --version
```
**Expected Output**: Gradle 8.x+ and Java version 21

### 3. Check Docker (Recommended)
```bash
docker --version
docker-compose --version
```
**Purpose**: Quickly start PostgreSQL + Redis development environment

---

## II. Project Structure

### Navigate to Java 21 Version
```bash
cd smart-admin-api-java21-springboot3
```

### Key Directories
```
smart-admin-api-java21-springboot3/
├── smartadmin-common/          # Public Foundation (DTOs, utilities, config)
├── smartadmin-support/         # Business Support modules (file, dict, login, etc.)
├── smartadmin-modules/         # Business Domain
│   ├── smartadmin-system/     # System modules (employee, role, etc.)
│   ├── smartadmin-business/   # Business logic modules
│   └── smartadmin-oa/         # OA modules (notice, enterprise, etc.)
├── smartadmin-api/             # API Contract Layer
├── smartadmin-starter/         # Starter Combinations
└── smartadmin-app/             # Unified Application Entry
    └── src/main/resources/
        ├── application.yaml
        └── dev/
```

---

## III. Database Setup (PostgreSQL)

### Using Docker Compose (Recommended)

```bash
# Start services
cd .agent/configs
docker-compose up -d postgres redis

# Verify
docker-compose ps  # Confirm 2 containers running
docker-compose logs postgres  # Confirm "database system is ready"

# Connection test
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3
```

**Default Configuration**: Database `smart_admin_v3`, user `smartadmin`, password `SmartAdmin@2024`, port `5432`

---

### Local PostgreSQL Installation (Optional)

```bash
# macOS
brew install postgresql@16 && brew services start postgresql@16

# Create database
psql postgres -c "CREATE DATABASE smart_admin_v3;"
psql postgres -c "CREATE USER smartadmin WITH PASSWORD 'SmartAdmin@2024';"
psql postgres -c "GRANT ALL ON DATABASE smart_admin_v3 TO smartadmin;"
```

---

## IV. Application Configuration

### Database Configuration (smartadmin-app/src/main/resources/dev/application.yaml)

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3
    username: smartadmin
    password: SmartAdmin@2024
  data:
    redis:
      host: 127.0.0.1
      port: 6379

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO  # PostgreSQL SERIAL
```

### Verify Dependencies

```bash
./gradlew dependencies | grep -E "(postgresql|vavr)"
# Confirm: postgresql:42.7.5, vavr:0.10.4
```

---

## V. Build and Run

```bash
# Compile
cd smart-admin-api-java21-springboot3
./gradlew clean compileJava

# Package (skip tests)
./gradlew clean build -x test

# Start
./gradlew :smartadmin-app:bootRun
# or: java -jar smartadmin-app/build/libs/smartadmin-app-*.jar
```

**Verify**: http://localhost:1024, Swagger: http://localhost:1024/swagger-ui.html

---

## VI. Code Quality Checks

```bash
# Architecture tests
./gradlew :smartadmin-app:test --tests ArchitectureTest

# Complete verification (tests + coverage)
./gradlew check
```

### Run Code Style Checks
```bash
./gradlew checkstyleMain checkstyleTest
```

### Generate Test Coverage Report
```bash
./gradlew test jacocoTestReport
```

**Report Location**: `smartadmin-app/build/reports/jacoco/test/html/index.html`

**Coverage Requirement**: ≥ 80%

---

## VII. Frontend Setup (Optional)

### 1. Navigate to Frontend Directory
```bash
cd ../../smart-admin-web
```

### 2. Install Dependencies
```bash
npm install
```

### 3. Start Development Server
```bash
npm run dev
```

**Access**: http://localhost:5173

---


### Q1: Port 1024 Already in Use
**Solution**: Modify `smartadmin-app/src/main/resources/dev/application.yaml`
```yaml
server:
  port: 8080  # Change to other port
```

### Q2: PostgreSQL Connection Failed
**Check Steps**:
```bash
# 1. Confirm PostgreSQL running status
docker-compose ps postgres

# 2. View PostgreSQL logs
docker-compose logs postgres

# 3. Test connection
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3

# 4. Verify configuration file
grep -A 5 "datasource:" smartadmin-app/src/main/resources/dev/application.yaml
```

### Q3: Redis Connection Failed
**Solution**:
- Redis is optional, basic development can be disabled
- Start Redis:
  ```bash
  docker-compose up -d redis
  ```
- Or disable Redis in configuration

### Q4: Dependency Download Slow
**Solution**: Configure Gradle mirror (Aliyun)

**File**: `~/.gradle/init.gradle`
```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        mavenCentral()
    }
}
```

### Q5: ArchUnit Test Failed
**Common Causes**:
- Service layer uses `java.util.Optional` (should use `io.vavr.control.Option`)
- Field injection (should use constructor injection)
- Included MySQL driver (should use PostgreSQL)

**Solution**: Review test report and fix code according to specifications

### Q6: JSONB TypeHandler Not Working
**Confirm Steps**:
1. Entity class adds `autoResultMap = true`
   ```java
   @TableName(value = "t_order", autoResultMap = true)
   ```
2. Field adds TypeHandler
   ```java
   @TableField(typeHandler = JsonbTypeHandler.class)
   private Map<String, Object> metadata;
   ```

---

## X. Next Steps

After successful initialization:

1. **Read Development Standards**
   - `.agent/README.md` - Development standards and navigation overview
   - `.agent/rules/technology/functional/P01-vavr-fundamentals.md` - Vavr functional programming standards
   - `.agent/rules/technology/database/D02-postgresql-advanced.md` - PostgreSQL database standards
   - `.agent/rules/technology/database/D04-mybatis-plus-core.md` - MyBatis Plus integration standards

2. **Explore Codebase**
   - `smartadmin-support/module/support/` - Reusable support modules
   - `smartadmin-modules/smartadmin-business/` - Business logic modules
   - `.agent/configs/ArchitectureTest.java` - Architecture test rules

3. **Development Practice**
   - Use Vavr Option instead of null checks
   - Use Vavr Try instead of try-catch
   - Use LambdaQueryWrapper to build type-safe queries
   - PostgreSQL JSONB fields handle extended data
   - Run ArchUnit tests to ensure architectural consistency

4. **Continuous Integration**
   - Configure CI/CD flow (reference `.agent/workflows/java-ci-cd-pipeline.md`)
   - Integrate SonarQube code quality analysis
   - Set JaCoCo test coverage thresholds

---

## XI. Quick Start Script

### One-Click Start Development Environment
```bash
#!/bin/bash
# File: start-dev.sh

echo "🚀 Starting SmartAdmin Development Environment..."

# 1. Start database and cache
cd .agent/configs
docker-compose up -d postgres redis
echo "✅ PostgreSQL and Redis started"

# 2. Wait for PostgreSQL ready
echo "⏳ Waiting for PostgreSQL..."
sleep 5

# 3. Compile project
cd ../../smart-admin-api-java21-springboot3
./gradlew clean compileJava
echo "✅ Project compiled"

# 4. Start application
./gradlew :smartadmin-app:bootRun
```

**Run**:
```bash
chmod +x start-dev.sh
./start-dev.sh
```

---

## XII. Stop Environment

```bash
# Stop application (Ctrl+C)

# Stop Docker services
cd .agent/configs
docker-compose down

# Stop and delete data volumes (use with caution)
docker-compose down -v
```

---

**Initialization Complete!** 🎉

Reference Documentation:
- Development Standards & Navigation: `.agent/README.md`
- Gradle Dependencies: See `build.gradle.kts` and `libs.versions.toml`
- Docker Environment: `.agent/configs/docker-compose.yml`
- Architecture Tests: `.agent/configs/ArchitectureTest.java`
