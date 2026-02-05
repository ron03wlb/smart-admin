# CI/CD Pipeline Builder - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: cicd-pipeline-builder (P2 - Productivity/DevOps)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| GitHub Actions | Setup GitHub CI/CD | ~15 min |
| GitLab CI | Setup GitLab pipeline | ~12 min |
| Jenkins Pipeline | Setup Jenkins job | ~20 min |
| Docker Build | Containerize application | ~10 min |
| Kubernetes Deploy | Deploy to K8s cluster | ~15 min |

---

## CI/CD Platform Selection

### Option 1: GitHub Actions (Recommended)

**Use When**: GitHub-hosted repositories

**Pros**:
- ✅ Free for public repos
- ✅ Built-in to GitHub
- ✅ Easy YAML syntax
- ✅ Large marketplace

**Complete Pipeline**:
```yaml
# .github/workflows/ci-cd.yml
name: SmartAdmin CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '21'
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'gradle'

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run Tests
        run: ./gradlew test

      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/'

  code-quality:
    runs-on: ubuntu-latest
    needs: build

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Checkstyle
        run: ./gradlew checkstyleMain

      - name: PMD
        run: ./gradlew pmdMain

      - name: SpotBugs
        run: ./gradlew spotbugsMain

      - name: ArchUnit
        run: ./gradlew :smartadmin-app:test --tests ArchitectureTest

  docker:
    runs-on: ubuntu-latest
    needs: [build, code-quality]
    if: github.ref == 'refs/heads/main'

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=sha,prefix={{branch}}-

      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    runs-on: ubuntu-latest
    needs: docker
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
      - name: Deploy to Kubernetes
        uses: azure/k8s-deploy@v4
        with:
          manifests: |
            k8s/deployment.yml
            k8s/service.yml
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

**Time to Setup**: 15-20 minutes

---

### Option 2: GitLab CI

**Complete Pipeline**:
```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - quality
  - deploy

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  JAVA_VERSION: "21"

before_script:
  - export GRADLE_USER_HOME=`pwd`/.gradle

cache:
  paths:
    - .gradle/wrapper
    - .gradle/caches

build:
  stage: build
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew assemble
  artifacts:
    paths:
      - build/libs/*.jar
    expire_in: 1 week

test:
  stage: test
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: build/test-results/test/TEST-*.xml
    paths:
      - build/reports/tests/

checkstyle:
  stage: quality
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew checkstyleMain
  allow_failure: false

archunit:
  stage: quality
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew :smartadmin-app:test --tests ArchitectureTest
  allow_failure: false

docker:
  stage: deploy
  image: docker:24
  services:
    - docker:24-dind
  only:
    - main
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
```

**Time to Setup**: 12-15 minutes

---

## Dockerfile Patterns

### Pattern 1: Multi-stage Build (Optimized)

```dockerfile
# Multi-stage build for smaller image
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY . .

# Build application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy only the JAR
COPY --from=builder /app/smartadmin-app/build/libs/smartadmin-app.jar app.jar

# Create non-root user
RUN groupadd -r smartadmin && useradd -r -g smartadmin smartadmin
USER smartadmin

EXPOSE 1024

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Xms2g", \
    "-Xmx4g", \
    "-jar", \
    "app.jar"]
```

**Image Size**: ~300MB (vs 800MB with full JDK)

---

### Pattern 2: Buildpacks (Cloud Native)

```bash
# Use Cloud Native Buildpacks (no Dockerfile needed)
./gradlew bootBuildImage --imageName=smartadmin-api:latest

# Result: Optimized image with automatic JVM tuning
```

---

## Kubernetes Deployment

### Pattern 1: Basic Deployment

```yaml
# k8s/deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smartadmin-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: smartadmin-api
  template:
    metadata:
      labels:
        app: smartadmin-api
    spec:
      containers:
      - name: api
        image: ghcr.io/org/smartadmin-api:latest
        ports:
        - containerPort: 1024
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: smartadmin-config
              key: db.host
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: smartadmin-secrets
              key: db.password
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 1024
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 1024
          initialDelaySeconds: 30
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: smartadmin-api
spec:
  type: LoadBalancer
  selector:
    app: smartadmin-api
  ports:
  - port: 80
    targetPort: 1024
```

---

## Environment-Specific Configurations

### Pattern: Secrets Management

**GitHub Actions Secrets**:
```yaml
jobs:
  deploy:
    steps:
      - name: Deploy
        env:
          DB_HOST: ${{ secrets.DB_HOST }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
        run: |
          kubectl create secret generic smartadmin-secrets \
            --from-literal=db.password=$DB_PASSWORD \
            --from-literal=jwt.secret=$JWT_SECRET
```

**Kubernetes ConfigMap**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: smartadmin-config
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres:5432/smartadmin
      redis:
        host: redis
        port: 6379
```

---

## Deployment Strategies

### Strategy 1: Rolling Update (Zero Downtime)

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
```

**Process**: Deploy 1 pod at a time, wait for health check

---

### Strategy 2: Blue-Green Deployment

```bash
# Deploy new version (green)
kubectl apply -f k8s/deployment-green.yml

# Test green deployment
curl http://green.smartadmin.com/actuator/health

# Switch traffic to green
kubectl patch service smartadmin-api -p '{"spec":{"selector":{"version":"green"}}}'

# Delete blue deployment
kubectl delete deployment smartadmin-api-blue
```

---

### Strategy 3: Canary Deployment

```yaml
# 10% traffic to canary
apiVersion: v1
kind: Service
metadata:
  name: smartadmin-api
spec:
  selector:
    app: smartadmin-api
  ports:
  - port: 80
    targetPort: 1024

---
# Stable version (90% traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smartadmin-api-stable
spec:
  replicas: 9
  selector:
    matchLabels:
      app: smartadmin-api
      version: stable

---
# Canary version (10% traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smartadmin-api-canary
spec:
  replicas: 1
  selector:
    matchLabels:
      app: smartadmin-api
      version: canary
```

---

## Monitoring Integration

### Pattern: Prometheus + Grafana

```yaml
# k8s/servicemonitor.yml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: smartadmin-api
spec:
  selector:
    matchLabels:
      app: smartadmin-api
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

---

## Time Estimates

| Task | Configuration | Testing | Total |
|------|--------------|---------|-------|
| GitHub Actions | 15 min | 10 min | 25 min |
| GitLab CI | 12 min | 8 min | 20 min |
| Dockerfile | 10 min | 5 min | 15 min |
| Kubernetes Deploy | 15 min | 15 min | 30 min |
| Secrets Setup | 8 min | 5 min | 13 min |

**Full CI/CD Pipeline**: 60-90 minutes

---

**See Also**:
- [DB Migration Manager](../db-migration-manager/) - Database migrations in CI/CD
- [APM Integration](../apm-integration/) - Monitor deployments
