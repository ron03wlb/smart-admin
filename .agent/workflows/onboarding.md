# SmartAdmin Developer Onboarding Guide

Welcome to **SmartAdmin**! This guide will help you get started with development on this enterprise-grade platform.

## 🚀 Quick Start

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd smart-admin/smart-admin-api-java17-springboot3
```

### 2. Prerequisites
- **Java**: 17+ (you have Java 21 ✅)
- **Maven**: 3.6+ (you have Maven 3.9.11 ✅)
- **MySQL**: 5.7+ or 8.0+
- **Redis**: Optional but recommended

### 3. Database Setup

Run the SQL scripts to create your database:

```bash
# From project root
cd ..
mysql -u root -p < 数据库SQL脚本/mysql/smart-admin-vX.sql
```

Update database credentials in `sa-base/src/main/resources/dev/sa-base.yaml`

### 4. Build and Run

```bash
cd smart-admin-api-java17-springboot3
mvn clean package -DskipTests
cd sa-admin
mvn spring-boot:run
```

Access the application:
- **API**: http://localhost:1024
- **Swagger**: http://localhost:1024/swagger-ui.html

---

## 📁 Project Structure

```
smart-admin-api-java17-springboot3/
├── sa-base/                      # Infrastructure Library (357 files)
│   ├── common/                   # Core framework code
│   │   ├── domain/              # Base DTOs (ResponseDTO, PageParam, etc.)
│   │   ├── util/                # Utilities (SmartBeanUtil, SmartPageUtil, etc.)
│   │   ├── constant/            # System constants
│   │   └── config/              # 26+ Spring configurations
│   └── module/support/          # 26 Support Modules
│       ├── dict/                # Dictionary management
│       ├── file/                # File upload/download
│       ├── operatelog/          # Operation logging
│       ├── loginlog/            # Login audit
│       ├── datatracer/          # Data change tracking
│       └── ...                  # See full list in CLAUDE.md
│
└── sa-admin/                     # Main Application (202 files)
    ├── module/
    │   ├── business/            # Business modules (your code goes here)
    │   └── system/              # System modules (employees, roles, etc.)
    └── config/                  # App-specific configurations
```

---

## 🏗️ Architecture Patterns

### Layered Architecture (MANDATORY)
Every business module follows this strict pattern:

```
Controller → Service → Dao → Database
     ↓          ↓        ↓
   @Valid   @Transaction  BaseMapper
```

**❌ NEVER** call Dao directly from Controller!

### Domain Object Hierarchy

Each business module uses 4 distinct types:

```java
// 1. Entity - Database mapping
@TableName("t_category")
public class CategoryEntity {
    @TableId(type = IdType.AUTO)
    private Long categoryId;
    // ... auto-filled audit fields
}

// 2. Form - Request DTOs
public class CategoryAddForm {
    @NotBlank(message = "名称不能为空")
    private String categoryName;
}

// 3. VO - Response DTOs
public class CategoryVO {
    private Long categoryId;
    private String categoryName;
}

// 4. DTO - Internal transfers (optional)
```

---

## 🔐 Key Conventions

### 1. Authentication & Permissions

```java
// Require permission
@SaCheckPermission("module:action")
public ResponseDTO<T> method() { ... }

// Public endpoint
@NoNeedLogin
public ResponseDTO<T> login() { ... }

// Get current user
RequestUser user = AdminRequestUtil.getRequestUser();
```

### 2. Response Pattern

All methods return `ResponseDTO<T>`:

```java
return ResponseDTO.ok(data);                    // Success
return ResponseDTO.error(UserErrorCode.XXX);    // Error
throw new BusinessException(ErrorCode.XXX);     // Exception
```

### 3. Validation

```java
// In Form classes
@NotBlank(message = "不能为空")
@Length(max = 50, message = "最多50字符")
private String name;

// In Controller
public ResponseDTO<T> add(@RequestBody @Valid Form form) { ... }
```

### 4. Pagination

```java
// QueryForm extends PageParam
Page<Entity> page = SmartPageUtil.convert2PageQuery(queryForm);
page = dao.selectPage(page, wrapper);
PageResult<VO> result = SmartPageUtil.convert2PageResult(page, VO.class);
```

### 5. Bean Conversion

```java
Entity entity = SmartBeanUtil.copy(form, Entity.class);
List<VO> voList = SmartBeanUtil.copyList(entities, VO.class);
```

---

## 📦 Essential Support Modules

SmartAdmin provides 26 reusable support modules in `sa-base/module/support/`:

| Module            | Purpose                    | Key Features                     |
| ----------------- | -------------------------- | -------------------------------- |
| **dict**          | Dictionary/enum management | UI-configurable key-value pairs  |
| **file**          | File upload/download       | Local & S3 storage               |
| **operatelog**    | Operation audit trail      | Auto-logging with `@OperateLog`  |
| **loginlog**      | Login/logout tracking      | IP, device info, failed attempts |
| **datatracer**    | Data change tracking       | Git-diff style change records    |
| **config**        | Runtime configuration      | Key-value config without restart |
| **reload**        | Hot reload configs         | `@ReloadData` annotation         |
| **captcha**       | Verification codes         | Numeric/letter/mixed modes       |
| **codegenerator** | Code scaffolding           | Generates full CRUD modules      |
| **cache**         | Multi-level caching        | Caffeine + Redis                 |

See `CLAUDE.md` for complete list and usage.

---

## 🛠️ Common Development Tasks

### Adding a New Business Module

1. **Create package structure:**
   ```
   sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{module}/
   ├── constant/
   ├── controller/
   ├── dao/
   ├── domain/
   │   ├── entity/
   │   ├── form/
   │   └── vo/
   ├── manager/      (optional)
   └── service/
   ```

2. **Create database table:**
   ```sql
   CREATE TABLE t_{module} (
       {module}_id BIGINT PRIMARY KEY AUTO_INCREMENT,
       -- your fields
       created_time DATETIME,
       created_by BIGINT,
       updated_time DATETIME,
       updated_by BIGINT,
       deleted_flag TINYINT(1) DEFAULT 0
   );
   ```

3. **Generate code** (optional):
   - Use the built-in code generator module
   - Or follow the patterns in existing modules

See full guide in `CLAUDE.md` → "Adding a New Business Module"

### Running Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=CategoryServiceTest

# Single test method
mvn test -Dtest=CategoryServiceTest#testAdd
```

Test classes extend `AdminApplicationTest` base class.

---

## 🔧 Configuration

### Multi-Environment Setup

SmartAdmin uses a two-layer configuration system:

1. **Base Config**: `sa-base/src/main/resources/{env}/sa-base.yaml`
   - Database, Redis, mail, cache settings
   
2. **App Config**: `sa-admin/src/main/resources/{env}/application.yaml`
   - Application-specific settings
   - Overrides base config

**Environments**: dev (default), test, pre, prod

Build with specific profile:
```bash
mvn clean package -P test
```

### Key Configuration Files

- **Database**: `sa-base/dev/sa-base.yaml` → `spring.datasource`
- **Redis**: Same file → `spring.data.redis`
- **Server Port**: `sa-admin/dev/application.yaml` → `server.port` (default: 1024)

---

## 📚 Essential Documentation

- **Official Site**: https://smartadmin.vip
- **Online Preview**: https://preview.smartadmin.vip
- **CLAUDE.md**: Comprehensive coding guide (READ THIS FIRST!)
- **API Docs**: http://localhost:1024/swagger-ui.html (when running)

---

## 🐛 Troubleshooting

| Issue                      | Solution                                                |
| -------------------------- | ------------------------------------------------------- |
| Port 1024 in use           | Change `server.port` in `application.yaml`              |
| Database connection failed | Verify MySQL running and credentials in `sa-base.yaml`  |
| Redis connection failed    | Redis is optional; disable or start with `redis-server` |
| Build fails                | Run `mvn clean` and ensure Java 17+                     |

---

## 💡 Pro Tips

1. **Read CLAUDE.md thoroughly** - It contains battle-tested patterns from 1000+ companies
2. **Use support modules** - Don't reinvent the wheel (logging, caching, file upload, etc.)
3. **Follow naming conventions** - Permission codes: `{module}:{action}`
4. **Always use `@Transactional`** for write operations
5. **Never skip Service layer** - Even for simple CRUD
6. **Leverage SmartBeanUtil** - For all object conversions
7. **Check Swagger docs** - Auto-generated and always up-to-date

---

## 🎯 Next Steps

1. ✅ Complete database setup
2. ✅ Start the application
3. 📖 Read `CLAUDE.md` coding conventions
4. 🔍 Explore existing business modules for patterns
5. 🚀 Create your first module!

**Need help?** Check the workflows in `.agent/workflows/` for common tasks.

---

**Happy Coding! 🎉**

Remember: *"We advocate for high-quality code. As developers, code is our sword. A flourish of keystrokes like a swordsman, mission accomplished, we fade into the background."*
