---
trigger: on_demand
description: Java 錯誤診斷與恢復流程
tags: [troubleshooting, debugging, error-recovery, archunit, quality-gate]
required_rules:
  - rules/10-architecture-rules.md

execution_order:
  - step: identify_error_type
    description: 識別錯誤類型
    validation: 明確的錯誤分類（編譯/測試/ArchUnit/QualityGate）

  - step: locate_root_cause
    description: 定位根因
    validation: 具體的文件和行號

  - step: apply_fix
    description: 應用修復方案
    validation: 代碼已修改

  - step: verify_fix
    description: 驗證修復
    commands:
      - mvn verify
    validation: BUILD SUCCESS

last_updated: 2025-01-13
---

# Java 錯誤診斷與恢復流程

> **目的**: 系統化診斷和修復編譯錯誤、測試失敗、ArchUnit 違規、Quality Gate 失敗等問題

---

## 🤖 AI 執行指南

### 何時應用此 Workflow
- ✅ 編譯失敗（compilation errors）
- ✅ 測試失敗（test failures）
- ✅ ArchUnit 測試失敗（architecture violations）
- ✅ Quality Gate 失敗（coverage/sonar/checkstyle）
- ✅ 運行時錯誤（NPE、異常）
- ✅ 依賴衝突

### 錯誤分類決策樹
```
檢測到錯誤 → 識別錯誤類型
  ├─ 編譯失敗
  │   ├─ Java 版本錯誤 → Section 1.1
  │   ├─ 依賴缺失 → Section 1.2
  │   └─ 語法錯誤 → Section 1.3
  │
  ├─ ArchUnit 測試失敗
  │   ├─ 架構違規（Controller → Repository）→ Section 2.1
  │   ├─ Optional vs Option → Section 2.2
  │   ├─ 字段注入 → Section 2.3
  │   └─ @Transactional 位置 → Section 2.4
  │
  ├─ Quality Gate 失敗
  │   ├─ 覆蓋率不足 → Section 3.1
  │   ├─ Checkstyle 錯誤 → Section 3.2
  │   └─ SonarQube 問題 → Section 3.3
  │
  └─ 運行時錯誤
      └─ NullPointerException → Section 4.1
```

---

## Section 1: 編譯錯誤診斷

### 1.1 Java 版本錯誤

#### 症狀
```
[ERROR] Failed to execute goal maven-compiler-plugin
[ERROR] Source option 21 is no longer supported
```

#### 診斷命令
```bash
java -version    # 期望: openjdk version "21.x.x"
mvn -version     # 期望: Java version: 21.x.x
echo $JAVA_HOME  # 期望: /path/to/jdk-21
```

#### AI 自動修復（macOS）
```bash
# 安裝 Java 21
brew install openjdk@21

# 設置環境變量
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

# 驗證
java -version && mvn clean compile
```

---

### 1.2 依賴缺失

#### 症狀
```
[ERROR] cannot find symbol: class Option
  location: package io.vavr.control
```

#### 診斷與修復
```bash
# 檢查依賴樹
mvn dependency:tree | grep vavr

# 清理並重新下載
mvn clean install -U

# 驗證
mvn clean compile
```

---

## Section 2: ArchUnit 測試失敗診斷

### 2.1 架構違規：Controller 直接訪問 Repository

#### AI 自動診斷流程
```bash
# 1. 運行測試查看詳細違規
mvn test -Dtest=ArchitectureTest#controllerNotAccessRepository -X

# 2. 定位違規代碼
grep -r "Repository" --include="*Controller.java" src/
```

#### AI 自動修復
```java
// ❌ 違規代碼
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository; // ❌

    @GetMapping("/{id}")
    public ResponseDTO<User> getUser(@PathVariable Long id) {
        return ResponseDTO.ok(userRepository.findById(id)); // ❌
    }
}

// ✅ AI 自動修正為
// Step 1: 創建 UserService
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }
}

// Step 2: 修改 Controller
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService; // ✅

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error("用戶不存在"));
    }
}
```

---

### 2.2 Vavr 違規：Service 使用 Optional

#### AI 自動修復
```java
// ❌ 違規
public Optional<User> findById(Long id) {
    return userMapper.findById(id);
}

// ✅ 自動修正為
public Option<User> findById(Long id) {
    return Option.ofOptional(userMapper.findById(id));
}
```

---

### 2.3 字段注入違規

#### AI 自動修復
```java
// ❌ 違規
@Service
public class UserService {
    @Autowired  // ❌ 字段注入
    private UserRepository userRepository;
}

// ✅ 自動修正為
@Service
@RequiredArgsConstructor  // ✅ 構造函數注入
public class UserService {
    private final UserMapper userMapper;  // ✅
}
```

---

## Section 3: Quality Gate 失敗診斷

### 3.1 測試覆蓋率不足

#### 診斷
```bash
# 生成覆蓋率報告
mvn clean test jacoco:report

# 打開報告
open target/site/jacoco/index.html
```

#### 修復：補充測試
```java
// 找到未測試的方法，補充測試
@Test
void shouldReturnNone_whenUserIsInactive() {
    User inactiveUser = User.builder().id(1L).active(false).build();
    when(userMapper.selectById(1L)).thenReturn(inactiveUser);

    Option<User> result = userService.findActiveById(1L);

    assertThat(result.isEmpty()).isTrue();
}
```

---

### 3.2 Checkstyle 錯誤

#### AI 批量修復
```bash
# 查看所有錯誤
mvn checkstyle:check -Dcheckstyle.console=true

# 自動格式化
mvn spotless:apply
```

---

### 3.3 SonarQube 問題修復

#### 認知複雜度過高
```java
// ❌ 複雜度 15
public void processOrder(Order order) {
    if (order != null) {
        if (order.getStatus() == Status.PENDING) {
            // 嵌套過深...
        }
    }
}

// ✅ 使用 Vavr Option 簡化
public void processOrder(Order order) {
    Option.of(order)
        .filter(o -> o.getStatus() == Status.PENDING)
        .forEach(this::doProcess);
}
```

---

## Section 4: 運行時錯誤診斷

### 4.1 NullPointerException

#### AI 自動重構
```java
// ❌ 容易 NPE
public String getUserCity(Long id) {
    User user = userRepository.findById(id);  // 可能 null
    return user.getAddress().getCity();  // NPE!
}

// ✅ 使用 Vavr Option
public String getUserCity(Long id) {
    return Option.of(userRepository.findById(id))
        .flatMap(user -> Option.of(user.getAddress()))
        .map(Address::getCity)
        .getOrElse("Unknown");
}
```

---

## 快速診斷命令

```bash
# 完整診斷流程
mvn clean verify                              # 完整構建
mvn test -Dtest=ArchitectureTest              # 架構測試
mvn jacoco:report && open target/site/jacoco/index.html  # 覆蓋率
mvn checkstyle:check                          # 代碼風格
```

---

**最後更新**: 2025-01-13
**Sprint 2 完成**: 詳細錯誤診斷流程