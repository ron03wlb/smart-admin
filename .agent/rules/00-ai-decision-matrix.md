---
trigger: always_on
description: AI 決策矩陣 - 場景到規則的映射
tags: [meta, ai-guide, decision-tree, orchestration]
positioning: current-standard
ai_role: orchestrator
auto_apply: true
ask_before_fix: false
prerequisites: []
conflicts_with: []
related_rules:
  - rules/01-naming-conventions.md
  - rules/10-architecture-rules.md
  - rules/08-vavr-fundamentals.md
archunit_test: none
checkstyle_rule: none
spotbugs_rule: none
last_updated: 2025-01-13
---

# AI 決策矩陣

> **目的**: 幫助 AI 快速識別用戶請求場景，並自動應用正確的規則組合

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ **永遠**: 此規則在每次響應前都應該被查閱
- ✅ 用戶發起任何代碼生成請求時
- ✅ 用戶要求 Code Review 時
- ✅ 用戶報告錯誤需要診斷時
- ✅ 不確定應該應用哪些規則時

### 強制執行檢查清單
在響應用戶請求前，必須：
- [ ] 識別請求類型（生成代碼/審查/診斷/查詢）
- [ ] 確定涉及的代碼層次（Controller/Service/Repository/Entity）
- [ ] 列出需要應用的所有規則
- [ ] 確認是否有衝突的規則
- [ ] 按優先級順序應用規則

### AI 決策樹

```
用戶請求分類
├─ 1️⃣ 生成新代碼
│   ├─ Controller 層
│   │   ├─ 應用規則: [01-naming, 10-architecture (RESTful), 04-exception-logging]
│   │   ├─ 檢查點: 不直接訪問 Repository, 使用 @Valid, 返回 ResponseDTO
│   │   └─ ArchUnit: controllerNotAccessRepository, controllerNaming
│   │
│   ├─ Service 層
│   │   ├─ 應用規則: [01-naming, 02-oop, 08-vavr-fundamentals, 10-architecture]
│   │   ├─ 檢查點: 返回 Option/Try, 構造函數注入, @Transactional
│   │   └─ ArchUnit: serviceUsesVavrOption, noFieldInjection
│   │
│   ├─ Repository 層 (Dao/Mapper)
│   │   ├─ 應用規則: [01-naming, 09-mybatis-plus-core, 05-postgresql-mybatis]
│   │   ├─ 檢查點: extends BaseMapper, LambdaQueryWrapper, 無業務邏輯
│   │   └─ ArchUnit: daoNaming
│   │
│   └─ Entity 層
│       ├─ 應用規則: [01-naming, 05-postgresql-basics, 09-mybatis-plus-core]
│       ├─ 檢查點: @TableName, @TableId(AUTO), JSONB/Array TypeHandler
│       └─ ArchUnit: 無 Spring 依賴
│
├─ 2️⃣ Code Review
│   ├─ 架構違規檢查
│   │   ├─ 應用規則: [10-architecture-rules]
│   │   ├─ 檢查: Controller → Service → Repository 層級
│   │   └─ 運行: mvn test -Dtest=ArchitectureTest
│   │
│   ├─ 命名規範檢查
│   │   ├─ 應用規則: [01-naming-conventions]
│   │   ├─ 檢查: CamelCase, 常量大寫, 方法前綴
│   │   └─ 運行: mvn checkstyle:check
│   │
│   ├─ Vavr 使用檢查
│   │   ├─ 應用規則: [08-vavr-fundamentals, 08-vavr-advanced]
│   │   ├─ 檢查: Option 替代 Optional, Try 替代 try-catch
│   │   └─ ArchUnit: serviceUsesVavrOption, noJavaOptionalInService
│   │
│   ├─ OOP 原則檢查
│   │   ├─ 應用規則: [02-oop-principles]
│   │   └─ 檢查: 單一職責, 依賴注入, 接口隔離
│   │
│   ├─ 並發安全檢查
│   │   ├─ 應用規則: [03-concurrency-rules]
│   │   └─ 檢查: 線程安全, 不可變對象, 並發工具使用
│   │
│   └─ 安全檢查
│       ├─ 應用規則: [07-owasp-top10-part1, 07-owasp-top10-part2]
│       └─ 檢查: SQL 注入, XSS, CSRF, 認證授權
│
├─ 3️⃣ 數據庫操作
│   ├─ 建表/修改表結構
│   │   ├─ 應用規則: [05-postgresql-basics]
│   │   ├─ 檢查點: 主鍵 BIGSERIAL, 審計字段, 索引策略
│   │   └─ 輸出: CREATE TABLE 語句 + Entity 類
│   │
│   ├─ 複雜查詢 (JSONB/窗口函數/CTE)
│   │   ├─ 應用規則: [05-postgresql-advanced]
│   │   ├─ 檢查點: JSONB 操作符, 窗口函數語法, CTE 可讀性
│   │   └─ 輸出: 優化的 SQL + 性能說明
│   │
│   ├─ MyBatis Mapper 編寫
│   │   ├─ 應用規則: [09-mybatis-plus-core, 09-mybatis-plus-postgresql]
│   │   ├─ 檢查點: LambdaQueryWrapper, TypeHandler, 分頁
│   │   └─ 輸出: Mapper 接口 + 類型安全查詢
│   │
│   └─ MySQL 遷移 PostgreSQL
│       ├─ 應用規則: [05-postgresql-mybatis-integration]
│       ├─ 檢查點: 類型映射, 自增 ID, JSONB 替換 JSON
│       └─ 輸出: 遷移腳本 + 代碼適配
│
├─ 4️⃣ 錯誤診斷
│   ├─ 編譯錯誤
│   │   ├─ 應用 Workflow: [java-failure-recovery]
│   │   ├─ 診斷步驟: 檢查 Java 版本 → Maven 配置 → 依賴衝突
│   │   └─ 輸出: 診斷報告 + 修復命令
│   │
│   ├─ ArchUnit 測試失敗
│   │   ├─ 應用規則: [10-architecture-rules] + 對應違規規則
│   │   ├─ 診斷: 解析錯誤信息 → 定位違規代碼 → 提供修復方案
│   │   └─ 輸出: 修復後的代碼 + 解釋
│   │
│   ├─ Quality Gate 失敗
│   │   ├─ 應用 Workflow: [quality-gates-local-ci]
│   │   ├─ 診斷: 覆蓋率 → SonarQube 問題 → 重複代碼
│   │   └─ 輸出: 本地複現步驟 + 修復建議
│   │
│   └─ 運行時錯誤 (NPE/異常)
│       ├─ 應用規則: [08-vavr-fundamentals (Try), 04-exception-logging]
│       ├─ 診斷: 堆棧追蹤分析 → 根因定位
│       └─ 輸出: Vavr 重構方案 + 日誌增強
│
└─ 5️⃣ 知識查詢
    ├─ PostgreSQL 特性
    │   └─ 應用規則: [05-postgresql-*]
    │
    ├─ Vavr 用法
    │   └─ 應用規則: [08-vavr-*]
    │
    ├─ MyBatis Plus 用法
    │   └─ 應用規則: [09-mybatis-plus-*]
    │
    └─ 架構設計
        └─ 應用規則: [10-architecture-rules]
```

---

## 📋 強制檢查矩陣

### 代碼生成時的強制約束

| 代碼類型 | 必須應用的規則 | 自動化工具 | 阻斷級別 |
|---------|--------------|-----------|---------|
| **Service 新方法** | 08-vavr (返回 Option/Try) | ArchUnit: `serviceUsesVavrOption` | 🚫 阻斷PR |
| **Service 新方法** | 10-architecture (構造函數注入) | ArchUnit: `noFieldInjection` | 🚫 阻斷PR |
| **Service 寫操作** | 10-architecture (@Transactional) | ArchUnit: `transactionalOnlyInService` | 🚫 阻斷PR |
| **Controller 新方法** | 10-architecture (不直接訪問Repo) | ArchUnit: `controllerNotAccessRepository` | 🚫 阻斷PR |
| **Controller 參數** | 08-vavr (禁止 Option 參數) | ArchUnit: `noOptionInControllerParams` | 🚫 阻斷PR |
| **新建 Entity** | 05-postgresql (JSONB/Array TypeHandler) | Code Review | ⚠️ 建議 |
| **新建 Entity** | 05-postgresql (主鍵 BIGSERIAL) | Code Review | ⚠️ 建議 |
| **Repository 方法** | 09-mybatis (LambdaQueryWrapper) | Code Review | ⚠️ 建議 |
| **任何新代碼** | 01-naming (命名規範) | Checkstyle | 🚫 阻斷PR |
| **任何新代碼** | 測試覆蓋率 ≥ 80% | JaCoCo | 🚫 阻斷PR |
| **任何新代碼** | 06-sonarqube (無嚴重問題) | SonarQube | 🚫 阻斷PR |

### 質量門禁標準

```yaml
Quality Gate 通過條件:
  ✅ ArchUnit:        100% 通過 (零容忍)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ 測試覆蓋率:       ≥ 80% (Line), ≥ 70% (Branch)
  ✅ SonarQube:       0 Blocker/Critical issues
  ✅ 代碼重複率:       < 3%
  ✅ 技術債務比率:     < 5%
```

---

## 🔧 AI 自動修正策略

### 可以自動修正的問題 (auto_apply: true)

```java
// 1️⃣ 檢測到: Optional 返回類型
// ❌ 原代碼
public Optional<User> findById(Long id) {
    return userRepository.findById(id);
}

// ✅ 自動修正為
public Option<User> findById(Long id) {
    return Option.ofOptional(userRepository.findById(id));
}

// 2️⃣ 檢測到: try-catch 異常處理
// ❌ 原代碼
public String readFile(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("文件讀取失敗", e);
        return null;
    }
}

// ✅ 自動修正為
public Try<String> readFile(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("文件讀取失敗", e));
}

// 3️⃣ 檢測到: 字段注入
// ❌ 原代碼
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
}

// ✅ 自動修正為
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
}

// 4️⃣ 檢測到: 命名不規範
// ❌ 原代碼
public class userService { }
public void GetUserById() { }
private final int MAX_COUNT = 100; // 非常量

// ✅ 自動修正為
public class UserService { }
public void getUserById() { }
private static final int MAX_COUNT = 100;
```

### 需要詢問用戶的問題 (ask_before_fix: true)

```java
// 1️⃣ 檢測到: Service 方法缺少 @Transactional
public ResponseDTO<String> createUser(UserCreateDTO dto) {
    // 包含多個寫操作...
}
// 👉 詢問: "此方法包含寫操作，是否需要添加 @Transactional？"

// 2️⃣ 檢測到: Controller 直接訪問 Repository
@RestController
public class UserController {
    private final UserRepository userRepository; // ❌
}
// 👉 詢問: "檢測到 Controller 直接依賴 Repository，是否需要創建 UserService？"

// 3️⃣ 檢測到: Entity 缺少 JSONB TypeHandler
@TableName("t_order")
public class Order {
    private String metadata; // 實際是 JSONB
}
// 👉 詢問: "metadata 字段是否為 JSONB 類型？需要添加 TypeHandler 嗎？"

// 4️⃣ 檢測到: 複雜業務邏輯在 Controller
@PostMapping("/orders")
public ResponseDTO<Order> createOrder(@RequestBody OrderDTO dto) {
    // 超過 30 行業務邏輯...
}
// 👉 詢問: "檢測到複雜業務邏輯，是否需要重構到 Service 層？"
```

### 禁止自動修正的問題 (必須人工確認)

- 🚫 **業務邏輯錯誤** - 需要理解業務含義
- 🚫 **安全漏洞** - 需要詳細的安全分析
- 🚫 **性能問題** - 需要 Profiling 數據支持
- 🚫 **數據庫遷移** - 涉及數據安全，必須人工審核
- 🚫 **刪除代碼** - 可能影響現有功能

---

## 🎯 典型場景完整流程

### 場景 A: 用戶要求 "創建用戶查詢接口"

```
AI 執行流程:
1️⃣ 識別場景: 生成新代碼 (Controller + Service + Repository)
2️⃣ 確定涉及層次: Controller → Service → Repository → Entity
3️⃣ 應用規則組合:
   - 01-naming-conventions (所有層)
   - 10-architecture-rules (所有層)
   - 08-vavr-fundamentals (Service 層)
   - 09-mybatis-plus-core (Repository 層)

4️⃣ 生成代碼:
   a. Entity (如果不存在)
   b. Repository extends BaseMapper
   c. Service 返回 Option<User>
   d. Controller 處理 Option 返回 ResponseDTO

5️⃣ 自動檢查:
   - ArchUnit 測試是否通過
   - Checkstyle 是否通過
   - 命名是否規範

6️⃣ 輸出給用戶:
   - 完整代碼
   - ArchUnit 驗證命令
   - 建議的測試用例
```

### 場景 B: Code Review - 發現 Optional 使用

```
AI 執行流程:
1️⃣ 識別場景: Code Review (Vavr 檢查)
2️⃣ 應用規則: 08-vavr-fundamentals
3️⃣ 檢測錯誤:
   public Optional<User> findById(Long id) // ❌

4️⃣ 判斷: auto_apply: true → 自動修正
5️⃣ 修正代碼:
   public Option<User> findById(Long id) {
       return Option.of(userMapper.selectById(id));
   }

6️⃣ 輸出給用戶:
   - 修正後的代碼
   - 解釋為什麼修正
   - ArchUnit 測試驗證: mvn test -Dtest=ArchitectureTest#serviceUsesVavrOption
```

### 場景 C: ArchUnit 測試失敗

```
AI 執行流程:
1️⃣ 識別場景: 錯誤診斷 (ArchUnit 失敗)
2️⃣ 應用 Workflow: java-failure-recovery
3️⃣ 解析錯誤信息:
   Architecture Violation: Controller UserController accesses Repository UserRepository

4️⃣ 定位規則: 10-architecture-rules (controllerNotAccessRepository)
5️⃣ 分析根因: Controller 直接注入了 Repository
6️⃣ 生成修復方案:
   - 創建 UserService
   - 修改 Controller 依賴 UserService
   - 移除 Controller 中的 Repository 依賴

7️⃣ 輸出給用戶:
   - 診斷報告
   - 完整修復代碼
   - 驗證命令: mvn test -Dtest=ArchitectureTest
```

---

## 📊 規則優先級矩陣

當多個規則同時適用時，按以下優先級應用：

### 優先級 P0 (最高) - 架構安全
1. **10-architecture-rules** - 分層架構必須正確
2. **07-owasp-top10-\*** - 安全漏洞零容忍
3. **ArchUnit 測試** - 架構約束強制執行

### 優先級 P1 (高) - 代碼質量
4. **08-vavr-fundamentals** - 新代碼強制使用 Vavr
5. **01-naming-conventions** - 命名規範統一
6. **02-oop-principles** - OOP 原則遵守
7. **測試覆蓋率** - ≥ 80% 強制要求

### 優先級 P2 (中) - 最佳實踐
8. **09-mybatis-plus-core** - LambdaQueryWrapper 優先
9. **05-postgresql-advanced** - PostgreSQL 高級特性
10. **04-exception-logging** - 日誌與異常處理

### 優先級 P3 (低) - 優化建議
11. **03-concurrency-rules** - 並發場景優化
12. **06-sonarqube-rules** - 代碼質量細節

---

## 🔍 衝突檢測與解決

### 規則衝突情況

| 衝突場景 | 規則 A | 規則 B | 解決方案 |
|---------|--------|--------|---------|
| Optional vs Option | Java 標準庫 | 08-vavr | **強制使用 Option** (P1) |
| 傳統異常 vs Try | try-catch | 08-vavr | **新代碼使用 Try** (P1) |
| XML Mapper vs Lambda | MyBatis XML | 09-mybatis | **新代碼使用 Lambda** (P2) |
| MySQL vs PostgreSQL | 舊配置 | 05-postgresql | **新項目用 PG，舊項目漸進遷移** |

### 衝突解決原則
1. **安全 > 性能 > 可讀性** - 優先保證安全性
2. **架構約束 > 代碼風格** - 架構正確性優先
3. **新代碼高標準 > 舊代碼兼容** - 新代碼必須符合理想架構
4. **自動化驗證 > 人工審查** - 能自動檢查的必須自動檢查

---

## 📚 快速參考卡

### 我是 AI，當我看到這些關鍵詞時...

| 關鍵詞 | 立即應用的規則 | 檢查點 |
|--------|--------------|--------|
| "創建 Controller" | 01, 10, 04 | RESTful, 不直接訪問 Repo |
| "創建 Service" | 01, 02, 08, 10 | Option/Try, 構造注入, @Transactional |
| "創建 Mapper" | 01, 09, 05 | LambdaQueryWrapper, TypeHandler |
| "創建 Entity" | 01, 05 | @TableName, JSONB/Array |
| "Code Review" | 10, 08, 01 | ArchUnit, Vavr, 命名 |
| "JSONB" | 05-advanced, 09-postgresql | TypeHandler, @> 操作符 |
| "異常處理" | 08-vavr, 04 | Try.of(), 日誌記錄 |
| "null 檢查" | 08-vavr | Option.of(), flatMap |
| "數據庫查詢" | 09, 05 | LambdaQueryWrapper, 類型安全 |
| "事務" | 10 | @Transactional 在 Service |

---

## ✅ 執行檢查清單

在每次生成代碼或審查代碼後，AI 必須確認：

### 基礎檢查
- [ ] 所有類名使用 UpperCamelCase
- [ ] 所有方法名使用 lowerCamelCase
- [ ] 所有常量全大寫下劃線分隔
- [ ] 沒有字段注入（使用構造函數注入）

### 架構檢查
- [ ] Controller 不直接訪問 Repository
- [ ] Service 方法返回 Option/Try（不是 Optional）
- [ ] Controller 參數不使用 Option
- [ ] @Transactional 只在 Service 層

### 質量檢查
- [ ] 所有方法有 JavaDoc（公共方法）
- [ ] 異常使用 Try 處理或合理拋出
- [ ] 日誌使用 slf4j (@Slf4j)
- [ ] 無 null 檢查（使用 Option）

### 自動化檢查命令
```bash
# 1. ArchUnit 測試（必須通過）
mvn test -Dtest=ArchitectureTest

# 2. Checkstyle（必須 0 errors）
mvn checkstyle:check

# 3. 測試覆蓋率（必須 ≥ 80%）
mvn clean test jacoco:report
mvn jacoco:check -Djacoco.minimum=0.80

# 4. 本地 Quality Gate
mvn verify
```

---

**此決策矩陣是所有規則的總入口，AI 在處理任何請求前都應該先查閱此文檔。**
