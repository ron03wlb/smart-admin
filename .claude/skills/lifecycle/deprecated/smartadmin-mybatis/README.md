# SmartAdmin MyBatis Skill

AI-powered MyBatis/MyBatis Plus code generator for SmartAdmin framework.

## Quick Start

### Manual Invocation

```bash
# Generate complete Dao layer code
/mybatis generate

# Build complex query assistance
/mybatis query

# Review Dao layer code quality
/mybatis review

# View MyBatis best practices
/mybatis best-practices
```

### Auto-Trigger

The skill automatically activates when:
- You mention "创建 xxx 表的 Dao 层" or "生成 MyBatis 代码"
- You edit `*Dao.java` or `*Mapper.xml` files
- You run code review on Dao layer changes

## Generated Components

| Component | Description |
|-----------|-------------|
| **Entity** | `@TableName`, field mappings, Lombok annotations, JavaDoc |
| **Dao** | `@Mapper`, extends `BaseMapper<T>`, custom query methods |
| **Mapper.xml** | Dynamic SQL, pagination, resultMap configuration |
| **QueryForm** | `extends PageParam`, query conditions, validation |

## Features

✅ SmartAdmin architecture compliance (ArchitectureTest validated)
✅ AI conversational code generation
✅ Intelligent query strategy (LambdaQueryWrapper vs XML)
✅ Performance optimization checks (N+1, indexes, pagination)
✅ Code review integration

## Documentation

- **[SKILL.md](SKILL.md)** - Complete skill documentation
- **[references/](references/)** - MyBatis best practices guides
  - [Dao Architecture](references/dao-architecture.md)
  - [LambdaQueryWrapper Guide](references/lambda-wrapper-guide.md)
  - [XML Mapper Patterns](references/xml-mapper-patterns.md)
  - [Performance Optimization](references/performance-optimization.md)
- **[templates/](templates/)** - Code generation templates

## Example Usage

### Generate Dao Layer from CREATE TABLE

```sql
CREATE TABLE t_product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    deleted_flag TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Invoke:** `/mybatis generate`

**Output:**
- ✓ `ProductEntity.java` (8 fields, @TableLogic on deletedFlag)
- ✓ `ProductDao.java` (basic CRUD + custom methods)
- ✓ `ProductMapper.xml` (dynamic query SQL)
- ✓ `ProductQueryForm.java` (pagination support)

## Architecture Compliance

All generated code follows SmartAdmin standards:

- ✅ `@Mapper` annotation on Dao interfaces
- ✅ Extends `BaseMapper<Entity>`
- ✅ Constructor injection (`@RequiredArgsConstructor`)
- ✅ Package naming: `net.lab1024.sa.{module}.dao`
- ✅ Boolean fields: `xxxFlag` (not `isXxx`)
- ✅ No transactions in Dao (Manager layer only)

## Validation

After generation, run:

```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:test --tests ArchitectureTest
```

## Version

- **Version:** 1.0.0
- **License:** MIT
- **Updated:** 2026-01-23

## Related Documentation

- [SmartAdmin Patterns](../../shared/knowledge/smartadmin-patterns.md)
- [Project Architecture](../../shared/knowledge/project-architecture.md)
- [Architecture Rules](../../../.agent/foundation/10-architecture-rules.md)
- [MyBatis Plus Core](../../../.agent/rules/technology/database/09-mybatis-plus-core.md)
