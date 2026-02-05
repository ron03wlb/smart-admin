---
name: smartadmin-integration-test
description: Generate Spring Boot integration tests with Testcontainers
priority: P0
category: full-stack
---

# SmartAdmin Integration Test

Generate Spring Boot integration tests using Testcontainers for database (PostgreSQL), cache (Redis), and API endpoint testing.

## Usage

```
User: "Generate integration tests for EmployeeService"
AI: [Generates test class with Testcontainers setup]
```

## When to Use

- Need database integration tests
- Testing with real PostgreSQL/Redis containers
- Validating API endpoints end-to-end
- Testing transactional behavior

## Test Architecture

```
@SpringBootTest
@Testcontainers
├── PostgreSQLContainer (database)
├── RedisContainer (cache)
└── MockMvc (API testing)
```

## Workflow

1. Analyze target service/controller
2. Generate base test class with Testcontainers
3. Create test methods for CRUD operations
4. Add test fixtures for data setup
5. Configure transaction rollback
6. Run and verify tests

## Related Rules

- [Q06-jacoco-coverage-rules.md](../../../rules/quality-tools/Q06-jacoco-coverage-rules.md)

## Example Session

**User:** Generate integration tests for EmployeeService

**AI Agent Actions:**
1. Create `EmployeeIntegrationTest.java`
2. Setup PostgreSQLContainer
3. Generate CRUD test methods
4. Add @Transactional for rollback
5. Run `./gradlew test`
