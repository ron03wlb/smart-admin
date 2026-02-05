# Spring Pattern Checker - Quick Reference

## @Transactional 規則

```java
// ❌ 錯誤: Service 層使用 @Transactional
@Service
public class EmployeeService {
    @Transactional  // 違規!
    public void updateEmployee() { }
}

// ✅ 正確: Manager 層使用 @Transactional
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee() { }
}
```

## 依賴注入規則

```java
// ❌ 錯誤: 欄位注入
@Service
public class EmployeeService {
    @Autowired
    private EmployeeDao employeeDao;
}

// ✅ 正確: 構造器注入
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
}
```

## 分層規則

| 層級 | 可呼叫 | 不可呼叫 |
|------|--------|----------|
| Controller | Service | Manager, Dao |
| Service | Manager, Dao | - |
| Manager | Dao | - |

## 檢查命令

```bash
./gradlew test --tests ArchitectureTest
```

## 相關規則

- [F04-architecture-rules.md](../../../../rules/foundation/F04-architecture-rules.md)
- [F03-manager-layer.md](../../../../rules/foundation/F03-manager-layer.md)
