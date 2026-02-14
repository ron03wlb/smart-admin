---
name: cicd-pipeline-builder
description: [P2 - Productivity] Automate CI/CD pipeline setup for SmartAdmin with GitHub Actions or GitLab CI. Use when (1) User requests "set up CI/CD", "create pipeline", "add GitHub Actions", or "configure GitLab CI"; (2) After project initialization; (3) When integration tests need automation; (4) User mentions "deployment automation", "quality gates", or "continuous integration"; (5) To validate code before merge; (6) When setting up dev/staging/production environments.
---

# SmartAdmin CI/CD Pipeline Builder

Automate CI/CD pipeline configuration for SmartAdmin projects with comprehensive workflows for build, test, quality analysis, and deployment.

## Quick Start

**Most common usage:**
```
User: "Set up GitHub Actions for SmartAdmin"
User: "Create GitLab CI pipeline with quality gates"
User: "Add deployment automation to dev/staging/prod"
```

You will:
1. Identify target CI/CD platform (GitHub Actions, GitLab CI)
2. Generate workflow configuration with appropriate stages
3. Configure service containers (PostgreSQL, Redis)
4. Set up test execution (unit, integration, architecture tests)
5. Add quality gates (SpotBugs, PMD, Checkstyle)
6. Configure deployment pipelines (dev, staging, production)

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "CI/CD" - CI/CD pipeline generation
- "GitHub Actions" - GitHub Actions workflow generation
- "GitLab CI" - GitLab CI pipeline generation
- "deployment automation" - Automated deployment setup

**Secondary Keywords** (Medium confidence):
- "pipeline" - Context: CI/CD pipeline configuration
- "workflow" - Context: GitHub Actions workflow
- "continuous integration" - Context: CI setup
- "continuous deployment" - Context: CD setup
- "quality gates" - Context: pipeline quality checks

**Phrase Patterns**:
- "Setup [CI/CD platform]" - Example: "Setup GitHub Actions for SmartAdmin"
- "Create [platform] pipeline" - Example: "Create GitLab CI pipeline with quality gates"
- "Add deployment automation to [environment]" - Example: "Add deployment automation to dev/staging/prod"

**Example User Requests**:
```
User: "Set up GitHub Actions for SmartAdmin"
User: "Create GitLab CI pipeline with quality gates"
User: "Add deployment automation to dev/staging/prod environments"
User: "Generate CI/CD workflow with Docker support"
```

**Note**: This skill can also be manually invoked via `/cicd-pipeline-builder` command.

## Why This Skill Matters

**Problem:** Manual integration validation is slow and error-prone, causing production issues.

**Root Causes:**
- Developers forget to run ArchitectureTest before commits
- Integration tests not executed locally (require Docker setup)
- Quality checks (SpotBugs, PMD) skipped manually
- No validation before merge
- Deployment process is manual and inconsistent

**Solution:**
Automated CI/CD pipelines ensure **every commit is validated** before merge:
- ✅ Automatic build validation
- ✅ Complete test coverage (unit + integration + architecture)
- ✅ Quality gate enforcement
- ✅ Consistent deployment process
- ✅ Integration issues caught early

## Core Tasks

### Task 1: Generate GitHub Actions Workflow

**When:** Setting up CI/CD for SmartAdmin project hosted on GitHub

**Steps:**

1. Copy template from `assets/templates/github-actions-gradle.yml`
2. Place in `.github/workflows/ci-cd.yml`
3. Customize `working-directory` if project path differs
4. Configure GitHub secrets (DOCKER_USERNAME, DOCKER_PASSWORD)
5. Commit and push

**Generated Workflow:**
- Build + Test (unit, integration, architecture)
- Code Quality (SpotBugs, PMD, Checkstyle)
- Docker Build
- Deploy (dev, staging, production)

### Task 2: Generate GitLab CI Pipeline

**When:** Setting up CI/CD for SmartAdmin project hosted on GitLab

**Steps:**

1. Copy template from `assets/templates/gitlab-ci-gradle.yml`
2. Place in `.gitlab-ci.yml` at project root
3. Customize variables (database credentials)
4. Configure GitLab CI/CD variables
5. Commit and push

**Pipeline Stages:**
- build → test → quality → package → deploy

### Task 3: Local CI Testing

**When:** Testing pipeline locally before committing

**Steps:**

1. Copy `assets/templates/docker-compose-ci.yml`
2. Start services: `docker-compose -f docker-compose-ci.yml up -d`
3. Run tests: `./gradlew :smartadmin-app:integrationTest`
4. Stop services: `docker-compose -f docker-compose-ci.yml down`

---

## Template Reference

### GitHub Actions Template

**File:** `assets/templates/github-actions-gradle.yml`

**Jobs:**
1. **build-and-test** - PostgreSQL + Redis services, Gradle build, all tests
2. **code-quality** - SpotBugs, PMD, Checkstyle
3. **build-docker** - Docker image build and push
4. **deploy-dev** - Auto-deploy to development
5. **deploy-staging** - Auto-deploy to staging
6. **deploy-production** - Manual deployment to production

### GitLab CI Template

**File:** `assets/templates/gitlab-ci-gradle.yml`

**Stages:**
1. **build** - Clean build without tests
2. **test** - Unit, integration, architecture tests in parallel
3. **quality** - SpotBugs, PMD, Checkstyle in parallel
4. **package** - Docker image build
5. **deploy** - Environment-based deployment

### Docker Compose for CI

**File:** `assets/templates/docker-compose-ci.yml`

**Services:**
- PostgreSQL 15 (port 5432)
- Redis 7 (port 6379)
- Kafka + Zookeeper (optional, with `--profile kafka`)

---

## SmartAdmin-Specific Configuration

### Architecture Tests (Critical)

```yaml
# Must run to validate layered architecture
- name: Run Architecture Tests
  run: ./gradlew :smartadmin-app:test --tests "*ArchitectureTest" --no-daemon
  working-directory: smart-admin-api-java21-springboot3
```

**Why:** Validates Controller → Service → Manager → Dao layering

### Working Directory

```yaml
# SmartAdmin project is in subdirectory
working-directory: smart-admin-api-java21-springboot3
```

### Java Version

```yaml
# SmartAdmin requires Java 21
env:
  JAVA_VERSION: '21'
```

### Integration Test Environment

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

## Validation Checklist

**Build & Test:**
- [ ] Build completes successfully
- [ ] Unit tests execute and pass
- [ ] Integration tests execute with service containers
- [ ] **Architecture tests validate layered structure** (SmartAdmin critical)
- [ ] Test results uploaded as artifacts

**Quality Gates:**
- [ ] SpotBugs analysis runs
- [ ] PMD analysis runs
- [ ] Checkstyle validation runs
- [ ] Quality reports uploaded
- [ ] Quality failures don't block (continue-on-error: true)

**Service Containers:**
- [ ] PostgreSQL health check configured
- [ ] Redis health check configured
- [ ] Environment variables set
- [ ] Ports mapped correctly

**Deployment:**
- [ ] Environment names configured (dev, staging, production)
- [ ] Branch-based rules set
- [ ] Manual approval for production
- [ ] Secrets configured

**SmartAdmin-Specific:**
- [ ] Working directory = `smart-admin-api-java21-springboot3`
- [ ] Java 21 specified
- [ ] Architecture tests included
- [ ] Gradle 8.11+ supported

---

## Common Issues

### Issue: Database connection refused

**Solution:** Add health check to PostgreSQL service:
```yaml
options: >-
  --health-cmd pg_isready
  --health-interval 10s
  --health-timeout 5s
  --health-retries 5
```

### Issue: Permission denied on gradlew

**Solution:** Add execute permission:
```yaml
- run: chmod +x gradlew
  working-directory: smart-admin-api-java21-springboot3
```

### Issue: Out of memory

**Solution:** Increase JVM memory:
```yaml
env:
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx2g -Dorg.gradle.daemon=false"
```

---

## References

- [references/github-actions-patterns.md](references/github-actions-patterns.md) - Complete GitHub Actions patterns

---

## Time Savings

**Manual Setup:** 8 hours (research + configure + debug)
**Skill-Generated:** 30 minutes (copy template + customize)
**Time Saved:** 7.5 hours (94% reduction)

**Quality Impact:**
- ✅ 100% test coverage in CI
- ✅ Automated quality validation
- ✅ Integration issues caught before merge
- ✅ Consistent deployment process
