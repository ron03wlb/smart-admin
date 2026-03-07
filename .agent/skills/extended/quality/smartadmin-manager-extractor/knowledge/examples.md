# SmartAdmin Manager Extractor - Examples

## 範例 1: 單一方法提取

**Before (Service 有 @Transactional)**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderDao orderDao;
    private final InventoryDao inventoryDao;

    @Transactional(rollbackFor = Throwable.class)  // ❌ 違規
    public void createOrder(OrderForm form) {
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);
        inventoryDao.deduct(form.getProductId(), form.getQuantity());
    }

    public Option<OrderVO> getById(Long id) {
        return Option.of(orderDao.selectById(id))
            .map(o -> SmartBeanUtil.copy(o, OrderVO.class));
    }
}
```

**After (提取到 Manager)**:

```java
// 新建 OrderManager
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final OrderDao orderDao;
    private final InventoryDao inventoryDao;

    @Transactional(rollbackFor = Throwable.class)  // ✅ 正確位置
    public void createOrder(OrderForm form) {
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);
        inventoryDao.deduct(form.getProductId(), form.getQuantity());
    }
}

// 更新 OrderService
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderManager orderManager;  // 注入 Manager
    private final OrderDao orderDao;

    public void createOrder(OrderForm form) {
        orderManager.createOrder(form);  // 委派
    }

    public Option<OrderVO> getById(Long id) {
        return Option.of(orderDao.selectById(id))
            .map(o -> SmartBeanUtil.copy(o, OrderVO.class));
    }
}
```

---

## 範例 2: 批量提取多個方法

**Before**:
```java
@Service
public class UserService {
    @Transactional(rollbackFor = Throwable.class)
    public void createUser(UserForm form) { ... }

    @Transactional(rollbackFor = Throwable.class)
    public void updateUser(UserUpdateForm form) { ... }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteUser(Long id) { ... }

    public Option<UserVO> getById(Long id) { ... }  // 不需要事務
}
```

**After**:
```java
// UserManager - 所有事務方法
@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserDao userDao;
    private final RoleDao roleDao;

    @Transactional(rollbackFor = Throwable.class)
    public void createUser(UserForm form) { ... }

    @Transactional(rollbackFor = Throwable.class)
    public void updateUser(UserUpdateForm form) { ... }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteUser(Long id) { ... }
}

// UserService - 委派 + 查詢
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;
    private final UserDao userDao;

    public void createUser(UserForm form) {
        userManager.createUser(form);
    }

    public void updateUser(UserUpdateForm form) {
        userManager.updateUser(form);
    }

    public void deleteUser(Long id) {
        userManager.deleteUser(id);
    }

    public Option<UserVO> getById(Long id) {
        return Option.of(userDao.selectById(id))
            .map(u -> SmartBeanUtil.copy(u, UserVO.class));
    }
}
```

---

## 範例 3: 驗證流程

```bash
# 1. 備份當前狀態
git stash push -m "Before manager extraction"

# 2. 執行重構
# (AI 生成代碼)

# 3. 編譯檢查
./gradlew :smartadmin-app:compileJava

# 4. ArchUnit 驗證
./gradlew :smartadmin-app:test --tests ArchitectureTest

# 5. 完整測試
./gradlew :smartadmin-app:test

# 6. 如果失敗，回滾
git stash pop
```
