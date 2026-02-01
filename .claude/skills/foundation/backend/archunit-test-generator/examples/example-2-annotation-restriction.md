# Example 2: Annotation Placement Validation

## Scenario

驗證 SmartAdmin 的註解使用規則：
- `@Transactional` 只能在 Manager 層使用
- `@Cacheable` 只能在 Manager 層使用
- `@Autowired` 字段注入被禁止（必須使用構造器注入）

## Input

```bash
User: "Generate ArchUnit test to enforce @Transactional only in Manager layer"
```

## Generated Output

**File:** `src/test/java/net/lab1024/sa/admin/ArchitectureTest.java`

```java
@AnalyzeClasses(packages = "net.lab1024.sa.admin")
class AnnotationRestrictionTest {

    @ArchTest
    static final ArchRule transactionalMustBeInManagerLayer =
        methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat()
                .resideInAPackage("..manager..")
            .because("@Transactional must only be used in Manager layer");

    @ArchTest
    static final ArchRule cacheableMustBeInManagerLayer =
        methods()
            .that().areAnnotatedWith(Cacheable.class)
            .should().beDeclaredInClassesThat()
                .resideInAPackage("..manager..")
            .because("@Cacheable must only be used in Manager layer");

    @ArchTest
    static final ArchRule noFieldInjection =
        noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("Use constructor injection instead of @Autowired field injection");

    @ArchTest
    static final ArchRule transactionalMustUseRollbackForThrowable =
        methods()
            .that().areAnnotatedWith(Transactional.class)
            .should(new ArchCondition<JavaMethod>("use rollbackFor = Throwable.class") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    Transactional annotation = method.getAnnotationOfType(Transactional.class);
                    Class<?>[] rollbackFor = annotation.rollbackFor();

                    boolean hasThrowable = Arrays.asList(rollbackFor)
                        .contains(Throwable.class);

                    if (!hasThrowable) {
                        String message = String.format(
                            "Method %s.%s() does not use rollbackFor = Throwable.class",
                            method.getOwner().getSimpleName(),
                            method.getName()
                        );
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            })
            .because("@Transactional must use rollbackFor = Throwable.class");
}
```

## Validation

```bash
./gradlew :sa-admin:test --tests ArchitectureTest

# Example violation output:
❌ AnnotationRestrictionTest > transactionalMustBeInManagerLayer FAILED
   Violation: Method UserService.saveUser() is annotated with @Transactional
   Expected: Should be in Manager layer
   Actual: Found in Service layer (UserService.java:45)
```

## Expected Result

- 所有註解使用規則測試通過
- 違規會顯示具體方法和文件位置
- 強制執行 SmartAdmin 架構約束
