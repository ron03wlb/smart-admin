---
description: Common SmartAdmin development workflows
---

# SmartAdmin Common Workflows

Quick reference for frequent development tasks.

---

## 🏗️ Creating a New Business Module

### 1. Plan Your Module
- Module name (e.g., `category`, `product`, `order`)
- Database table structure
- Required fields and relationships

### 2. Create Database Table
```sql
CREATE TABLE t_{module_name} (
    {module_name}_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- your business fields
    name VARCHAR(100) NOT NULL,
    type INT,
    -- ... more fields
    
    -- Standard audit fields (auto-filled by framework)
    created_time DATETIME,
    created_by BIGINT,
    updated_time DATETIME,
    updated_by BIGINT,
    deleted_flag TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{Module description}';
```

### 3. Create Package Structure
```bash
cd sa-admin/src/main/java/net/lab1024/sa/admin/module/business/
mkdir -p {module}/{constant,controller,dao,domain/{entity,form,vo},service}
```

### 4. Create Domain Objects

**Entity** (domain/entity/{Module}Entity.java):
```java
@Data
@TableName("t_{module}")
public class {Module}Entity {
    @TableId(type = IdType.AUTO)
    private Long {module}Id;
    
    private String name;
    private Integer type;
    
    // Auto-filled fields
    private LocalDateTime createdTime;
    private Long createdBy;
    private LocalDateTime updatedTime;
    private Long updatedBy;
    private Boolean deletedFlag;
}
```

**Forms** (domain/form/):
- `{Module}AddForm.java`
- `{Module}UpdateForm.java`
- `{Module}QueryForm.java` (extends PageParam)

**VO** (domain/vo/{Module}VO.java):
```java
@Data
public class {Module}VO {
    private Long {module}Id;
    private String name;
    private Integer type;
    private LocalDateTime createdTime;
}
```

### 5. Create Dao
```java
@Mapper
public interface {Module}Dao extends BaseMapper<{Module}Entity> {
    // MyBatis Plus provides basic CRUD
    // Add custom queries here if needed
}
```

### 6. Create Service
```java
@Service
public class {Module}Service {
    @Resource
    private {Module}Dao dao;
    
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> add({Module}AddForm form) {
        {Module}Entity entity = SmartBeanUtil.copy(form, {Module}Entity.class);
        dao.insert(entity);
        return ResponseDTO.ok();
    }
    
    public ResponseDTO<PageResult<{Module}VO>> query({Module}QueryForm form) {
        Page<{Module}Entity> page = SmartPageUtil.convert2PageQuery(form);
        LambdaQueryWrapper<{Module}Entity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq({Module}Entity::getDeletedFlag, false);
        // Add your query conditions
        page = dao.selectPage(page, wrapper);
        PageResult<{Module}VO> result = SmartPageUtil.convert2PageResult(page, {Module}VO.class);
        return ResponseDTO.ok(result);
    }
}
```

### 7. Create Controller
```java
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_{MODULE})
@RestController
@RequestMapping("/{module}")
public class {Module}Controller {
    @Resource
    private {Module}Service service;
    
    @Operation(summary = "添加")
    @PostMapping("/add")
    @SaCheckPermission("{module}:add")
    public ResponseDTO<String> add(@RequestBody @Valid {Module}AddForm form) {
        return service.add(form);
    }
    
    @Operation(summary = "分页查询")
    @PostMapping("/query")
    @SaCheckPermission("{module}:query")
    public ResponseDTO<PageResult<{Module}VO>> query(@RequestBody @Valid {Module}QueryForm form) {
        return service.query(form);
    }
}
```

### 8. Add Permissions
Add to database `t_menu` table or configure via admin interface:
- `{module}:add`
- `{module}:update`
- `{module}:delete`
- `{module}:query`

---

## 🧪 Testing Your Module

### Write Unit Tests
Create `{Module}ServiceTest.java` extending `AdminApplicationTest`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class {Module}ServiceTest extends AdminApplicationTest {
    @Resource
    private {Module}Service service;
    
    @Test
    public void testAdd() {
        {Module}AddForm form = new {Module}AddForm();
        form.setName("Test Name");
        ResponseDTO<String> result = service.add(form);
        Assertions.assertTrue(result.getOk());
    }
}
```

// turbo
### Run Tests
```bash
mvn test -Dtest={Module}ServiceTest
```

---

## 📦 Using Support Modules

### Dictionary Management
```java
@Resource
private DictService dictService;

// Get dict values
List<DictVO> dictList = dictService.queryByType("CATEGORY_TYPE");
```

### File Upload
```java
@Resource
private FileService fileService;

// Upload file
String fileKey = fileService.saveFile(multipartFile);

// Get file URL
String url = fileService.getFileUrl(fileKey);
```

### Operation Logging
```java
@OperateLog(description = "删除类目")
public ResponseDTO<String> delete(Long id) {
    // Automatically logged by OperateLogAspect
}
```

### Data Change Tracking
```java
// Add to entity fields
@DataTracerFieldLabel("类目名称")
private String name;

// Changes are tracked automatically
```

### Caching
```java
@Resource
private CacheService cacheService;

// Set cache
cacheService.set("key", value, 3600); // seconds

// Get cache
Object value = cacheService.get("key");

// Remove cache
cacheService.remove("key");
```

---

## 🔧 Database Operations

### Running Migrations
```bash
# Import SQL file
mysql -u root -p smart_admin_v3 < path/to/migration.sql
```

### Checking P6Spy Logs
P6Spy logs all SQL queries to console in dev environment.

Look for output like:
```
Consume Time: 5 ms | Execute SQL: SELECT * FROM t_category WHERE deleted_flag = 0
```

---

## 🚀 Build and Deploy

### Development Build
```bash
mvn clean package -P dev -DskipTests
```

### Production Build
```bash
mvn clean package -P prod
```

### Run JAR
```bash
java -jar sa-admin/target/sa-admin-prod-3.0.0.jar
```

### Docker (if configured)
```bash
cd docker
docker-compose up -d
```

---

## 🐛 Debugging

### Enable Debug Logging
In `application.yaml`:
```yaml
logging:
  level:
    net.lab1024.sa: DEBUG
```

### View SQL Logs
P6Spy automatically logs SQL in dev environment. Check console output.

### Swagger API Testing
- Navigate to: http://localhost:1024/swagger-ui.html
- Use "Authorize" button to set authentication token
- Test APIs directly in browser

---

## 📝 Code Generation

SmartAdmin includes a code generator module:

1. Access: `/support/codegenerator`
2. Configure table and fields
3. Generate: Entity, Form, VO, Dao, Service, Controller
4. Download and integrate into project

---

## 🔄 Hot Reload Configuration

Use `@ReloadData` for runtime config updates:

```java
@ReloadData(value = "my-config")
public void reloadConfig() {
    // This method will be called when admin triggers reload
    // Re-initialize your configuration here
}
```

Trigger via: `/support/reload/reload?tag=my-config`

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:1024/actuator/health
```

### Application Logs
Located in: `logs/` directory
- `app.log` - Application logs
- `error.log` - Error logs

---

## 🎯 Best Practices Checklist

When creating a new module, ensure:

- [ ] Database table has audit fields (created_time, created_by, etc.)
- [ ] Entity has `@TableName` and `@TableId` annotations
- [ ] Forms have `@Valid` annotations
- [ ] Service methods have `@Transactional` for writes
- [ ] Controller methods have `@SaCheckPermission`
- [ ] All methods return `ResponseDTO<T>`
- [ ] Use `SmartBeanUtil` for object conversion
- [ ] QueryForm extends `PageParam`
- [ ] Use `SmartPageUtil` for pagination
- [ ] Error handling uses `BusinessException` with error codes
- [ ] APIs documented with `@Operation` annotations
- [ ] Tests written for critical business logic

---

## 🔗 Related Workflows

- `/init` - Initialize development environment
- See `.agent/workflows/onboarding.md` - Developer onboarding guide

---

**Need more help?** Check `CLAUDE.md` for comprehensive coding standards and patterns!
