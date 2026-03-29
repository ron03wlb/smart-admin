# SmartAdmin Project Architecture

This document provides essential context about the SmartAdmin project structure, technology stack, and build processes. **All agents should reference this document to understand the project context.**

## Technology Stack

### Backend Stack (smart-admin-api-java21-springboot3)

| Component | Version | Purpose | Rules Reference |
|-----------|---------|---------|-----------------|
| **Java** | 21 | Programming language with modern features | - |
| **Spring Boot** | 3.5.4 | Application framework | - |
| **MyBatis Plus** | 3.5.12 | ORM and database access | - |
| **Sa-Token** | 1.44.0 | Authentication and authorization | - |
| **PostgreSQL Driver** | 42.7.5 | Database driver | - |
| **Vavr** | 0.10.4 | Functional programming library | - |
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
**PostgreSQL 16** (Target Architecture)
- JSONB support for flexible document storage
- Array types for efficient data structures
- CTEs and window functions for complex queries

**MyBatis Plus 3.5.12**
- LambdaQueryWrapper for type-safe queries (preferred)
- Automatic CRUD via BaseMapper

## Module Structure

The project follows a modular monolith architecture (v4.1.0) with 47 modules organized in 6 layers:

```
smart-admin-api-java21-springboot3/
├── smartadmin-common/       # Public Foundation (21 modules)
│   ├── smartadmin-common-bom           # Bill of Materials
│   ├── smartadmin-common-core          # Core utilities (SmartBeanUtil, ResponseDTO)
│   ├── smartadmin-common-web           # Web framework base
│   ├── smartadmin-common-mybatis       # ORM layer
│   ├── smartadmin-common-redis         # Cache provider
│   ├── smartadmin-common-token         # Sa-Token integration
│   └── ... (15 more modules)
│
├── smartadmin-support/      # Business Support (17 modules)
│   ├── smartadmin-support-config       # Config management
│   ├── smartadmin-support-dict         # Dictionary data
│   ├── smartadmin-support-job          # Job scheduling (Snail-Job)
│   ├── smartadmin-support-liteflow     # LiteFlow rule engine
│   └── ... (13 more modules)
│
├── smartadmin-modules/      # Business Domain (3 modules)
│   ├── smartadmin-system               # System management (User, Role, Menu, Dept)
│   ├── smartadmin-business             # Business features
│   └── smartadmin-oa                   # Office automation
│
├── smartadmin-api/          # API Contract Layer (3 modules)
│   ├── smartadmin-api-system           # System API contracts/DTOs
│   ├── smartadmin-api-business         # Business API contracts/DTOs
│   └── smartadmin-api-oa              # OA API contracts/DTOs
│
├── smartadmin-starter/      # Starter Combinations (2 modules)
│   ├── smartadmin-starter-web          # Spring Boot web starter
│   └── smartadmin-starter-all          # Complete starter with all features
│
├── smartadmin-app/          # Unified Application Entry (1 module)
│   └── SmartAdminApplication           # Main class: net.lab1024.sa.SmartAdminApplication
│
└── settings.gradle.kts      # Gradle module configuration (47 modules)
```

### Module Responsibilities

**smartadmin-common/** (21 modules)
- Public foundation libraries shared across all modules
- Core utilities: SmartBeanUtil, SmartPageUtil, ResponseDTO
- Infrastructure integrations: Redis, MyBatis, Sa-Token, Swagger
- Cross-cutting features: validation, caching, MQ, security

**smartadmin-support/** (17 modules)
- Business support services not tied to specific domains
- Support features: file upload, config management, code generation
- Job scheduling, heartbeat monitoring, LiteFlow rule engine
- Audit logging, data tracing, serial number generation

**smartadmin-modules/** (3 modules)
- Core business domain logic
- Each module follows layered architecture: controller/service/manager/dao/domain
- Packages: `net.lab1024.sa.{system|business|oa}.{feature}.*`

**smartadmin-api/** (3 modules)
- API contract layer with DTOs for cross-module communication
- Adapter pattern for service interface contracts
- Packages: `net.lab1024.sa.api.{system|business|oa}.*`

**smartadmin-starter/** (2 modules)
- Spring Boot auto-configuration starters
- Bundles common and support modules for easy dependency management

**smartadmin-app/** (1 module)
- Unified application entry point
- Depends on all business modules via starters
- Main class: `net.lab1024.sa.SmartAdminApplication`
- Contains integration tests and ArchitectureTest

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
./gradlew :smartadmin-app:bootRun

# Application starts on: http://localhost:1024
# Swagger UI available at: http://localhost:1024/swagger-ui.html
```

### Testing

```bash
# Run all tests
./gradlew :smartadmin-app:test

# Run specific test class
./gradlew :smartadmin-app:test --tests ArchitectureTest

# Run per-module architecture tests
./gradlew :smartadmin-modules:smartadmin-system:test --tests ArchitectureTest
./gradlew :smartadmin-modules:smartadmin-business:test --tests ArchitectureTest
./gradlew :smartadmin-modules:smartadmin-oa:test --tests ArchitectureTest

# Run architecture validation (CRITICAL before commits)
./gradlew :smartadmin-app:test --tests ArchitectureTest
```

**Important:** `ArchitectureTest` exists in each business module and in `smartadmin-app`. The `smartadmin-app` test transitively validates all modules. This test MUST pass before committing code.

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
1. Start backend: `cd smart-admin-api-java21-springboot3 && ./gradlew :smartadmin-app:bootRun`
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
./gradlew :smartadmin-app:test --tests ArchitectureTest
```

### Code Quality Rules

See [CLAUDE.md](../../../CLAUDE.md) for naming conventions, architecture rules, Manager layer constraints, and commit message conventions.

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
Scopes: smartadmin-system, smartadmin-business, smartadmin-oa, smartadmin-app, smartadmin-common, smartadmin-support, smart-admin-web, smart-app, docker, docs

Examples:
feat(smartadmin-system): add employee performance review module
fix(smartadmin-support): resolve NPE in file upload service
refactor(smartadmin-common): optimize cache key generation strategy
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
./gradlew :smartadmin-app:test --tests ArchitectureTest
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
- **SmartAdmin Patterns** - See `smartadmin-patterns.md` in this directory
- **Quality Standards** - See `quality-standards.md` in this directory
