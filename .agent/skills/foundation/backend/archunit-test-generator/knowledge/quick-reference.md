# ArchUnit Test Generator - Quick Reference

## ArchUnit 基本語法

```java
// 類別規則
classes().that().haveSimpleNameEndingWith("Controller")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..service..", "..domain..");

// 方法規則
methods().that().areDeclaredInClassesThat()
    .haveSimpleNameEndingWith("Manager")
    .and().areAnnotatedWith(Transactional.class)
    .should().beAnnotatedWith(Transactional.class);

// 欄位規則
noFields().that().areDeclaredInClassesThat()
    .resideInAPackage("..service..")
    .should().beAnnotatedWith(Autowired.class);
```

## SmartAdmin 架構規則

| 規則 | ArchUnit 實現 |
|------|---------------|
| Controller 只呼叫 Service | `classes().should().onlyDependOnClassesThat()` |
| Service 使用 Vavr Option | `noClasses().should().dependOnClassesThat(Optional.class)` |
| @Transactional 在 Manager | `methods().should().beAnnotatedWith()` |
| 構造器注入 | `noFields().should().beAnnotatedWith(Autowired.class)` |

## 完整測試範本

```java
@AnalyzeClasses(packages = "net.lab1024.sa")
public class ArchitectureTest {

    // 分層規則
    @ArchTest
    static final ArchRule layer_dependencies = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Manager").definedBy("..manager..")
        .layer("Dao").definedBy("..dao..")
        .whereLayer("Controller").mayOnlyAccessLayers("Service")
        .whereLayer("Service").mayOnlyAccessLayers("Manager", "Dao")
        .whereLayer("Manager").mayOnlyAccessLayers("Dao");

    // Vavr Option 規則
    @ArchTest
    static final ArchRule service_should_use_vavr_option =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .belongToAnyOf(java.util.Optional.class);

    // @Transactional 規則
    @ArchTest
    static final ArchRule transactional_only_in_manager =
        noMethods()
            .that().areDeclaredInClassesThat()
            .resideOutsideOfPackage("..manager..")
            .should().beAnnotatedWith(Transactional.class);

    // 構造器注入規則
    @ArchTest
    static final ArchRule no_field_injection =
        noFields()
            .that().areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .should().beAnnotatedWith(Autowired.class);
}
```

## 運行命令

```bash
# 運行架構測試
./gradlew test --tests ArchitectureTest

# 運行特定規則
./gradlew test --tests "ArchitectureTest.layer_dependencies"
```

## 相關文件

- [ArchitectureTest.java](../../../../configs/ArchitectureTest.java) - 完整配置範本
- [F04-architecture-rules.md](../../../../rules/foundation/F04-architecture-rules.md)
