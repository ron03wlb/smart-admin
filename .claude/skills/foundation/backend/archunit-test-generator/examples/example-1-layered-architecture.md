# 範例 1: 驗證分層架構（Layered Architecture）

**技能**: archunit-test-generator
**難度**: ⭐⭐☆☆☆（中等）
**預估時間**: 10-15 分鐘

---

## 場景描述

確保 SmartAdmin 的分層架構規則被正確執行：
- Controller 層不應直接訪問 Dao 層（必須通過 Service）
- Service 層不應訪問 Controller 層
- 所有層都可以訪問 Domain 層（Entity, VO, DTO）

---

## 問題範例

### ❌ 錯誤代碼（架構違規）

```java
package net.lab1024.sa.admin.module.business.user.controller;

import net.lab1024.sa.admin.module.business.user.dao.UserDao;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserDao userDao; // ❌ Controller 不應直接注入 Dao

    @GetMapping("/{userId}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long userId) {
        // ❌ Controller 直接調用 Dao（跳過 Service 層）
        UserEntity user = userDao.selectById(userId);
        return ResponseDTO.ok(SmartBeanUtil.copy(user, UserVO.class));
    }
}
```

**問題**:
- Controller 直接依賴 Dao 層（違反分層原則）
- 跳過 Service 層導致業務邏輯分散
- 難以測試和維護

---

## 解決方案

### 1. 使用 ArchUnit 測試生成器

**觸發命令**:
```
User: "Generate ArchUnit test for layered architecture"
```

或在 ArchitectureTest.java 中添加：

```java
@AnalyzeClasses(packages = "net.lab1024.sa.admin.module", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule controllerShouldNotAccessDao =
        classes()
            .that().resideInAPackage("..controller..")
            .should().onlyDependOnClassesThat(
                resideInAnyPackage(
                    "..service..",    // ✅ 允許訪問 Service
                    "..domain..",     // ✅ 允許訪問 Domain
                    "java..",         // ✅ 允許 Java 標準庫
                    "org.springframework..",  // ✅ 允許 Spring
                    "lombok.."        // ✅ 允許 Lombok
                )
            )
            .because("Controller should only depend on Service layer, not Dao layer");
}
```

---

### 2. 執行測試

```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest.controllerShouldNotAccessDao
```

---

### 3. 測試輸出（失敗）

```
Architecture Violation [Priority: MEDIUM] - Rule 'classes that reside in a package '..controller..'
should only depend on classes that reside in any package ['..service..', '..domain..', 'java..',
'org.springframework..', 'lombok..']' was violated (1 times):

Method <net.lab1024.sa.admin.module.business.user.controller.UserController.getUser(java.lang.Long)>
depends on class <net.lab1024.sa.admin.module.business.user.dao.UserDao> in
(UserController.java:15)
```

---

## 修正代碼

### ✅ 正確代碼（符合架構規範）

```java
package net.lab1024.sa.admin.module.business.user.controller;

import net.lab1024.sa.admin.module.business.user.service.UserService;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // ✅ Controller 注入 Service

    @GetMapping("/{userId}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long userId) {
        // ✅ Controller 調用 Service（符合分層原則）
        UserVO user = userService.getUserById(userId);
        return ResponseDTO.ok(user);
    }
}
```

**Service 層**:
```java
package net.lab1024.sa.admin.module.business.user.service;

import net.lab1024.sa.admin.module.business.user.dao.UserDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import io.vavr.control.Option;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;

    public UserVO getUserById(Long userId) {
        return Option.ofOptional(userDao.selectById(userId))
            .map(entity -> SmartBeanUtil.copy(entity, UserVO.class))
            .getOrElseThrow(() -> new BusinessException(UserErrorCode.DATA_NOT_EXIST));
    }
}
```

---

## 驗證測試通過

```bash
./gradlew :sa-admin:test --tests ArchitectureTest.controllerShouldNotAccessDao
```

**輸出**:
```
> Task :sa-admin:test

ArchitectureTest > controllerShouldNotAccessDao() PASSED

BUILD SUCCESSFUL in 12s
```

---

## 相關規則

本範例直接關聯以下 SmartAdmin 架構規則：

- **[Architecture Rules - Layer Dependencies](./../../../../../.agent/rules/foundation/10-architecture-rules.md#layer-dependencies)**
  - Controller → Service ONLY（Controller CANNOT call Dao/Manager）
  - Service → Dao/Manager（Service CAN call both）
  - Manager → Dao（Manager CAN call Dao for @Transactional operations）

- **[Naming Conventions](./../../../../../.agent/rules/foundation/01-naming-conventions.md)**
  - 類別命名: `UserController`, `UserService`, `UserDao`（不使用 Impl 後綴）

---

## 延伸練習

### 練習 1: 驗證 Service 不訪問 Controller

**任務**: 編寫 ArchUnit 規則確保 Service 層不依賴 Controller 層

<details>
<summary>💡 提示</summary>

```java
@ArchTest
public static final ArchRule serviceShouldNotAccessController =
    classes()
        .that().resideInAPackage("..service..")
        .should().onlyDependOnClassesThat(
            resideInAnyPackage(
                "..dao..",
                "..manager..",
                "..domain..",
                "java..",
                "io.vavr..",
                "org.springframework..",
                "lombok.."
            )
        )
        .because("Service should not depend on Controller layer");
```
</details>

---

### 練習 2: 驗證所有層都可以訪問 Domain

**任務**: 確保 Domain 層（Entity, VO, DTO）不依賴任何業務層

<details>
<summary>💡 提示</summary>

```java
@ArchTest
public static final ArchRule domainShouldNotDependOnLayers =
    classes()
        .that().resideInAPackage("..domain..")
        .should().onlyDependOnClassesThat(
            resideInAnyPackage(
                "..domain..",
                "java..",
                "com.baomidou.mybatisplus..",
                "lombok.."
            )
        )
        .because("Domain layer should be independent of business layers");
```
</details>

---

## 總結

### 學到什麼

✅ **分層架構原則**:
- Controller → Service → Dao/Manager 的依賴方向
- 每層的職責邊界

✅ **ArchUnit 測試技巧**:
- `resideInAPackage()` 匹配套件
- `onlyDependOnClassesThat()` 限制依賴
- `because()` 添加規則說明

✅ **SmartAdmin 架構規範**:
- Controller 必須通過 Service 訪問數據
- Service 可以直接調用 Dao（單表 CRUD）
- 需要事務時，Service 委託給 Manager

---

## 參考資料

- [ArchUnit 官方文檔](https://www.archunit.org/userguide/html/000_Index.html)
- [SmartAdmin Architecture Rules](../../../../../.agent/rules/foundation/10-architecture-rules.md)
- [ArchitectureTest.java](../../../../../smart-admin-api-java21-springboot3/sa-admin/src/test/java/net/lab1024/sa/admin/module/ArchitectureTest.java)
