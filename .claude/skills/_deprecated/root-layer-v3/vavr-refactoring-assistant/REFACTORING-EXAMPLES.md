# Vavr Refactoring Examples

Real before/after examples from SmartAdmin codebase showing complete refactoring patterns.

---

## Example 1: Simple Entity Lookup

**Context:** BrandService.getById() - typical CRUD query pattern

### BEFORE (Traditional null check)

```java
package net.lab1024.sa.admin.module.business.brand.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.brand.dao.BrandDao;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.admin.module.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandDao brandDao;

    /**
     * Get brand by ID
     */
    public ResponseDTO<BrandVO> getById(Long brandId) {
        BrandEntity entity = brandDao.selectById(brandId);
        if (entity == null || entity.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("Brand does not exist");
        }

        BrandVO vo = SmartBeanUtil.copy(entity, BrandVO.class);
        return ResponseDTO.ok(vo);
    }
}
```

### AFTER (Vavr Option pattern)

```java
package net.lab1024.sa.admin.module.business.brand.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.brand.dao.BrandDao;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.admin.module.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandDao brandDao;

    /**
     * Get brand by ID
     *
     * @return Option containing BrandVO if found and not deleted
     */
    public Option<BrandVO> getById(Long brandId) {
        return Option.of(brandDao.selectById(brandId))
            .filter(entity -> !entity.getDeletedFlag())
            .map(entity -> SmartBeanUtil.copy(entity, BrandVO.class));
    }
}
```

### Controller Layer (handles Option)

```java
@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping("/{brandId}")
    public ResponseDTO<BrandVO> getBrand(@PathVariable Long brandId) {
        return brandService.getById(brandId)
            .fold(
                () -> ResponseDTO.userErrorParam("Brand does not exist"),
                brand -> ResponseDTO.ok(brand)
            );
    }
}
```

**Key Changes:**
1. Service returns `Option<BrandVO>` instead of `ResponseDTO<BrandVO>`
2. Null check replaced with `Option.of()` + `.filter()`
3. Error handling moved to Controller layer
4. Method chaining replaces imperative if-else

---

## Example 2: Uniqueness Validation

**Context:** BrandService.addBrand() - check duplicate before insert

### BEFORE (Multiple null checks with early returns)

```java
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandDao brandDao;
    private final BrandManager brandManager;

    /**
     * Add brand
     */
    public ResponseDTO<String> addBrand(BrandAddForm addForm) {
        // Validate unique brand name
        BrandEntity existing = brandDao.getByBrandName(addForm.getBrandName(), null);
        if (existing != null) {
            return ResponseDTO.userErrorParam("Brand name already exists");
        }

        BrandEntity entity = SmartBeanUtil.copy(addForm, BrandEntity.class);
        brandManager.saveBrand(entity);
        return ResponseDTO.ok();
    }
}
```

### AFTER (Either pattern for validation chain)

```java
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandDao brandDao;
    private final BrandManager brandManager;

    /**
     * Add brand with validation
     *
     * @return Either.Left(error) or Either.Right(success message)
     */
    public Either<String, String> addBrand(BrandAddForm addForm) {
        return validateBrandNameUnique(addForm.getBrandName())
            .flatMap(name -> saveBrand(addForm));
    }

    private Either<String, String> validateBrandNameUnique(String brandName) {
        return Option.of(brandDao.getByBrandName(brandName, null)).isEmpty()
            ? Either.right(brandName)
            : Either.left("Brand name already exists");
    }

    private Either<String, String> saveBrand(BrandAddForm addForm) {
        return Try.of(() -> {
                BrandEntity entity = SmartBeanUtil.copy(addForm, BrandEntity.class);
                brandManager.saveBrand(entity);
                return "Brand created successfully";
            })
            .toEither()
            .mapLeft(ex -> "Brand creation failed: " + ex.getMessage());
    }
}
```

### Controller Layer (handles Either)

```java
@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseDTO<String> addBrand(@Valid @RequestBody BrandAddForm addForm) {
        return brandService.addBrand(addForm)
            .fold(
                error -> ResponseDTO.userErrorParam(error),
                success -> ResponseDTO.ok(success)
            );
    }
}
```

**Key Changes:**
1. Validation returns `Either<String, String>` (error or success)
2. `Option.isEmpty()` replaces null check
3. `Try.of()` wraps Manager layer call (handles exceptions)
4. `.flatMap()` chains validation → save
5. Controller uses `.fold()` to handle both cases

---

## Example 3: Complex Nested Access

**Context:** EmployeeService.getEmployeeCity() - nested nullable navigation

### BEFORE (Multiple null checks)

```java
@Service
public class EmployeeService {

    @Resource
    private EmployeeDao employeeDao;

    @Resource
    private DepartmentDao departmentDao;

    /**
     * Get employee with department city
     */
    public ResponseDTO<EmployeeVO> getEmployeeCity(Long employeeId) {
        EmployeeEntity employee = employeeDao.selectById(employeeId);
        if (employee == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long departmentId = employee.getDepartmentId();
        String departmentName = "Unknown";

        if (departmentId != null) {
            DepartmentEntity department = departmentDao.selectById(departmentId);
            if (department != null) {
                departmentName = department.getDepartmentName();
            }
        }

        EmployeeVO vo = SmartBeanUtil.copy(employee, EmployeeVO.class);
        vo.setDepartmentName(departmentName);
        return ResponseDTO.ok(vo);
    }
}
```

### AFTER (Option flatMap chaining)

```java
import io.vavr.control.Option;

@Service
public class EmployeeService {

    @Resource
    private EmployeeDao employeeDao;

    @Resource
    private DepartmentDao departmentDao;

    /**
     * Get employee with department city
     *
     * @return Option containing EmployeeVO with department info
     */
    public Option<EmployeeVO> getEmployeeCity(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .map(employee -> {
                EmployeeVO vo = SmartBeanUtil.copy(employee, EmployeeVO.class);

                // Get department name with flatMap chaining
                String departmentName = Option.of(employee.getDepartmentId())
                    .flatMap(deptId -> Option.of(departmentDao.selectById(deptId)))
                    .map(dept -> dept.getDepartmentName())
                    .getOrElse("Unknown");

                vo.setDepartmentName(departmentName);
                return vo;
            });
    }
}
```

### Alternative: More Functional Style

```java
/**
 * Get employee with department city (pure functional)
 */
public Option<EmployeeVO> getEmployeeCityFunctional(Long employeeId) {
    return findEmployee(employeeId)
        .map(employee -> buildEmployeeVO(employee, findDepartmentName(employee)));
}

private Option<EmployeeEntity> findEmployee(Long employeeId) {
    return Option.of(employeeDao.selectById(employeeId));
}

private String findDepartmentName(EmployeeEntity employee) {
    return Option.of(employee.getDepartmentId())
        .flatMap(deptId -> Option.of(departmentDao.selectById(deptId)))
        .map(DepartmentEntity::getDepartmentName)
        .getOrElse("Unknown");
}

private EmployeeVO buildEmployeeVO(EmployeeEntity employee, String departmentName) {
    EmployeeVO vo = SmartBeanUtil.copy(employee, EmployeeVO.class);
    vo.setDepartmentName(departmentName);
    return vo;
}
```

**Key Changes:**
1. Three levels of null checks replaced with `.flatMap()` chain
2. `employee → departmentId → department → departmentName` navigation is safe
3. `.getOrElse("Unknown")` provides default at the end of chain
4. Functional style version splits into small, testable functions

---

## Example 4: Exception Handling

**Context:** File I/O operation in Service layer

### BEFORE (try-catch with early return)

```java
@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    /**
     * Read configuration file
     */
    public ResponseDTO<String> readConfig(String configPath) {
        try {
            String content = Files.readString(Paths.get(configPath));
            return ResponseDTO.ok(content);
        } catch (IOException e) {
            log.error("Failed to read config file: {}", configPath, e);
            return ResponseDTO.error("Configuration file not found");
        }
    }
}
```

### AFTER (Try pattern)

```java
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    /**
     * Read configuration file
     *
     * @return Try containing config content or exception
     */
    public Try<String> readConfig(String configPath) {
        return Try.of(() -> Files.readString(Paths.get(configPath)))
            .onFailure(e -> log.error("Failed to read config file: {}", configPath, e));
    }
}
```

### Controller Layer (handles Try)

```java
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public ResponseDTO<String> getConfig(@RequestParam String path) {
        return configService.readConfig(path)
            .fold(
                error -> ResponseDTO.error("Configuration file not found"),
                content -> ResponseDTO.ok(content)
            );
    }

    // Alternative: with default value
    @GetMapping("/with-default")
    public ResponseDTO<String> getConfigWithDefault(@RequestParam String path) {
        String content = configService.readConfig(path)
            .getOrElse("# Default configuration\nkey=value");
        return ResponseDTO.ok(content);
    }
}
```

**Key Changes:**
1. `try-catch` replaced with `Try.of()`
2. Service returns `Try<String>` instead of `ResponseDTO<String>`
3. `.onFailure()` logs error (side effect)
4. Controller decides how to handle failure (error or default)

---

## Example 5: Multiple Validations (Business Logic)

**Context:** Employee creation with multiple validation rules

### BEFORE (Multiple if checks with early returns)

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;
    private final SecurityPasswordService securityPasswordService;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Add new employee
     */
    public ResponseDTO<String> addEmployee(EmployeeAddForm form) {
        // Validate email format
        if (!EMAIL_PATTERN.matcher(form.getEmail()).matches()) {
            return ResponseDTO.userErrorParam("Invalid email format");
        }

        // Check login name uniqueness
        EmployeeEntity existingByLogin =
            employeeDao.getByLoginName(form.getLoginName(), null);
        if (existingByLogin != null) {
            return ResponseDTO.userErrorParam("Login name already exists");
        }

        // Check phone uniqueness
        EmployeeEntity existingByPhone =
            employeeDao.getByPhone(form.getPhone(), null);
        if (existingByPhone != null) {
            return ResponseDTO.userErrorParam("Phone number already exists");
        }

        // Check email uniqueness
        EmployeeEntity existingByEmail =
            employeeDao.getByEmail(form.getEmail(), null);
        if (existingByEmail != null) {
            return ResponseDTO.userErrorParam("Email already exists");
        }

        // Create employee
        try {
            EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
            String randomPassword = securityPasswordService.randomPassword();
            entity.setLoginPwd(securityPasswordService.getEncryptPwd(randomPassword));
            entity.setDeletedFlag(false);

            employeeManager.saveEmployee(entity, form.getRoleIdList());
            return ResponseDTO.ok(randomPassword);
        } catch (Exception e) {
            log.error("Failed to create employee", e);
            return ResponseDTO.error("Employee creation failed");
        }
    }
}
```

### AFTER (Either validation chain)

```java
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;
    private final SecurityPasswordService securityPasswordService;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Add new employee with validation chain
     *
     * @return Either.Left(error) or Either.Right(generated password)
     */
    public Either<String, String> addEmployee(EmployeeAddForm form) {
        return validateEmailFormat(form.getEmail())
            .flatMap(email -> validateLoginNameUnique(form.getLoginName()))
            .flatMap(loginName -> validatePhoneUnique(form.getPhone()))
            .flatMap(phone -> validateEmailUnique(form.getEmail()))
            .flatMap(email -> createEmployee(form));
    }

    private Either<String, String> validateEmailFormat(String email) {
        return EMAIL_PATTERN.matcher(email).matches()
            ? Either.right(email)
            : Either.left("Invalid email format");
    }

    private Either<String, String> validateLoginNameUnique(String loginName) {
        return Option.of(employeeDao.getByLoginName(loginName, null)).isEmpty()
            ? Either.right(loginName)
            : Either.left("Login name already exists");
    }

    private Either<String, String> validatePhoneUnique(String phone) {
        return Option.of(employeeDao.getByPhone(phone, null)).isEmpty()
            ? Either.right(phone)
            : Either.left("Phone number already exists");
    }

    private Either<String, String> validateEmailUnique(String email) {
        return Option.of(employeeDao.getByEmail(email, null)).isEmpty()
            ? Either.right(email)
            : Either.left("Email already exists");
    }

    private Either<String, String> createEmployee(EmployeeAddForm form) {
        return Try.of(() -> {
                EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
                String randomPassword = securityPasswordService.randomPassword();
                entity.setLoginPwd(securityPasswordService.getEncryptPwd(randomPassword));
                entity.setDeletedFlag(false);

                employeeManager.saveEmployee(entity, form.getRoleIdList());
                return randomPassword;
            })
            .toEither()
            .mapLeft(ex -> "Employee creation failed: " + ex.getMessage());
    }
}
```

### Controller Layer (handles Either)

```java
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseDTO<String> addEmployee(@Valid @RequestBody EmployeeAddForm form) {
        return employeeService.addEmployee(form)
            .fold(
                error -> ResponseDTO.userErrorParam(error),
                password -> ResponseDTO.ok(password)
            );
    }
}
```

**Key Changes:**
1. Four validation checks chained with `.flatMap()`
2. Each validation returns `Either<String, String>`
3. Short-circuits on first failure (fail-fast)
4. Small, testable validation functions
5. Exception handling with `Try.toEither()`
6. Single return point in Controller

**Benefits:**
- Each validation is independently testable
- Easy to add/remove validations
- Clear error propagation
- No nested if-else complexity

---

## Comparison Summary

| Pattern | Before | After | Key Benefit |
|---------|--------|-------|-------------|
| **Null Check** | `if (x == null)` | `Option.of(x)` | No explicit null handling |
| **Nested Nulls** | Multiple if blocks | `.flatMap()` chain | Declarative navigation |
| **Exceptions** | `try-catch` | `Try.of()` | Exception as value |
| **Validation** | Early returns | `Either.flatMap()` | Fail-fast chain |
| **Layer Separation** | Service returns ResponseDTO | Service returns Option/Try/Either | Clear responsibility |

---

## Migration Strategy

**Recommended order:**
1. Start with simple lookups (Example 1)
2. Add validation chains (Example 2)
3. Handle nested navigation (Example 3)
4. Convert exception handling (Example 4)
5. Refactor complex validations (Example 5)

**Validation:**
```bash
./gradlew :sa-admin:test --tests ArchitectureTest#serviceUsesVavrOption
```

---

**Document Version:** 1.0
**Last Updated:** 2026-01-25
