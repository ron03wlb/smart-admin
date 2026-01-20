# 開發規範總覽

> 本文檔整理了 SmartAdmin 專案的完整編碼標準，包括後端、前端、資料庫、Git、測試等各方面規範。

**📖 閱讀指南**:
- 本文提供 **開發規範概述**，適合快速瀏覽和新人入職
- 完整規範和 AI 決策邏輯請參考 [rules/](../rules/) 目錄下的詳細文檔
- 常用命令速查請參考 [quick-reference.md](quick-reference.md)
- 遇到問題請參考 [faq-troubleshooting.md](faq-troubleshooting.md)

---

## 1. 後端規範

### 1.1 分層架構（強制）

所有業務模組必須遵循嚴格的分層架構：

```
Controller → Service → Dao → Database
     ↓          ↓        ↓
   @Valid   @Transaction  BaseMapper
```

**❌ 禁止** Controller 直接呼叫 Dao！

**詳細規範**: [rules/10-architecture-rules.md](../rules/10-architecture-rules.md)

### 1.2 領域物件層次

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

### 1.3 回應模式

所有 Controller 方法必須返回 `ResponseDTO<T>`:

```java
return ResponseDTO.ok(data);                    // 成功
return ResponseDTO.error(UserErrorCode.XXX);    // 錯誤
throw new BusinessException(ErrorCode.XXX);     // 異常
```

### 1.4 認證與權限

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

### 1.5 資料驗證

```java
// 在 Form 類中
@NotBlank(message = "不能為空")
@Length(max = 50, message = "最多50字元")
private String name;

// 在 Controller 中
public ResponseDTO<T> add(@RequestBody @Valid Form form) { ... }
```

### 1.6 分頁查詢

```java
// QueryForm 必須繼承 PageParam
Page<Entity> page = SmartPageUtil.convert2PageQuery(queryForm);
page = dao.selectPage(page, wrapper);
PageResult<VO> result = SmartPageUtil.convert2PageResult(page, VO.class);
```

### 1.7 Bean 轉換

```java
Entity entity = SmartBeanUtil.copy(form, Entity.class);
List<VO> voList = SmartBeanUtil.copyList(entities, VO.class);
```

### 1.8 事務管理

```java
@Service
public class CategoryService {
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> add(CategoryAddForm form) {
        // 寫入操作必須加事務
    }
}
```

### 1.9 函數式編程 (Vavr)

**Option 替代 null 檢查**:
```java
import io.vavr.control.Option;

public Option<User> findById(Long id) {
    return Option.of(userMapper.selectById(id));
}

// Controller 使用
return userService.findById(id)
    .map(UserVO::from)
    .fold(
        () -> ResponseDTO.error("使用者不存在"),
        user -> ResponseDTO.ok(user)
    );
```

**Try 替代 try-catch**:
```java
import io.vavr.control.Try;

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
```

**詳細規範**:
- [rules/08-vavr-fundamentals.md](../rules/08-vavr-fundamentals.md) - Option、Try 基礎
- [rules/08-vavr-advanced.md](../rules/08-vavr-advanced.md) - Either、集合、模式匹配

### 1.10 PostgreSQL 特性

**JSONB 欄位**:
```java
// Entity
@Data
@TableName("t_order")
public class Order {
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;  // JSONB 映射
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
```

**詳細規範**:
- [rules/05-postgresql-advanced.md](../rules/05-postgresql-advanced.md)
- [rules/05-postgresql-mybatis-integration.md](../rules/05-postgresql-mybatis-integration.md)

---

## 2. 前端規範

### 2.1 目錄結構規範

- **api/**: API 接口定義，按業務模組分檔案
- **components/**: 可複用元件，使用 PascalCase 命名
- **constants/**: 常數和列舉，避免魔法數字
- **views/**: 頁面元件，按功能模組組織

### 2.2 命名規範

- **元件檔案**: PascalCase (如: `UserList.vue`)
- **工具檔案**: camelCase (如: `formatDate.ts`)
- **常數檔案**: UPPER_SNAKE_CASE (如: `API_ROUTES.ts`)

### 2.3 API 呼叫規範

```typescript
// api/category.ts
import { request } from '@/utils/request';

export const categoryApi = {
  add: (data: CategoryAddForm) => request.post('/category/add', data),
  query: (params: CategoryQueryForm) => request.post('/category/query', params),
};
```

### 2.4 狀態管理

使用 Pinia，按功能模組建立 store:

```typescript
// store/category.ts
import { defineStore } from 'pinia';

export const useCategoryStore = defineStore('category', {
  state: () => ({ ... }),
  actions: { ... },
});
```

### 2.5 型別定義

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

## 3. 通用編碼標準

### 3.1 程式碼格式化

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

**詳細規範**: [rules/01-naming-conventions.md](../rules/01-naming-conventions.md)

### 3.2 Java 規範

1. **套件命名**: `net.lab1024.sa.admin.module.{business|system}.{模組名}`
2. **類別命名**:
   - Controller: `{Module}Controller`
   - Service: `{Module}Service`
   - Dao: `{Module}Dao`
   - Entity: `{Module}Entity`

3. **常數定義**: 使用列舉或常數類別，避免魔法數字

### 3.3 TypeScript 規範

1. **嚴格型別**: 啟用 TypeScript 嚴格模式
2. **介面優先**: 優先使用 interface 而非 type
3. **避免 any**: 必須明確型別，避免使用 any

---

## 4. 配置管理

### 4.1 後端配置

採用兩層配置系統：

1. **基礎配置**: `sa-base/src/main/resources/{env}/sa-base.yaml`
   - 資料庫、Redis、郵件、快取設定

2. **應用配置**: `sa-admin/src/main/resources/{env}/application.yaml`
   - 應用專屬設定
   - 覆寫基礎配置

**環境**: dev (預設), test, pre, prod

建置命令: `mvn clean package -P {env}`

### 4.2 前端配置

環境配置檔案:
- `.env.localhost` - 本地開發
- `.env.development` - 開發環境
- `.env.test` - 測試環境
- `.env.pre` - 預發布環境
- `.env.production` - 正式環境

---

## 5. 資料庫標準

### 5.1 表命名
- 統一使用小寫加底線: `t_{模組名}`
- 範例: `t_category`, `t_employee`

### 5.2 欄位規範
所有表必須包含以下審計欄位（由框架自動填充）:
```sql
created_time DATETIME,
created_by BIGINT,
updated_time DATETIME,
updated_by BIGINT,
deleted_flag TINYINT(1) DEFAULT 0
```

### 5.3 主鍵規範
- 使用自增主鍵: `{table_name}_id BIGINT PRIMARY KEY AUTO_INCREMENT`
- Entity 中使用 `@TableId(type = IdType.AUTO)`

**詳細規範**: [rules/05-postgresql-basics.md](../rules/05-postgresql-basics.md)

---

## 6. Git 標準

### 6.1 分支管理
- **main**: 正式分支
- **develop**: 開發分支
- **feature/{功能名}**: 功能分支
- **hotfix/{問題描述}**: 緊急修復分支

### 6.2 提交訊息
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

**詳細規範**: [rules/17-commit-message-conventions.md](../rules/17-commit-message-conventions.md)

---

## 7. 測試標準

### 7.1 後端測試
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

### 7.2 前端測試
- 單元測試: Vitest
- E2E 測試: 按需配置

**詳細規範**: [workflows/tdd-workflow.md](../workflows/tdd-workflow.md)

---

## 8. API 文件標準

### 8.1 Swagger 註解
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

## 9. 安全標準

1. **密碼加密**: 使用 BCrypt
2. **傳輸加密**: 支援國產加密演算法和 AES
3. **SQL 注入**: 使用 MyBatis 參數綁定
4. **XSS 防護**: 前端輸出跳脫
5. **CSRF**: Sa-Token 自動處理

**詳細規範**:
- [rules/07-owasp-top10-part1.md](../rules/07-owasp-top10-part1.md)
- [rules/07-owasp-top10-part2.md](../rules/07-owasp-top10-part2.md)

---

## 10. 效能優化

1. **快取策略**: 使用 Caffeine (本地) + Redis (分散式)
2. **分頁查詢**: 必須使用 PageParam 和 SmartPageUtil
3. **N+1 問題**: 注意 MyBatis 關聯查詢
4. **批次操作**: 使用 MyBatis Plus 的 saveBatch
5. **索引優化**: 高頻查詢欄位新增索引

---

**最後更新**: 2025-01-21
**返回**: [README.md](../README.md)
