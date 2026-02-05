# Vavr Refactoring Assistant - Examples

## 範例 1: Service 層 Option 重構

**Before (違反 ArchUnit):**
```java
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    public Optional<EmployeeVO> getById(Long employeeId) {
        EmployeeEntity entity = employeeDao.selectById(employeeId);
        return Optional.ofNullable(entity)
            .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
    }

    public Optional<EmployeeVO> getByEmail(String email) {
        return Optional.ofNullable(employeeDao.getByEmail(email))
            .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
    }
}
```

**After (通過 ArchUnit):**
```java
import io.vavr.control.Option;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    public Option<EmployeeVO> getById(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
    }

    public Option<EmployeeVO> getByEmail(String email) {
        return Option.of(employeeDao.getByEmail(email))
            .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
    }
}
```

## 範例 2: Controller 使用 Option

```java
@RestController
@RequestMapping("/admin/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/{id}")
    public ResponseDTO<EmployeeVO> getById(@PathVariable Long id) {
        return employeeService.getById(id)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST));
    }
}
```

## 範例 3: Try 處理可能失敗的操作

```java
@Service
@RequiredArgsConstructor
public class FileService {

    public ResponseDTO<String> processFile(MultipartFile file) {
        return Try.of(() -> {
                String content = new String(file.getBytes());
                // 處理檔案內容
                return processContent(content);
            })
            .map(ResponseDTO::ok)
            .recover(IOException.class, ex ->
                ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "檔案讀取失敗"))
            .get();
    }
}
```

## 範例 4: Either 驗證模式

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    public Either<String, EmployeeVO> validateAndUpdate(EmployeeUpdateForm form) {
        // 驗證 ID
        if (form.getEmployeeId() == null) {
            return Either.left("員工ID不能為空");
        }

        // 驗證存在
        return Option.of(employeeDao.selectById(form.getEmployeeId()))
            .toEither("員工不存在")
            .flatMap(entity -> {
                // 驗證 email 唯一
                EmployeeEntity existing = employeeDao.getByEmail(form.getEmail());
                if (existing != null && !existing.getEmployeeId().equals(form.getEmployeeId())) {
                    return Either.left("Email 已被使用");
                }

                // 更新
                SmartBeanUtil.copyNonNull(form, entity);
                employeeDao.updateById(entity);
                return Either.right(SmartBeanUtil.copy(entity, EmployeeVO.class));
            });
    }

    // Controller 使用
    @PostMapping("/update")
    public ResponseDTO<?> update(@RequestBody EmployeeUpdateForm form) {
        return employeeService.validateAndUpdate(form)
            .fold(
                error -> ResponseDTO.userErrorParam(error),
                success -> ResponseDTO.ok(success)
            );
    }
}
```

## 常見錯誤修復

### 錯誤 1: 混用 Optional 和 Option

```java
// ❌ 混用
import java.util.Optional;
import io.vavr.control.Option;

public Option<User> getUser() {
    Optional<User> opt = repository.findById(id);  // 返回 Optional
    return Option.ofOptional(opt);  // 轉換
}

// ✅ 統一使用 Option
public Option<User> getUser() {
    User user = repository.selectById(id);  // 返回可能為 null
    return Option.of(user);
}
```

### 錯誤 2: Option.get() 不安全

```java
// ❌ 不安全
Option<User> userOpt = getUser();
User user = userOpt.get();  // 可能拋出 NoSuchElementException

// ✅ 安全
User user = userOpt.getOrElse(defaultUser);
User user = userOpt.getOrElseThrow(() -> new NotFoundException("User not found"));
```
