# ArchUnit Test Generator - Examples

## 範例 1: 模組架構測試

**User Request:**
```
為 employee 模組生成 ArchUnit 測試
```

**Generated Output:**
```java
package net.lab1024.sa.admin.module.employee;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@AnalyzeClasses(packages = "net.lab1024.sa.admin.module.employee")
public class EmployeeArchitectureTest {

    @ArchTest
    static final ArchRule controller_should_only_call_service =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..service..",
                "..domain..",
                "java..",
                "javax..",
                "org.springframework.."
            );

    @ArchTest
    static final ArchRule service_should_use_vavr =
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat()
            .belongToAnyOf(java.util.Optional.class);
}
```

## 範例 2: @Transactional 位置檢查

```java
@ArchTest
static final ArchRule transactional_only_in_manager =
    noMethods()
        .that().areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Service")
        .should().beAnnotatedWith(Transactional.class)
        .because("@Transactional should only be used in Manager layer");
```

**違規範例:**
```java
// ❌ 違規: Service 層使用 @Transactional
@Service
public class EmployeeService {
    @Transactional  // ArchUnit 會報錯
    public void updateEmployee(EmployeeUpdateForm form) { ... }
}

// ✅ 正確: Manager 層使用 @Transactional
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee(EmployeeEntity entity) { ... }
}
```

## 範例 3: 命名規範檢查

```java
@ArchTest
static final ArchRule controllers_naming =
    classes()
        .that().resideInAPackage("..controller..")
        .should().haveSimpleNameEndingWith("Controller")
        .because("Controllers should be named XXXController");

@ArchTest
static final ArchRule services_naming =
    classes()
        .that().resideInAPackage("..service..")
        .and().areAnnotatedWith(Service.class)
        .should().haveSimpleNameEndingWith("Service")
        .or().haveSimpleNameEndingWith("Manager");

@ArchTest
static final ArchRule entities_naming =
    classes()
        .that().resideInAPackage("..entity..")
        .should().haveSimpleNameEndingWith("Entity")
        .because("Entities should be named XXXEntity, not XXXPO or XXXDO");
```

## 範例 4: 依賴注入檢查

```java
@ArchTest
static final ArchRule no_field_injection =
    noFields()
        .should().beAnnotatedWith(Autowired.class)
        .because("Use constructor injection with @RequiredArgsConstructor");

@ArchTest
static final ArchRule classes_should_have_required_args_constructor =
    classes()
        .that().areAnnotatedWith(Service.class)
        .should().beAnnotatedWith(RequiredArgsConstructor.class)
        .because("Service classes should use @RequiredArgsConstructor");
```

**違規範例:**
```java
// ❌ 違規: 欄位注入
@Service
public class EmployeeService {
    @Autowired
    private EmployeeDao employeeDao;  // ArchUnit 會報錯
}

// ✅ 正確: 構造器注入
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
}
```

## 常見違規修復

### 違規 1: Service uses java.util.Optional

```java
// ❌ 違規
import java.util.Optional;
public Optional<EmployeeVO> getById(Long id) { ... }

// ✅ 修復
import io.vavr.control.Option;
public Option<EmployeeVO> getById(Long id) { ... }
```

### 違規 2: Controller calls Dao directly

```java
// ❌ 違規
@RestController
public class EmployeeController {
    private final EmployeeDao employeeDao;  // 直接呼叫 Dao
}

// ✅ 修復
@RestController
public class EmployeeController {
    private final EmployeeService employeeService;  // 透過 Service
}
```
