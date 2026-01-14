# SmartAdmin 開發規範與導航總覽

> **目標架構**: Java 21 + Spring Boot 3.5.4 + PostgreSQL 16 + Vavr 0.10.4 + MyBatis Plus 3.5.12

---

## 1. 快速開始

### 主開發目錄

**後續主要開發目錄：**
- **後端**: `smart-admin-api-java21-springboot3/` - Java 21 + Spring Boot 3 版本
- **前端**: `smart-admin-web/` - TypeScript + Vue 3 版本

> **說明**: 這些是從 `smart-admin-api-java17-springboot3` 和 `smart-admin-web-typescript` 複製而來，作為專案的主要開發分支。

### 新人入職路徑

1. **環境設定**: 從 [workflows/init.md](workflows/init.md) 開始，完成 Java 21 + PostgreSQL + Redis 環境配置
2. **架構理解**: 閱讀 [rules/10-architecture-rules.md](rules/10-architecture-rules.md) 了解分層架構
3. **學習 Vavr**: 從 [rules/08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) 開始函數式編程
4. **PostgreSQL 特性**: 參考 [rules/05-postgresql-advanced.md](rules/05-postgresql-advanced.md) 學習 JSONB 和陣列
5. **開發實踐**: 閱讀本文檔第 5 章「開發規範」了解具體編碼標準

### 環境設定檢查

```bash
# 驗證前置條件
java -version      # 期望: Java 21+
mvn -version       # 期望: Maven 3.8+, Java 21
docker --version   # 期望: Docker installed

# 啟動開發環境
cd .agent/configs && docker-compose up -d postgres redis

# 驗證架構測試
cd smart-admin-api-java21-springboot3
mvn test -Dtest=ArchitectureTest
```

---

## 2. 規範導航

### 🏗️ 核心編碼規範
基礎編碼標準（always apply）:
- [01-naming-conventions.md](rules/01-naming-conventions.md) - 命名規範
- [02-oop-principles.md](rules/02-oop-principles.md) - OOP 原則
- [03-concurrency-rules.md](rules/03-concurrency-rules.md) - 並發規則
- [04-exception-logging.md](rules/04-exception-logging.md) - 異常與日誌
- [06-sonarqube-rules.md](rules/06-sonarqube-rules.md) - SonarQube 規則
- [10-architecture-rules.md](rules/10-architecture-rules.md) - 分層架構

### 🗄️ PostgreSQL 資料庫規範（理想架構）
- [05-postgresql-basics.md](rules/05-postgresql-basics.md) - 建表與索引
- [05-postgresql-advanced.md](rules/05-postgresql-advanced.md) - JSONB、陣列、CTE、窗口函數
- [05-postgresql-mybatis-integration.md](rules/05-postgresql-mybatis-integration.md) - SQL 優化、MySQL 遷移

### 🔒 安全規範（OWASP Top 10）
- [07-owasp-top10-part1.md](rules/07-owasp-top10-part1.md) - A01-A04
- [07-owasp-top10-part2.md](rules/07-owasp-top10-part2.md) - A05-A10

### 🎯 Vavr 函數式編程（理想架構）
- [08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) - Option、Try 基礎
- [08-vavr-advanced.md](rules/08-vavr-advanced.md) - Either、集合、模式匹配
- [08-vavr-mybatis-integration.md](rules/08-vavr-mybatis-integration.md) - Vavr + MyBatis Plus

### 💾 MyBatis Plus 持久層（理想架構：LambdaQueryWrapper 為主）
- [09-mybatis-plus-core.md](rules/09-mybatis-plus-core.md) - LambdaQueryWrapper、分頁、IEnum
- [09-mybatis-plus-postgresql.md](rules/09-mybatis-plus-postgresql.md) - PostgreSQL 整合、JSONB/陣列 TypeHandler

### 🔄 開發工作流程
- [init.md](workflows/init.md) - 環境初始化設定
- [github-actions-pipeline.md](workflows/github-actions-pipeline.md) - GitHub Actions CI/CD
- [quality-gates-local-ci.md](workflows/quality-gates-local-ci.md) - 本地 Quality Gate、GitLab CI
- [tdd-workflow.md](workflows/tdd-workflow.md) - 測試驅動開發
- [java-failure-recovery.md](workflows/java-failure-recovery.md) - 錯誤恢復流程

### ⚙️ 配置參考
- [maven-dependencies.md](configs/maven-dependencies.md) - 完整依賴清單
- [docker-compose.yml](configs/docker-compose.yml) - PostgreSQL + Redis 環境
- [ArchitectureTest.java](configs/ArchitectureTest.java) - ArchUnit 測試範本

---

## 3. 專案概覽

### 後端結構
```
smart-admin-api-java21-springboot3/
├── sa-base/                          # 基礎設施庫 (357 個 Java 檔案)
│   ├── common/                       # 核心框架程式碼
│   │   ├── domain/                  # 基礎 DTO (ResponseDTO, PageParam 等)
│   │   ├── util/                    # 工具類 (SmartBeanUtil, SmartPageUtil 等)
│   │   ├── constant/                # 系統常數
│   │   └── config/                  # 26+ Spring 配置類
│   └── module/support/              # 26 個支援模組
│       ├── dict/                    # 字典管理
│       ├── file/                    # 檔案上傳下載
│       ├── operatelog/              # 操作日誌
│       ├── loginlog/                # 登入日誌
│       ├── datatracer/              # 資料變更追蹤
│       ├── config/                  # 系統配置
│       ├── reload/                  # 熱重載
│       ├── cache/                   # 快取管理
│       ├── captcha/                 # 驗證碼
│       ├── codegenerator/           # 程式碼產生器
│       └── ...                      # 更多模組
│
└── sa-admin/                         # 主應用程式 (202 個 Java 檔案)
    ├── module/
    │   ├── business/                # 業務模組（你的程式碼放這裡）
    │   └── system/                  # 系統模組（員工、角色等）
    └── config/                      # 應用專屬配置
```

### 前端結構
```
smart-admin-web/
├── src/
│   ├── api/                         # API 接口定義
│   ├── assets/                      # 靜態資源
│   ├── components/                  # 公共元件
│   ├── constants/                   # 常數和列舉
│   ├── layout/                      # 布局元件
│   ├── lib/                         # 第三方庫封裝
│   ├── router/                      # 路由配置
│   ├── store/                       # Pinia 狀態管理
│   ├── utils/                       # 工具函數
│   └── views/                       # 頁面視圖
├── public/                          # 公共資源
└── package.json                     # 專案配置
```

---

## 4. 技術棧

### 後端技術棧 (smart-admin-api-java21-springboot3)

| 技術 | 版本 | 用途 | 規則檔案 |
|------|------|------|----------|
| Java | 21 | 核心語言 | [01-naming-conventions.md](rules/01-naming-conventions.md) |
| Spring Boot | 3.5.4 | 應用框架 | [10-architecture-rules.md](rules/10-architecture-rules.md) |
| Sa-Token | 1.44.0 | 認證授權 | - |
| MyBatis Plus | 3.5.12 | ORM 框架 | [09-mybatis-plus-core.md](rules/09-mybatis-plus-core.md) |
| PostgreSQL Driver | 42.7.5 | 資料庫驅動 | [05-postgresql-basics.md](rules/05-postgresql-basics.md) |
| Vavr | 0.10.4 | 函數式編程 | [08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) |
| Knife4j | 4.6.0 | API 文檔 | - |
| Druid | 1.2.25 | 資料庫連接池 | - |
| Redisson | 3.50.0 | Redis 快取和分散式 | - |
| P6Spy | 3.9.1 | SQL 監控 | - |

### 前端技術棧 (smart-admin-web)

| 技術 | 版本 | 用途 | 規則檔案 |
|------|------|------|----------|
| Vue | 3.4.27 | 前端框架 | - |
| TypeScript | 5.6.3 | 型別系統 | - |
| Vite | 5.2.12 | 建置工具 | - |
| Ant Design Vue | 4.2.5 | UI 元件庫 | - |
| Pinia | 2.1.7 | 狀態管理 | - |
| Vue Router | 4.3.2 | 路由管理 | - |
| Node | >= 18 | 執行環境 | - |

---

## 5. 開發規範

### 5.1 後端規範

#### 5.1.1 分層架構（強制）
所有業務模組必須遵循嚴格的分層架構：

```
Controller → Service → Dao → Database
     ↓          ↓        ↓
   @Valid   @Transaction  BaseMapper
```

**❌ 禁止** Controller 直接呼叫 Dao！

**詳細規範**: [rules/10-architecture-rules.md](rules/10-architecture-rules.md)

#### 5.1.2 領域物件層次

每個業務模組使用 4 種不同的領域物件：

```java
// 1. Entity - 資料庫映射
@TableName("t_category")
public class CategoryEntity {
    @TableId(type = IdType.AUTO)
    private Long categoryId;
    // ... 自動填充的審計欄位
}

// 2. Form - 請求 DTO
public class CategoryAddForm {
    @NotBlank(message = "名稱不能為空")
    private String categoryName;
}

// 3. VO - 回應 DTO
public class CategoryVO {
    private Long categoryId;
    private String categoryName;
}

// 4. DTO - 內部傳輸（可選）
```

#### 5.1.3 回應模式

所有 Controller 方法必須返回 `ResponseDTO<T>`:

```java
return ResponseDTO.ok(data);                    // 成功
return ResponseDTO.error(UserErrorCode.XXX);    // 錯誤
throw new BusinessException(ErrorCode.XXX);     // 異常
```

#### 5.1.4 認證與權限

```java
// 需要權限檢查
@SaCheckPermission("module:action")
public ResponseDTO<T> method() { ... }

// 公開端點
@NoNeedLogin
public ResponseDTO<T> login() { ... }

// 獲取當前使用者
RequestUser user = AdminRequestUtil.getRequestUser();
```

#### 5.1.5 資料驗證

```java
// 在 Form 類中
@NotBlank(message = "不能為空")
@Length(max = 50, message = "最多50字元")
private String name;

// 在 Controller 中
public ResponseDTO<T> add(@RequestBody @Valid Form form) { ... }
```

#### 5.1.6 分頁查詢

```java
// QueryForm 必須繼承 PageParam
Page<Entity> page = SmartPageUtil.convert2PageQuery(queryForm);
page = dao.selectPage(page, wrapper);
PageResult<VO> result = SmartPageUtil.convert2PageResult(page, VO.class);
```

#### 5.1.7 Bean 轉換

```java
Entity entity = SmartBeanUtil.copy(form, Entity.class);
List<VO> voList = SmartBeanUtil.copyList(entities, VO.class);
```

#### 5.1.8 事務管理

```java
@Service
public class CategoryService {
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> add(CategoryAddForm form) {
        // 寫入操作必須加事務
    }
}
```

#### 5.1.9 函數式編程 (Vavr)

使用 Vavr 提升程式碼健壯性和可維護性：

```java
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;

@Service
public class UserService {

    // Option 替代 null 檢查
    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }

    // Try 替代 try-catch
    public Try<User> createUser(UserCreateDTO dto) {
        return Try.of(() -> {
            User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();
            userMapper.insert(user);
            return user;
        });
    }

    // Either 業務邏輯分支
    public Either<String, Order> validateAndCreateOrder(OrderDTO dto) {
        return validateStock(dto.getItems())
            .flatMap(items -> validatePayment(dto.getPayment()))
            .map(payment -> createOrder(dto));
    }
}

// Controller 層使用
@RestController
public class UserController {

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .fold(
                () -> ResponseDTO.error("使用者不存在"),
                user -> ResponseDTO.ok(user)
            );
    }
}
```

**詳細規範**:
- [rules/08-vavr-fundamentals.md](rules/08-vavr-fundamentals.md) - Option、Try 基礎
- [rules/08-vavr-advanced.md](rules/08-vavr-advanced.md) - Either、集合、模式匹配

#### 5.1.10 PostgreSQL 特性

充分利用 PostgreSQL 高級功能：

**JSONB 欄位**:
```java
// Entity
@Data
@TableName("t_order")
public class Order {
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;  // JSONB 映射
}

// Mapper 查詢
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    default List<Order> findByMetadata(String key, String value) {
        return selectList(
            new LambdaQueryWrapper<Order>()
                .apply("metadata @> '{\"" + key + "\": \"" + value + "\"}'::jsonb")
        );
    }
}
```

**陣列類型**:
```java
// Entity
@Data
@TableName("t_article")
public class Article {
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] tags;  // PostgreSQL 陣列
}

// Mapper 查詢
default List<Article> findByTag(String tag) {
    return selectList(
        new LambdaQueryWrapper<Article>()
            .apply("tags && ARRAY[{0}]::TEXT[]", tag)
    );
}
```

**資料庫配置**:
```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3?useSSL=false
    username: smartadmin
    password: SmartAdmin@2024

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO  # PostgreSQL SERIAL 策略
```

**詳細規範**:
- [rules/05-postgresql-advanced.md](rules/05-postgresql-advanced.md) - JSONB、陣列、CTE、窗口函數
- [rules/05-postgresql-mybatis-integration.md](rules/05-postgresql-mybatis-integration.md) - SQL 優化、MySQL 遷移

---

### 5.2 前端規範

#### 5.2.1 目錄結構規範

- **api/**: API 接口定義，按業務模組分檔案
- **components/**: 可複用元件，使用 PascalCase 命名
- **constants/**: 常數和列舉，避免魔法數字
- **views/**: 頁面元件，按功能模組組織

#### 5.2.2 命名規範

- **元件檔案**: PascalCase (如: `UserList.vue`)
- **工具檔案**: camelCase (如: `formatDate.ts`)
- **常數檔案**: UPPER_SNAKE_CASE (如: `API_ROUTES.ts`)

#### 5.2.3 API 呼叫規範

```typescript
// api/category.ts
import { request } from '@/utils/request';

export const categoryApi = {
  add: (data: CategoryAddForm) => request.post('/category/add', data),
  query: (params: CategoryQueryForm) => request.post('/category/query', params),
};
```

#### 5.2.4 狀態管理

使用 Pinia，按功能模組建立 store:

```typescript
// store/category.ts
import { defineStore } from 'pinia';

export const useCategoryStore = defineStore('category', {
  state: () => ({ ... }),
  actions: { ... },
});
```

#### 5.2.5 型別定義

```typescript
// types/category.ts
export interface CategoryVO {
  categoryId: number;
  categoryName: string;
}

export interface CategoryAddForm {
  categoryName: string;
  categoryType: number;
}
```

---

### 5.3 通用編碼標準

#### 5.3.1 程式碼格式化

1. **程式碼格式化**:
   - 後端: 使用 IDEA 預設格式化
   - 前端: 使用 Prettier (已配置)

2. **註解規範**:
   - 類別和方法必須有 Javadoc/JSDoc 註解
   - 複雜邏輯必須新增行內註解

3. **命名規範**:
   - 見名知意，避免縮寫
   - 布林值以 is/has/can 開頭
   - 集合以複數形式命名

**詳細規範**: [rules/01-naming-conventions.md](rules/01-naming-conventions.md)

#### 5.3.2 Java 規範

1. **套件命名**: `net.lab1024.sa.admin.module.{business|system}.{模組名}`
2. **類別命名**:
   - Controller: `{Module}Controller`
   - Service: `{Module}Service`
   - Dao: `{Module}Dao`
   - Entity: `{Module}Entity`

3. **常數定義**: 使用列舉或常數類別，避免魔法數字

#### 5.3.3 TypeScript 規範

1. **嚴格型別**: 啟用 TypeScript 嚴格模式
2. **介面優先**: 優先使用 interface 而非 type
3. **避免 any**: 必須明確型別，避免使用 any

---

### 5.4 配置管理

#### 5.4.1 後端配置

採用兩層配置系統：

1. **基礎配置**: `sa-base/src/main/resources/{env}/sa-base.yaml`
   - 資料庫、Redis、郵件、快取設定

2. **應用配置**: `sa-admin/src/main/resources/{env}/application.yaml`
   - 應用專屬設定
   - 覆寫基礎配置

**環境**: dev (預設), test, pre, prod

建置命令: `mvn clean package -P {env}`

#### 5.4.2 前端配置

環境配置檔案:
- `.env.localhost` - 本地開發
- `.env.development` - 開發環境
- `.env.test` - 測試環境
- `.env.pre` - 預發布環境
- `.env.production` - 正式環境

---

### 5.5 資料庫標準

#### 5.5.1 表命名
- 統一使用小寫加底線: `t_{模組名}`
- 範例: `t_category`, `t_employee`

#### 5.5.2 欄位規範
所有表必須包含以下審計欄位（由框架自動填充）:
```sql
created_time DATETIME,
created_by BIGINT,
updated_time DATETIME,
updated_by BIGINT,
deleted_flag TINYINT(1) DEFAULT 0
```

#### 5.5.3 主鍵規範
- 使用自增主鍵: `{table_name}_id BIGINT PRIMARY KEY AUTO_INCREMENT`
- Entity 中使用 `@TableId(type = IdType.AUTO)`

**詳細規範**: [rules/05-postgresql-basics.md](rules/05-postgresql-basics.md)

---

### 5.6 Git 標準

#### 5.6.1 分支管理
- **main**: 正式分支
- **develop**: 開發分支
- **feature/{功能名}**: 功能分支
- **hotfix/{問題描述}**: 緊急修復分支

#### 5.6.2 提交訊息
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 類型:**
- feat: 新功能
- fix: 修復
- docs: 文件
- style: 格式
- refactor: 重構
- test: 測試
- chore: 建置/工具

**範例:**
```
feat(category): 新增類目管理功能

- 實現類目增刪改查
- 新增權限控制
- 完成單元測試

Closes #123
```

---

### 5.7 測試標準

#### 5.7.1 後端測試
```java
@SpringBootTest
public class CategoryServiceTest extends AdminApplicationTest {
    @Resource
    private CategoryService service;

    @Test
    public void testAdd() {
        CategoryAddForm form = new CategoryAddForm();
        form.setName("測試類目");
        ResponseDTO<String> result = service.add(form);
        Assertions.assertTrue(result.getOk());
    }
}
```

#### 5.7.2 前端測試
- 單元測試: Vitest
- E2E 測試: 按需配置

**詳細規範**: [workflows/tdd-workflow.md](workflows/tdd-workflow.md)

---

### 5.8 API 文件標準

#### 5.8.1 Swagger 註解
```java
@Tag(name = "類目管理")
@RestController
public class CategoryController {

    @Operation(summary = "新增類目")
    @PostMapping("/category/add")
    @SaCheckPermission("category:add")
    public ResponseDTO<String> add(@RequestBody @Valid CategoryAddForm form) {
        return service.add(form);
    }
}
```

---

### 5.9 安全標準

1. **密碼加密**: 使用 BCrypt
2. **傳輸加密**: 支援國產加密演算法和 AES
3. **SQL 注入**: 使用 MyBatis 參數綁定
4. **XSS 防護**: 前端輸出跳脫
5. **CSRF**: Sa-Token 自動處理

**詳細規範**:
- [rules/07-owasp-top10-part1.md](rules/07-owasp-top10-part1.md)
- [rules/07-owasp-top10-part2.md](rules/07-owasp-top10-part2.md)

---

### 5.10 效能優化

1. **快取策略**: 使用 Caffeine (本地) + Redis (分散式)
2. **分頁查詢**: 必須使用 PageParam 和 SmartPageUtil
3. **N+1 問題**: 注意 MyBatis 關聯查詢
4. **批次操作**: 使用 MyBatis Plus 的 saveBatch
5. **索引優化**: 高頻查詢欄位新增索引

---

### 5.11 常用命令

#### 5.11.1 後端命令
```bash
# 進入 Java 21 專案目錄
cd smart-admin-api-java21-springboot3

# 建置專案
mvn clean package -P dev

# 執行應用
cd sa-admin && mvn spring-boot:run

# 執行測試
mvn test

# 存取 API 文件
# http://localhost:1024/swagger-ui.html
```

#### 5.11.2 前端命令
```bash
# 進入前端專案目錄
cd smart-admin-web

# 安裝依賴
npm install

# 本地開發
npm run localhost

# 開發環境
npm run dev

# 建置測試環境
npm run build:test

# 建置正式環境
npm run build:prod
```

---

## 6. AI 規則系統

### 6.1 定位說明

`.agent` 文檔定義的是 **理想目標架構**，用於引導專案技術演進：

- **理想架構** (`positioning: ideal`): PostgreSQL + Vavr + LambdaQueryWrapper
- **當前實作**: MySQL + 傳統 Java + XML Mapper（見實際程式碼）
- **遷移路徑** (`positioning: migration`): 提供從當前實作遷移至理想架構的指南

### 6.2 檔案組織原則

1. **字元限制**: 所有 rules/*.md 和 workflows/*.md ≤ 11000 字元（特殊索引檔案除外）
2. **平鋪結構**: rules/ 和 workflows/ 不使用子目錄
3. **前置依賴**: 透過 front matter 的 `prerequisites` 欄位標註
4. **交叉引用**: 透過 `related_rules` 和 `related_workflows` 欄位連結
5. **定位標籤**: `ideal`（理想架構）/ `migration`（遷移支援）/ `current-standard`（當前必須遵守）
6. **Workflow 依賴**: 所有 workflow 必須在 `required_rules` 中指定依賴的 rules
7. **AI 指令區塊**: 所有 rules 必須包含 `🤖 AI 指令區塊`，幫助 AI 理解規則應用場景

### 6.3 檔案命名規則

- **編號前綴**: 使用原始編號（01-10）維持連貫性
- **拆分檔案**: 同編號 + 後綴說明主題
  - 例: `05-postgresql-basics.md`, `05-postgresql-advanced.md`
  - 例: `08-vavr-fundamentals.md`, `08-vavr-advanced.md`
- **便於識別**: 同編號檔案屬於相同主題系列

### 6.4 規則格式規範 v2.0

#### 6.4.1 Front Matter 必填欄位

所有 rules 和 workflows 檔案必須包含以下 front matter 欄位：

```yaml
---
trigger: always_on | on_demand
description: [簡短描述，50字以內]
tags: [tag1, tag2, tag3]
positioning: current-standard | ideal | migration

# AI 執行控制（rules 專用）
ai_role: code_reviewer_and_generator | orchestrator | code_reviewer
auto_apply: true | false
ask_before_fix: true | false

# 依賴關係
prerequisites:
  - rules/XX-prerequisite.md

conflicts_with:
  - none

related_rules:
  - rules/XX-related.md

# 自動化檢查
archunit_test: ArchitectureTest#testMethodName
checkstyle_rule: RuleName
spotbugs_rule: RuleName

last_updated: YYYY-MM-DD
---
```

#### 6.4.2 AI 指令區塊結構

每個 rule 檔案必須在 front matter 後包含 `🤖 AI 指令區塊`：

```markdown
## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 場景 1
- ✅ 場景 2

### 強制執行檢查清單
- [ ] 檢查項 1
- [ ] 檢查項 2

### AI 決策樹
```
[決策流程圖]
```

### 錯誤模式檢測與自動修正
[程式碼範例]

### 生成程式碼標準模板
[程式碼模板]

### 驗證命令
[自動化檢查命令]
```

### 6.5 如何新增規則

1. **使用驗證腳本檢查格式**：
   ```bash
   # 驗證單個檔案
   ./.agent/scripts/validate-rule.sh rules/XX-new-rule.md

   # 驗證所有檔案
   ./.agent/scripts/validate-rule.sh
   ```

2. **必須包含的元素**：
   - ✅ 完整的 front matter（所有必填欄位）
   - ✅ AI 指令區塊（幫助 AI 理解如何應用規則）
   - ✅ 錯誤模式檢測（至少 2-3 個常見錯誤範例）
   - ✅ 程式碼模板（生成程式碼時的標準格式）
   - ✅ 驗證命令（自動化檢查命令）

3. **檔案大小限制**：
   - Rules: ≤ 11000 字元（特殊索引檔案除外）
   - Workflows: ≤ 11000 字元

4. **命名規範**：
   - 使用原始編號維持連貫性（01-10）
   - 同編號 + 後綴說明主題（如 `05-postgresql-basics.md`）

---

## 7. 質量保證

### 7.1 ArchUnit 測試

所有架構規則都在 `.agent/configs/ArchitectureTest.java` 中自動驗證：

```bash
# 執行完整架構測試
mvn test -Dtest=ArchitectureTest

# 執行特定測試
mvn test -Dtest=ArchitectureTest#serviceUsesVavrOption
mvn test -Dtest=ArchitectureTest#noJavaOptionalInService
mvn test -Dtest=ArchitectureTest#layerDependencies
```

#### 7.1.1 強制規則（阻斷 PR）

- ✅ Service 層必須使用 `io.vavr.control.Option` 而非 `java.util.Optional`
- ✅ Controller 不能直接存取 Repository
- ✅ 禁止欄位注入（使用建構函數注入）
- ✅ @Transactional 只在 Service 層
- ✅ 命名規範（Controller/Service/Dao 後綴）
- ✅ Domain 層無 Spring 依賴
- ✅ 無循環依賴

**完整測試**: [configs/ArchitectureTest.java](configs/ArchitectureTest.java)

### 7.2 測試覆蓋率

**目標**: ≥ 80% 程式碼覆蓋率

```bash
# 生成測試覆蓋率報告
mvn clean test jacoco:report

# 報告位置
open target/site/jacoco/index.html
```

### 7.3 程式碼品質檢查

```bash
# Checkstyle 檢查
mvn checkstyle:check

# PMD 檢查
mvn pmd:check

# SpotBugs 檢查
mvn spotbugs:check

# 完整驗證（測試 + 覆蓋率）
mvn verify
```

**詳細規範**: [workflows/quality-gates-local-ci.md](workflows/quality-gates-local-ci.md)

---

## 8. 快速參考

### 8.1 環境設定快速檢查

```bash
# ✅ 前置條件
java -version      # Java 21+
mvn -version       # Maven 3.8+, Java 21
docker --version   # Docker installed

# ✅ 啟動資料庫
cd .agent/configs
docker-compose up -d postgres redis

# ✅ 驗證連線
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3

# ✅ 建置專案
cd smart-admin-api-java21-springboot3
mvn clean compile

# ✅ 執行架構測試
mvn test -Dtest=ArchitectureTest
```

### 8.2 開發流程快速檢查

```bash
# 1️⃣ 建立功能分支
git checkout -b feature/your-feature

# 2️⃣ 開發並遵守規範
# - 使用 Vavr Option/Try 替代 null/try-catch
# - 使用 LambdaQueryWrapper 建構查詢
# - 遵守分層架構 (Controller → Service → Dao)

# 3️⃣ 執行測試和品質檢查
mvn clean verify
mvn test -Dtest=ArchitectureTest

# 4️⃣ 提交程式碼
git add .
git commit -m "feat(module): 功能描述"

# 5️⃣ 推送並建立 PR
git push origin feature/your-feature
```

### 8.3 常見問題快速解決

**Q1: Port 1024 已被佔用**
```yaml
# 修改 sa-admin/src/main/resources/dev/application.yaml
server:
  port: 8080  # 改為其他埠
```

**Q2: PostgreSQL 連線失敗**
```bash
# 確認 PostgreSQL 執行狀態
docker-compose ps postgres

# 檢視 PostgreSQL 日誌
docker-compose logs postgres

# 測試連線
docker exec -it smartadmin-postgres psql -U smartadmin -d smart_admin_v3
```

**Q3: Redis 連線失敗**
```bash
# 啟動 Redis
docker-compose up -d redis

# 或在配置中停用 Redis（開發階段可選）
```

**Q4: ArchUnit 測試失敗**
常見原因：
- Service 層使用了 `java.util.Optional`（應使用 `io.vavr.control.Option`）
- 欄位注入（應使用建構函數注入）
- 引入了 MySQL 驅動（應使用 PostgreSQL）

解決：檢視測試報告，按規範修復程式碼

**Q5: Maven 依賴下載緩慢**
```xml
<!-- 配置 Maven 鏡像（阿里雲）-->
<!-- 檔案: ~/.m2/settings.xml -->
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 8.4 相關文件連結

**官方資源**:
- **官方文件**: https://smartadmin.vip
- **線上預覽**: https://preview.smartadmin.vip

**內部文件**:
- **初始化指南**: [workflows/init.md](workflows/init.md)
- **開發者入門**: [workflows/onboarding.md](workflows/onboarding.md)
- **常用任務**: [workflows/common-tasks.md](workflows/common-tasks.md)
- **完整編碼規範**: 見本文檔第 2 章「規範導航」

**配置文件**:
- **Maven 依賴**: [configs/maven-dependencies.md](configs/maven-dependencies.md)
- **Docker 環境**: [configs/docker-compose.yml](configs/docker-compose.yml)
- **架構測試**: [configs/ArchitectureTest.java](configs/ArchitectureTest.java)

---

**最後更新**: 2025-01-14
**版本**: v2.0 (合併版)
**記住**: 我們推崇高品質的程式碼。身為開發者，程式碼即利劍！ ⚔️
