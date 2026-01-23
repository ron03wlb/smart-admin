# SmartAdmin Project Architecture

This document provides essential context about the SmartAdmin project structure, technology stack, and build processes. **All agents should reference this document to understand the project context.**

## Technology Stack

### Backend Stack (smart-admin-api-java21-springboot3)

| Component | Version | Purpose | Rules Reference |
|-----------|---------|---------|-----------------|
| **Java** | 21 | Programming language with modern features | [01-naming-conventions.md](../../../.agent/rules/01-naming-conventions.md) |
| **Spring Boot** | 3.5.4 | Application framework | [10-architecture-rules.md](../../../.agent/rules/10-architecture-rules.md) |
| **MyBatis Plus** | 3.5.12 | ORM and database access | [09-mybatis-plus-core.md](../../../.agent/rules/09-mybatis-plus-core.md) |
| **Sa-Token** | 1.44.0 | Authentication and authorization | - |
| **PostgreSQL Driver** | 42.7.5 | Database driver | [05-postgresql-basics.md](../../../.agent/rules/05-postgresql-basics.md) |
| **Vavr** | 0.10.4 | Functional programming library | [08-vavr-fundamentals.md](../../../.agent/rules/08-vavr-fundamentals.md) |
| **Knife4j** | 4.6.0 | API documentation (Swagger UI) | - |
| **Druid** | 1.2.25 | Database connection pool | - |
| **Redisson** | 3.50.0 | Distributed Redis client with caching | - |
| **P6Spy** | 3.9.1 | SQL monitoring and logging | - |
| **Gradle** | - | Build tool with Kotlin DSL | - |

### Frontend Stack (smart-admin-web)

| Component | Version | Purpose |
|-----------|---------|---------|
| **Vue** | 3.4.27 | Progressive JavaScript framework |
| **TypeScript** | 5.6.3 | Type-safe JavaScript |
| **Vite** | 5.2.12 | Build tool and dev server |
| **Ant Design Vue** | 4.2.5 | Enterprise UI component library |
| **Pinia** | 2.1.7 | State management |
| **Vue Router** | 4.3.2 | Routing library |
| **Node.js** | >= 18 | JavaScript runtime |

### Technology Highlights

**Java 21**
- Virtual Threads for improved concurrency
- Pattern Matching for cleaner code
- Record types for immutable data classes

**Vavr 0.10.4**
- Functional containers: `Option`, `Try`, `Either`
- Replaces null checks and try-catch blocks
- See: [08-vavr-fundamentals.md](../../../.agent/rules/08-vavr-fundamentals.md)

**PostgreSQL 16** (Target Architecture)
- JSONB support for flexible document storage
- Array types for efficient data structures
- CTEs and window functions for complex queries
- See: [05-postgresql-advanced.md](../../../.agent/rules/05-postgresql-advanced.md)

**MyBatis Plus 3.5.12**
- LambdaQueryWrapper for type-safe queries (preferred)
- Automatic CRUD via BaseMapper
- See: [09-mybatis-plus-core.md](../../../.agent/rules/09-mybatis-plus-core.md)

## Module Structure

The project follows a modular monolith architecture with strict layering:

```
smart-admin-api-java21-springboot3/
├── sa-admin/           # Business modules and system functionality
│   ├── controller/     # API endpoints
│   ├── service/        # Business logic
│   ├── manager/        # Transaction and cache layer
│   ├── dao/            # Data access
│   └── domain/         # Domain objects (entity, form, vo)
│
├── sa-base/            # Infrastructure and support modules
│   ├── base-common/    # Common utilities, constants
│   ├── base-support/   # Support features (config, codegen, etc.)
│   └── base-security/  # Security configuration
│
├── sa-common/          # Shared cross-cutting concerns
│   ├── api-encrypt/    # API encryption/decryption
│   ├── cache/          # Caching abstractions
│   ├── mq/             # Message queue integration
│   └── redis-lock/     # Distributed locking
│
└── settings.gradle.kts # Gradle module configuration
```

### Module Responsibilities

**sa-admin/**
- Core business functionality
- System management features
- User-facing business modules
- Extends sa-base infrastructure

**sa-base/**
- Framework infrastructure
- Common utilities and helpers
- Support features (file upload, config management)
- Authentication and authorization setup
- No business logic - pure infrastructure

**sa-common/**
- Cross-cutting concerns
- Reusable services (caching, messaging, locking)
- Shared across multiple projects
- Technology-specific integrations

## Build Commands

All build commands run from the project root: `smart-admin-api-java21-springboot3/`

### Building the Project

```bash
# Clean and build
./gradlew clean build

# Build without tests (faster)
./gradlew build -x test

# Environment-specific build
./gradlew build -Penv=dev     # Development
./gradlew build -Penv=test    # Testing
./gradlew build -Penv=pre     # Pre-production
./gradlew build -Penv=prod    # Production
```

### Running the Application

```bash
# Run application
./gradlew :sa-admin:bootRun

# Application starts on: http://localhost:1024
# Swagger UI available at: http://localhost:1024/swagger-ui.html
```

### Testing

```bash
# Run all tests
./gradlew :sa-admin:test

# Run specific test class
./gradlew :sa-admin:test --tests ArchitectureTest
./gradlew :sa-admin:test --tests AdminApplicationTest

# Run architecture validation (CRITICAL before commits)
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Important:** `ArchitectureTest` enforces all architectural rules using ArchUnit. This test MUST pass before committing code.

## Frontend Commands

### Vue 3 Web Application (smart-admin-web/)

**Location:** `smart-admin-web/` (Vue 3 + Vite + TypeScript + Ant Design Vue)

```bash
# Install dependencies
npm install
# or
pnpm install

# Development server (http://localhost:5173)
npm run dev
# or
pnpm dev

# Production build
npm run build
# or
pnpm build

# Preview production build
npm run preview

# Type checking (TypeScript)
npm run type-check

# Lint code
npm run lint

# Format code
npm run format
```

**Frontend development server:** `http://localhost:5173`

**Note:** Frontend connects to backend API at `http://localhost:1024`

### Mobile Application (smart-app/)

**Location:** `smart-app/` (uni-app framework for multi-platform mobile)

```bash
# Install dependencies
npm install

# Development (choose platform)
npm run dev:mp-weixin     # WeChat Mini Program
npm run dev:h5            # H5 web app
npm run dev:app           # Native app

# Build for production
npm run build:mp-weixin
npm run build:h5
npm run build:app
```

**Platforms supported:** WeChat Mini Program, H5, iOS, Android

### Frontend-Backend Integration

**Backend API:** `http://localhost:1024`
**Frontend Dev:** `http://localhost:5173`
**Swagger UI:** `http://localhost:1024/swagger-ui.html`

**Typical Development Workflow:**
1. Start backend: `cd smart-admin-api-java21-springboot3 && ./gradlew :sa-admin:bootRun`
2. Start frontend: `cd smart-admin-web && npm run dev`
3. Access frontend: `http://localhost:5173`
4. Test APIs: `http://localhost:1024/swagger-ui.html`

## Application Configuration

### Profiles and Environments

The application supports multiple environment profiles:
- `dev` - Development (local)
- `test` - Testing environment
- `pre` - Pre-production (staging)
- `prod` - Production

### Key Configuration Files

```
src/main/resources/
├── application.yml                    # Base configuration
├── application-dev.yml                # Development overrides
├── application-test.yml               # Test overrides
├── application-pre.yml                # Pre-production overrides
└── application-prod.yml               # Production overrides
```

### Application Ports

| Environment | Port | Purpose |
|-------------|------|---------|
| Development | 1024 | Local dev server |
| Test | 1024 | Test environment |
| Pre-prod | 1024 | Staging |
| Production | 1024 | Production |

## API Documentation

**Swagger UI:** `http://localhost:1024/swagger-ui.html`

- Browse all API endpoints
- Test APIs interactively
- View request/response schemas
- Generated from controller annotations

## Architecture Validation

### ArchUnit Tests

The project uses ArchUnit to enforce architectural rules at build time:

```java
// Enforces:
// - Controller → Service only
// - Service → Manager/Dao only
// - Manager → Dao only
// - @Transactional only in Manager
// - Constructor injection only
// - No @Autowired field injection
```

Run before committing:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

### Code Quality Rules

Located in `.agent/rules/` directory:
- `01-naming-conventions.md` - Alibaba Java naming standards
- `04-exception-logging.md` - Exception handling and logging standards
- `09-manager-layer.md` - Manager layer constraints
- `10-architecture-rules.md` - Layered architecture enforcement
- `17-commit-message-conventions.md` - Git commit conventions

## Project Guidelines

### CLAUDE.md

The project root contains `CLAUDE.md` with comprehensive coding standards:
- Quick reference card for common patterns
- Detailed architecture explanations
- Build and test commands
- Anti-patterns to avoid
- Technology stack details

**All agents must read and follow CLAUDE.md guidance.**

### Git Commit Conventions

Follow Conventional Commits format:

```
<type>(<scope>): <subject>

Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
Scopes: sa-admin, sa-base, sa-common, smart-admin-web, smart-app, docker, docs

Examples:
feat(sa-admin): add employee performance review module
fix(sa-base): resolve NPE in file upload service
refactor(sa-common): optimize cache key generation strategy
```

## Database

### MyBatis Plus Configuration

- Entity mapping with `@TableName`
- Automatic CRUD operations via `BaseMapper`
- LambdaQueryWrapper for type-safe queries
- XML mappers for complex SQL

### Database Location

```
src/main/resources/mapper/    # MyBatis XML mappers
```

## Common Workflows

### Adding a New Feature

1. Create domain objects (Entity, Form, VO)
2. Create Dao interface extending BaseMapper
3. Implement Manager (if transactions/caching needed)
4. Implement Service with business logic
5. Create Controller with API endpoints
6. Run ArchitectureTest to validate
7. Write unit and integration tests
8. Commit with conventional commit message

### Architecture Test Validation

Before every commit:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

If test fails, fix architectural violations before committing.

## Development Tools

**IDE Support:**
- IntelliJ IDEA recommended
- Lombok plugin required
- Enable annotation processing

**Gradle Wrapper:**
- Use `./gradlew` (Unix/Mac) or `gradlew.bat` (Windows)
- No need to install Gradle globally

## References

- **CLAUDE.md** - Complete development guidelines (project root)
- **`.agent/rules/`** - Detailed coding rules and conventions
- **SmartAdmin Patterns** - See `smartadmin-patterns.md` in this directory
- **Quality Standards** - See `quality-standards.md` in this directory
