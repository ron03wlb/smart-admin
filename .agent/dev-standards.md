# SmartAdmin 开发规范

## 主开发目录

**后续主要开发目录：**
- **后端**: `smart-admin-api-java21-springboot3/` - Java 21 + Spring Boot 3 版本
- **前端**: `smart-admin-web/` - TypeScript + Vue 3 版本

> **说明**: 这些是从 `smart-admin-api-java17-springboot3` 和 `smart-admin-web-typescript` 复制而来，作为项目的主要开发分支。

---

## 技术栈

### 后端 (smart-admin-api-java21-springboot3)
- **Java**: 21
- **Spring Boot**: 3.5.4
- **Sa-Token**: 1.44.0 (认证授权)
- **MyBatis Plus**: 3.5.12 (ORM)
- **PostgreSQL**: 42.7.5 驱动 (主数据库)
- **Vavr**: 0.10.4 (函数式编程)
- **Knife4j**: 4.6.0 (API 文档)
- **Druid**: 1.2.25 (数据库连接池)
- **Redis**: Redisson 3.50.0 (缓存和分布式)
- **P6Spy**: 3.9.1 (SQL 监控)

### 前端 (smart-admin-web)
- **Vue**: 3.4.27
- **TypeScript**: 5.6.3
- **Vite**: 5.2.12
- **Ant Design Vue**: 4.2.5
- **Pinia**: 2.1.7 (状态管理)
- **Vue Router**: 4.3.2
- **Node**: >= 18

---

## 项目结构

### 后端结构
```
smart-admin-api-java21-springboot3/
├── sa-base/                          # 基础设施库 (357 个 Java 文件)
│   ├── common/                       # 核心框架代码
│   │   ├── domain/                  # 基础 DTO (ResponseDTO, PageParam 等)
│   │   ├── util/                    # 工具类 (SmartBeanUtil, SmartPageUtil 等)
│   │   ├── constant/                # 系统常量
│   │   └── config/                  # 26+ Spring 配置类
│   └── module/support/              # 26 个支持模块
│       ├── dict/                    # 字典管理
│       ├── file/                    # 文件上传下载
│       ├── operatelog/              # 操作日志
│       ├── loginlog/                # 登录日志
│       ├── datatracer/              # 数据变更追踪
│       ├── config/                  # 系统配置
│       ├── reload/                  # 热重载
│       ├── cache/                   # 缓存管理
│       ├── captcha/                 # 验证码
│       ├── codegenerator/           # 代码生成器
│       └── ...                      # 更多模块
│
└── sa-admin/                         # 主应用程序 (202 个 Java 文件)
    ├── module/
    │   ├── business/                # 业务模块（你的代码放这里）
    │   └── system/                  # 系统模块（员工、角色等）
    └── config/                      # 应用专属配置
```

### 前端结构
```
smart-admin-web/
├── src/
│   ├── api/                         # API 接口定义
│   ├── assets/                      # 静态资源
│   ├── components/                  # 公共组件
│   ├── constants/                   # 常量和枚举
│   ├── layout/                      # 布局组件
│   ├── lib/                         # 第三方库封装
│   ├── router/                      # 路由配置
│   ├── store/                       # Pinia 状态管理
│   ├── utils/                       # 工具函数
│   └── views/                       # 页面视图
├── public/                          # 公共资源
└── package.json                     # 项目配置
```

---

## 开发规范

### 后端规范

#### 1. 分层架构（强制）
所有业务模块必须遵循严格的分层架构：

```
Controller → Service → Dao → Database
     ↓          ↓        ↓
   @Valid   @Transaction  BaseMapper
```

**❌ 禁止** Controller 直接调用 Dao！

#### 2. 领域对象层次

每个业务模块使用 4 种不同的领域对象：

```java
// 1. Entity - 数据库映射
@TableName("t_category")
public class CategoryEntity {
    @TableId(type = IdType.AUTO)
    private Long categoryId;
    // ... 自动填充的审计字段
}

// 2. Form - 请求 DTO
public class CategoryAddForm {
    @NotBlank(message = "名称不能为空")
    private String categoryName;
}

// 3. VO - 响应 DTO
public class CategoryVO {
    private Long categoryId;
    private String categoryName;
}

// 4. DTO - 内部传输（可选）
```

#### 3. 响应模式

所有 Controller 方法必须返回 `ResponseDTO<T>`:

```java
return ResponseDTO.ok(data);                    // 成功
return ResponseDTO.error(UserErrorCode.XXX);    // 错误
throw new BusinessException(ErrorCode.XXX);     // 异常
```

#### 4. 认证与权限

```java
// 需要权限检查
@SaCheckPermission("module:action")
public ResponseDTO<T> method() { ... }

// 公开端点
@NoNeedLogin
public ResponseDTO<T> login() { ... }

// 获取当前用户
RequestUser user = AdminRequestUtil.getRequestUser();
```

#### 5. 数据验证

```java
// 在 Form 类中
@NotBlank(message = "不能为空")
@Length(max = 50, message = "最多50字符")
private String name;

// 在 Controller 中
public ResponseDTO<T> add(@RequestBody @Valid Form form) { ... }
```

#### 6. 分页查询

```java
// QueryForm 必须继承 PageParam
Page<Entity> page = SmartPageUtil.convert2PageQuery(queryForm);
page = dao.selectPage(page, wrapper);
PageResult<VO> result = SmartPageUtil.convert2PageResult(page, VO.class);
```

#### 7. Bean 转换

```java
Entity entity = SmartBeanUtil.copy(form, Entity.class);
List<VO> voList = SmartBeanUtil.copyList(entities, VO.class);
```

#### 8. 事务管理

```java
@Service
public class CategoryService {
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> add(CategoryAddForm form) {
        // 写操作必须加事务
    }
}
```

#### 9. 函数式编程 (Vavr)

使用 Vavr 提升代码健壮性和可维护性：

```java
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;

@Service
public class UserService {

    // Option 替代 null 检查
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

    // Either 业务逻辑分支
    public Either<String, Order> validateAndCreateOrder(OrderDTO dto) {
        return validateStock(dto.getItems())
            .flatMap(items -> validatePayment(dto.getPayment()))
            .map(payment -> createOrder(dto));
    }
}

// Controller 层使用
@RestController
public class UserController {

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .fold(
                () -> ResponseDTO.error("用户不存在"),
                user -> ResponseDTO.ok(user)
            );
    }
}
```

#### 10. PostgreSQL 特性

充分利用 PostgreSQL 高级功能：

**JSONB 字段**:
```java
// Entity
@Data
@TableName("t_order")
public class Order {
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;  // JSONB 映射
}

// Mapper 查询
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

**数组类型**:
```java
// Entity
@Data
@TableName("t_article")
public class Article {
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] tags;  // PostgreSQL 数组
}

// Mapper 查询
default List<Article> findByTag(String tag) {
    return selectList(
        new LambdaQueryWrapper<Article>()
            .apply("tags && ARRAY[{0}]::TEXT[]", tag)
    );
}
```

**数据库配置**:
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

---

### 前端规范

#### 1. 目录结构规范

- **api/**: API 接口定义，按业务模块分文件
- **components/**: 可复用组件，使用 PascalCase 命名
- **constants/**: 常量和枚举，避免魔法数字
- **views/**: 页面组件，按功能模块组织

#### 2. 命名规范

- **组件文件**: PascalCase (如: `UserList.vue`)
- **工具文件**: camelCase (如: `formatDate.ts`)
- **常量文件**: UPPER_SNAKE_CASE (如: `API_ROUTES.ts`)

#### 3. API 调用规范

```typescript
// api/category.ts
import { request } from '@/utils/request';

export const categoryApi = {
  add: (data: CategoryAddForm) => request.post('/category/add', data),
  query: (params: CategoryQueryForm) => request.post('/category/query', params),
};
```

#### 4. 状态管理

使用 Pinia，按功能模块创建 store:

```typescript
// store/category.ts
import { defineStore } from 'pinia';

export const useCategoryStore = defineStore('category', {
  state: () => ({ ... }),
  actions: { ... },
});
```

#### 5. 类型定义

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

## 编码规范要点

### 通用规范

1. **代码格式化**: 
   - 后端: 使用 IDEA 默认格式化
   - 前端: 使用 Prettier (已配置)

2. **注释规范**:
   - 类和方法必须有 Javadoc/JSDoc 注释
   - 复杂逻辑必须添加行内注释

3. **命名规范**:
   - 见名知意，避免缩写
   - 布尔值以 is/has/can 开头
   - 集合以复数形式命名

### Java 规范

1. **包命名**: `net.lab1024.sa.admin.module.{business|system}.{模块名}`
2. **类命名**: 
   - Controller: `{Module}Controller`
   - Service: `{Module}Service`
   - Dao: `{Module}Dao`
   - Entity: `{Module}Entity`

3. **常量定义**: 使用枚举或常量类，避免魔法数字

### TypeScript 规范

1. **严格类型**: 启用 TypeScript 严格模式
2. **接口优先**: 优先使用 interface 而非 type
3. **避免 any**: 必须明确类型，避免使用 any

---

## 配置管理

### 后端配置

采用两层配置系统：

1. **基础配置**: `sa-base/src/main/resources/{env}/sa-base.yaml`
   - 数据库、Redis、邮件、缓存设置

2. **应用配置**: `sa-admin/src/main/resources/{env}/application.yaml`
   - 应用专属设置
   - 覆盖基础配置

**环境**: dev (默认), test, pre, prod

构建命令: `mvn clean package -P {env}`

### 前端配置

环境配置文件:
- `.env.localhost` - 本地开发
- `.env.development` - 开发环境
- `.env.test` - 测试环境
- `.env.pre` - 预发布环境
- `.env.production` - 生产环境

---

## 常用命令

### 后端
```bash
# 进入 Java 21 项目目录
cd smart-admin-api-java21-springboot3

# 构建项目
mvn clean package -P dev

# 运行应用
cd sa-admin && mvn spring-boot:run

# 运行测试
mvn test

# 访问 API 文档
# http://localhost:1024/swagger-ui.html
```

### 前端
```bash
# 进入前端项目目录
cd smart-admin-web

# 安装依赖
npm install

# 本地开发
npm run localhost

# 开发环境
npm run dev

# 构建测试环境
npm run build:test

# 构建生产环境
npm run build:prod
```

---

## 数据库规范

### 表命名
- 统一使用小写加下划线: `t_{模块名}`
- 示例: `t_category`, `t_employee`

### 字段规范
所有表必须包含以下审计字段（由框架自动填充）:
```sql
created_time DATETIME,
created_by BIGINT,
updated_time DATETIME,
updated_by BIGINT,
deleted_flag TINYINT(1) DEFAULT 0
```

### 主键规范
- 使用自增主键: `{table_name}_id BIGINT PRIMARY KEY AUTO_INCREMENT`
- Entity 中使用 `@TableId(type = IdType.AUTO)`

---

## Git 规范

### 分支管理
- **main**: 生产分支
- **develop**: 开发分支
- **feature/{功能名}**: 功能分支
- **hotfix/{问题描述}**: 紧急修复分支

### 提交信息
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型:**
- feat: 新功能
- fix: 修复
- docs: 文档
- style: 格式
- refactor: 重构
- test: 测试
- chore: 构建/工具

**示例:**
```
feat(category): 添加类目管理功能

- 实现类目增删改查
- 添加权限控制
- 完成单元测试

Closes #123
```

---

## 测试规范

### 后端测试
```java
@SpringBootTest
public class CategoryServiceTest extends AdminApplicationTest {
    @Resource
    private CategoryService service;
    
    @Test
    public void testAdd() {
        CategoryAddForm form = new CategoryAddForm();
        form.setName("测试类目");
        ResponseDTO<String> result = service.add(form);
        Assertions.assertTrue(result.getOk());
    }
}
```

### 前端测试
- 单元测试: Vitest
- E2E 测试: 按需配置

---

## API 文档规范

### Swagger 注解
```java
@Tag(name = "类目管理")
@RestController
public class CategoryController {

    @Operation(summary = "添加类目")
    @PostMapping("/category/add")
    @SaCheckPermission("category:add")
    public ResponseDTO<String> add(@RequestBody @Valid CategoryAddForm form) {
        return service.add(form);
    }
}
```

---

## 安全规范

1. **密码加密**: 使用 BCrypt
2. **传输加密**: 支持国产加密算法和 AES
3. **SQL 注入**: 使用 MyBatis 参数绑定
4. **XSS 防护**: 前端输出转义
5. **CSRF**: Sa-Token 自动处理

---

## 性能优化建议

1. **缓存策略**: 使用 Caffeine (本地) + Redis (分布式)
2. **分页查询**: 必须使用 PageParam 和 SmartPageUtil
3. **N+1 问题**: 注意 MyBatis 关联查询
4. **批量操作**: 使用 MyBatis Plus 的 saveBatch
5. **索引优化**: 高频查询字段添加索引

---

## 相关文档

- **官方文档**: https://smartadmin.vip
- **在线预览**: https://preview.smartadmin.vip
- **初始化指南**: `.agent/workflows/init.md`
- **开发者入门**: `.agent/workflows/onboarding.md`
- **常用任务**: `.agent/workflows/common-tasks.md`
- **详细规范**: `CLAUDE.md` (包含完整的编码规范和模式)

---

**记住**: 我们推崇高质量的代码。身为开发，代码即利剑！ ⚔️
