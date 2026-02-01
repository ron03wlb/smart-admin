# Example 1: Layer Dependency Validation

## Scenario

驗證 SmartAdmin 的分層架構依賴規則：
- Controller 只能調用 Service
- Service 可以調用 Dao 或 Manager
- Manager 可以調用 Dao
- Dao 不能調用任何上層（Controller/Service/Manager）

## Input

```bash
User: "Generate ArchUnit test to validate layer dependencies"
```

## Generated Output

**File:** `src/test/java/net/lab1024/sa/admin/ArchitectureTest.java`

```java
@AnalyzeClasses(packages = "net.lab1024.sa.admin")
class LayerDependencyTest {

    @ArchTest
    static final ArchRule controllerShouldOnlyAccessService =
        classes()
            .that().resideInAPackage("..controller..")
            .should().onlyAccessClassesThat(
                resideInAnyPackage(
                    "..service..",
                    "..controller..",
                    "..domain..",
                    "java..",
                    "org.springframework.."
                )
            )
            .because("Controller should only call Service layer");

    @ArchTest
    static final ArchRule serviceShouldNotAccessController =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().accessClassesThat().resideInAPackage("..controller..")
            .because("Service must not access Controller layer");

    @ArchTest
    static final ArchRule daoShouldNotAccessUpperLayers =
        noClasses()
            .that().resideInAPackage("..dao..")
            .should().accessClassesThat(
                resideInAnyPackage(
                    "..controller..",
                    "..service..",
                    "..manager.."
                )
            )
            .because("Dao must not access upper layers");
}
```

## Validation

```bash
./gradlew :sa-admin:test --tests ArchitectureTest

# Expected Output:
✅ LayerDependencyTest > controllerShouldOnlyAccessService PASSED
✅ LayerDependencyTest > serviceShouldNotAccessController PASSED
✅ LayerDependencyTest > daoShouldNotAccessUpperLayers PASSED
```

## Expected Result

- 所有分層依賴規則測試通過
- 違規會在編譯時失敗並顯示具體類名
- 確保分層架構一致性
