# Test Fixture Generator - Quick Reference

**Version**: 1.0.0  
**Last Updated**: 2026-02-02  
**Skill**: test-fixture-generator (P0 - Critical)

---

## Quick Start

### Generate Test Builder

// For Employee entity
public class EmployeeBuilder {
    private Long id = 1L;
    private String name = "John Doe";
    private Boolean deleted = false;
    
    public EmployeeBuilder id(Long id) {
        this.id = id;
        return this;
    }
    
    public EmployeeBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public EmployeeEntity build() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDeleted(deleted);
        return entity;
    }
}

### Usage in Tests

@Test
void testEmployeeService() {
    EmployeeEntity employee = new EmployeeBuilder()
        .name("Jane Doe")
        .build();
    
    employeeDao.insert(employee);
    
    Option<EmployeeVO> result = employeeService.queryDetail(employee.getId());
    assertTrue(result.isDefined());
}

---

## 2 Core Patterns

### 1. Builder Pattern
- Fluent API
- Default values
- Immutable once built

### 2. Test Data Management
- Centralized fixtures
- Reusable across tests
- Easy to maintain

---

## Commands

### Generate Builder
User: "Generate test builder for Employee entity"

Output:
- EmployeeBuilder.java
- Example usage in test

---

## Builder Template

public class {Entity}Builder {
    // Fields with defaults
    private Long id = 1L;
    private String field = "default";
    
    // Fluent setters
    public {Entity}Builder field(Type value) {
        this.field = value;
        return this;
    }
    
    // Build method
    public {Entity} build() {
        {Entity} entity = new {Entity}();
        // Set all fields
        return entity;
    }
}

---

## Best Practices

- Use builders in @BeforeEach
- Reset state between tests
- Provide sensible defaults
- Support partial updates

---

See SKILL.md for complete builder patterns.
