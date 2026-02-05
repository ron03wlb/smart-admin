---
name: archunit-test-generator
description: Generate ArchUnit tests to enforce SmartAdmin architecture rules
priority: P0
category: backend
---

# ArchUnit Test Generator

Auto-generate ArchUnit test methods to enforce SmartAdmin's layered architecture constraints, annotation restrictions, and naming conventions.

## Usage

```
User: "Generate ArchUnit tests for the employee module"
AI: [Generates ArchUnit test class with architecture validation rules]
```

## When to Use

- Need to validate Controller → Service → Manager → Dao layering
- Want to ensure @Transactional only in Manager layer
- Enforce Vavr Option usage in Service layer
- Validate naming conventions (XXXController, XXXService, etc.)
- Check constructor injection (no @Autowired field injection)

## Generated Output

```java
@AnalyzeClasses(packages = "net.lab1024.sa.admin.module.employee")
public class EmployeeArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_only_call_services =
        classes().that().haveSimpleNameEndingWith("Controller")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..service..", "..domain..");

    @ArchTest
    static final ArchRule services_should_use_vavr_option =
        noClasses().that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat()
            .belongToAnyOf(java.util.Optional.class);
}
```

## Workflow

1. Read existing project structure
2. Identify module packages to test
3. Generate layered architecture rules
4. Generate annotation placement rules
5. Generate naming convention rules
6. Generate dependency injection rules
7. Output complete ArchUnit test class

## Architecture Rules Enforced

| Rule | Description | ArchUnit Pattern |
|------|-------------|------------------|
| Layer Access | Controller → Service only | `onlyDependOnClassesThat()` |
| Vavr Option | Service uses io.vavr.control.Option | `noClasses().should().dependOnClassesThat()` |
| Transaction | @Transactional in Manager only | `methods().should().beAnnotatedWith()` |
| DI Pattern | Constructor injection | `noFields().should().beAnnotatedWith()` |
| Naming | XXXController, XXXService | `haveSimpleNameEndingWith()` |

## Related Rules

- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)
- [F01-naming-conventions.md](../../../rules/foundation/F01-naming-conventions.md)
- [F03-manager-layer.md](../../../rules/foundation/F03-manager-layer.md)

## Example Session

**User:** Generate ArchUnit tests for architecture validation

**AI Agent Actions:**
1. Read existing ArchitectureTest.java for patterns
2. Identify target module packages
3. Generate test class with:
   - Layer dependency rules
   - Annotation placement rules
   - Naming convention rules
   - DI pattern rules
4. Validate generated tests compile
5. Report test coverage
