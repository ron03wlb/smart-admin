---
name: test-fixture-generator
description: Generate test data builders for complex domain objects
priority: P0
category: testing
---

# Test Fixture Generator

Generate reusable test data builders for Entity, Form, and VO objects using AtomicInteger pattern for unique values.

## Usage

```
User: "Generate test fixtures for Employee"
AI: [Creates EmployeeTestFixture.java with builder methods]
```

## When to Use

- Need test data for Entity/Form/VO
- Creating reusable test fixtures
- Avoiding hardcoded test values
- Setting up integration test data

## Generated Pattern

```java
public class EmployeeTestFixture {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static EmployeeEntity createEntity() {
        int id = COUNTER.incrementAndGet();
        // ... generate unique fields
    }

    public static EmployeeAddForm createAddForm() { ... }
    public static EmployeeUpdateForm createUpdateForm(Long id) { ... }
    public static EmployeeQueryForm createQueryForm() { ... }
}
```

## Workflow

1. Analyze target domain objects
2. Identify required fields
3. Generate fixture class with AtomicInteger
4. Create builder methods for each type
5. Add customization methods
6. Generate test examples

## Related Rules

- [Q06-jacoco-coverage-rules.md](../../../rules/quality-tools/Q06-jacoco-coverage-rules.md)

## Example Session

**User:** Generate test fixtures for Product entity

**AI Agent Actions:**
1. Read ProductEntity fields
2. Create ProductTestFixture class
3. Generate createEntity(), createAddForm(), etc.
4. Add AtomicInteger for unique values
5. Output complete fixture class
