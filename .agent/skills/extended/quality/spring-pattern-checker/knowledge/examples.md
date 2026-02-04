# Spring Pattern Checker - Examples

## 範例 1: 修復 @Transactional 位置

**Before (違規):**
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDao orderDao;

    @Transactional  // ❌ 違規: Service 層不應有 @Transactional
    public void createOrder(OrderAddForm form) {
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);
        // 更多操作...
    }
}
```

**After (修復):**
```java
// Service 層: 無 @Transactional
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;

    public ResponseDTO<Long> createOrder(OrderAddForm form) {
        return orderManager.createOrder(form);
    }
}

// Manager 層: @Transactional
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Long> createOrder(OrderAddForm form) {
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);
        return ResponseDTO.ok(order.getOrderId());
    }
}
```

## 範例 2: 修復依賴注入

**Before (違規):**
```java
@RestController
public class EmployeeController {

    @Autowired  // ❌ 違規: 欄位注入
    private EmployeeService employeeService;

    @Autowired
    private EmployeeDao employeeDao;  // ❌ 違規: Controller 直接呼叫 Dao
}
```

**After (修復):**
```java
@RestController
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;  // ✅ 構造器注入
    // 移除 EmployeeDao，透過 Service 存取
}
```
