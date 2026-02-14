# ArchUnit Test Patterns

## Three Main Pattern Categories

1. **Layer Rules**: Control layer dependencies (Controller → Service → Manager → Dao)
2. **Annotation Rules**: Restrict annotation placement and parameters
3. **Naming Rules**: Enforce class/method naming conventions

## Layer Architecture Pattern

layeredArchitecture()
    .whereLayer(X).mayOnlyBeAccessedByLayers(Y)

## Annotation Restriction Pattern

methods()
    .that().areAnnotatedWith(Annotation.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")

## Annotation Parameter Validation

methods().should(new ArchCondition<JavaMethod>() {
    public void check(JavaMethod method, ConditionEvents events) {
        // Custom validation logic
    }
})

See SKILL.md for 7 complete patterns with examples.
