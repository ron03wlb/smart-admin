---
name: smartadmin-mybatis
description: MyBatis/MyBatis Plus AI code generator for SmartAdmin framework. Use when creating Dao layer code, optimizing queries, or reviewing MyBatis implementations. Automatically triggers when user mentions creating new Dao/Entity/Mapper or editing Dao layer files.
license: MIT
version: "1.0.0"
---

# SmartAdmin MyBatis Code Generator

AI-powered MyBatis/MyBatis Plus code generator and optimization assistant for SmartAdmin framework. Generates architecture-compliant Dao layer code through conversational interaction.

## When to Use This Skill

### Manual Invocation
```bash
/mybatis generate          # Generate complete Dao layer code
/mybatis query             # Build complex query assistance
/mybatis review            # Review Dao layer code quality
/mybatis best-practices    # View MyBatis best practices
```

### Auto-Trigger Scenarios

**1. New Feature Development**
- User mentions: "创建 xxx 表的 Dao 层" / "需要 CRUD 功能" / "新建 Entity"
- Keywords detected: "生成 Dao", "MyBatis 代码", "数据库表映射"

**2. File Editing**
- Opening or modifying `*Dao.java` files
- Opening or modifying `*Mapper.xml` files
- Creating new files in `dao/` or `domain/entity/` directories

**3. Code Review Integration**
- Running `/review-pr` command with Dao layer changes
- `code-reviewer` agent detects MyBatis code modifications
- Checks for N+1 queries, missing indexes, SQL injection risks

## Core Features

### 1. AI Conversational Code Generation

Generates SmartAdmin-compliant code through guided conversation:

```
User: "我需要创建 t_order 表的 Dao 层"

Skill: "我会帮您生成 SmartAdmin 规范的 MyBatis 代码。
       请提供表结构信息（以下任一格式）：
       1. SQL CREATE TABLE 语句
       2. 表名 + 字段列表（字段名、类型、注释）"

User: [粘贴 CREATE TABLE t_order (...)]

Skill: "✅ 已分析表结构：
       - 表名：t_order
       - 主键：order_id (BIGINT AUTO_INCREMENT)
       - 字段：12 个字段（order_no, user_id, amount, status...）

       将生成以下文件：
       ✓ OrderEntity.java          (domain/entity/)
       ✓ OrderDao.java             (dao/)
       ✓ OrderMapper.xml           (resources/mapper/)
       ✓ OrderQueryForm.java       (domain/form/)

       需要以下额外功能吗？
       [ ] 多表关联查询（JOIN）
       [ ] 软删除支持（deletedFlag）
       [ ] 乐观锁版本控制
       [ ] 复杂统计查询"

User: "需要软删除支持"

Skill: [生成代码，遵循 ArchitectureTest 规则]
       "✅ 代码已生成！

       关键配置：
       1. Entity 添加了 @TableLogic 注解到 deletedFlag
       2. Dao 自定义方法自动过滤已删除数据
       3. QueryForm 包含 deletedFlag 条件（hidden = true）

       建议验证步骤：
       1. 运行 ./gradlew :smartadmin-app:test --tests ArchitectureTest
       2. 检查 Mapper.xml SQL 的 WHERE 条件
       3. 在 Service 层调用 Dao 方法测试"
```

### 2. Generated Code Components

| Component | Generated Content | Key Features |
|-----------|------------------|--------------|
| **Entity** | `@TableName`, field mappings, Lombok, JavaDoc | SmartAdmin naming conventions |
| **Dao** | `@Mapper`, `extends BaseMapper<T>`, custom methods | ArchUnit layer rules compliance |
| **Mapper.xml** | CRUD SQL, dynamic queries, resultMap | Smart strategy: XML vs LambdaQueryWrapper |
| **QueryForm** | `extends PageParam`, query conditions, validation | SmartPageUtil pagination integration |

### 3. Intelligent Query Strategy

Automatically selects the best implementation approach:

```java
// Simple queries → LambdaQueryWrapper (in Service layer)
LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
    .eq(Order::getUserId, userId)
    .ge(Order::getCreateTime, startTime)
    .orderByDesc(Order::getCreateTime);

// Complex queries → XML Mapper
<select id="queryOrderWithUser" resultType="OrderVO">
    SELECT o.*, u.user_name
    FROM t_order o
    LEFT JOIN t_user u ON u.user_id = o.user_id
    <where>
        <if test="queryForm.status != null">
            AND o.status = #{queryForm.status}
        </if>
    </where>
</select>
```

**Decision Matrix:**
- **Use LambdaQueryWrapper**: Single table, ≤ 5 conditions, simple sorting
- **Use XML Mapper**: Multi-table JOIN, complex subqueries, dynamic sorting, > 5 conditions

### 4. Architecture Rules Integration

All generated code enforces SmartAdmin architecture standards:

**ArchitectureTest Compliance:**
- ✅ Dao interfaces use `@Mapper` annotation
- ✅ Dao interfaces extend `BaseMapper<Entity>`
- ✅ No `@Transactional` in Dao (Manager layer only)
- ✅ Constructor injection with `@RequiredArgsConstructor` (if dependencies exist)
- ✅ Package naming: `net.lab1024.sa.{module}.dao`

**.agent/rules/ Coding Standards:**
- ✅ Naming conventions: `*Dao.java`, `*Entity.java`, `*Mapper.xml`
- ✅ Boolean fields: `deletedFlag` NOT `isDeleted`
- ✅ No logging in Dao/Entity layers
- ✅ Exception handling: Use `BusinessException` in Service layer

**SmartAdmin Tools Integration:**
- ✅ Pagination: `SmartPageUtil.convert2PageQuery(form)`
- ✅ Bean conversion: `SmartBeanUtil.copy(source, Target.class)`
- ✅ API responses: `ResponseDTO.ok(data)`

## Generated Code Style

### Entity Template
```java
/**
 * 订单 实体表
 *
 * @author SmartAdmin MyBatis Generator
 * @since 2026-01-23
 */
@Data
@TableName("t_order")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单状态 */
    private Integer status;

    /** 是否删除 */
    @TableLogic
    private Boolean deletedFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

### Dao Interface Template
```java
/**
 * 订单 Dao
 *
 * @author SmartAdmin MyBatis Generator
 * @since 2026-01-23
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    /**
     * 分页查询订单列表
     *
     * @param page 分页参数
     * @param queryForm 查询条件
     * @return 订单列表
     */
    List<OrderVO> queryOrder(Page<?> page, @Param("queryForm") OrderQueryForm queryForm);

    /**
     * 根据订单号查询
     *
     * @param orderNo 订单号
     * @return 订单实体
     */
    OrderEntity getByOrderNo(@Param("orderNo") String orderNo);
}
```

### QueryForm Template
```java
/**
 * 订单查询表单
 *
 * @author SmartAdmin MyBatis Generator
 * @since 2026-01-23
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderQueryForm extends PageParam {

    @Schema(description = "订单编号")
    @Length(max = 32, message = "订单编号最多32字符")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "删除标识", hidden = true)
    private Boolean deletedFlag;
}
```

### Mapper.xml Template
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="net.lab1024.sa.business.order.dao.OrderDao">

    <select id="queryOrder" resultType="net.lab1024.sa.business.order.domain.vo.OrderVO">
        SELECT *
        FROM t_order
        <where>
            <if test="queryForm.orderNo != null and queryForm.orderNo != ''">
                AND order_no = #{queryForm.orderNo}
            </if>
            <if test="queryForm.userId != null">
                AND user_id = #{queryForm.userId}
            </if>
            <if test="queryForm.status != null">
                AND status = #{queryForm.status}
            </if>
            <if test="queryForm.deletedFlag != null">
                AND deleted_flag = #{queryForm.deletedFlag}
            </if>
        </where>
        ORDER BY create_time DESC
    </select>

    <select id="getByOrderNo" resultType="net.lab1024.sa.business.order.domain.entity.OrderEntity">
        SELECT *
        FROM t_order
        WHERE order_no = #{orderNo}
          AND deleted_flag = false
    </select>

</mapper>
```

## Performance Optimization & Anti-Patterns

### Built-in Performance Checks

| Issue | Detection Rule | Fix Recommendation |
|-------|---------------|-------------------|
| **N+1 Queries** | Loop calling single query | Use batch query or JOIN |
| **Full Table Scan** | Missing indexed fields in WHERE | Add index or adjust query |
| **Large Offset Pagination** | OFFSET > 10000 | Use cursor pagination or lastId |
| **SQL Injection Risk** | `${}` with user input | Use `#{}` parameter binding |
| **Over-fetching** | SELECT * from large table | Specify required columns only |

### Performance Examples

```java
// ❌ N+1 Query Anti-Pattern
for (Long deptId : deptIds) {
    employeeDao.selectByDepartmentId(deptId);  // ← N queries
}

// ✅ Optimized Batch Query
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .in(Employee::getDepartmentId, deptIds);  // ← 1 query
List<Employee> employees = employeeDao.selectList(wrapper);

// ❌ Large Offset Pagination
SELECT * FROM t_order LIMIT 100000, 20;  // ← Slow!

// ✅ Cursor Pagination
SELECT * FROM t_order
WHERE order_id > #{lastOrderId}
ORDER BY order_id
LIMIT 20;  // ← Fast!
```

## Conversational Workflow

### Scenario 1: Standard CRUD Generation

```
User: "创建 t_product 表的 Dao 层"
Skill: [Guide table structure input] → [Generate 4 files] → [Run ArchTest] → [✓ Done]
Time: ~2 minutes
```

### Scenario 2: Complex Query Optimization

```
User: "商品查询太慢，需要关联分类和品牌表"
Skill: [Analyze existing code] → [Suggest XML Mapper + JOIN] → [Generate optimized SQL]
Time: ~3 minutes
```

### Scenario 3: Code Review Integration

```
User: "/review-pr 123"
Skill: [Auto-detect Dao changes] → [Check rules] → [Find N+1 query] → [Provide fix]
Time: Seamless (auto-triggered)
```

## Best Practices Reference

See `references/` directory for detailed guides:

- **dao-architecture.md**: Dao layer architecture patterns
- **lambda-wrapper-guide.md**: LambdaQueryWrapper usage guide
- **xml-mapper-patterns.md**: XML Mapper best practices
- **performance-optimization.md**: Performance tuning strategies

## Usage Examples

### Example 1: Generate from CREATE TABLE

```sql
CREATE TABLE t_product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    deleted_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='商品表';
```

**Generated Output:**
- ✓ `ProductEntity.java` with 8 fields, `@TableLogic` on deletedFlag
- ✓ `ProductDao.java` with basic CRUD methods
- ✓ `ProductMapper.xml` with dynamic query SQL
- ✓ `ProductQueryForm.java` with pagination support

### Example 2: Complex Query Builder

```
User: "需要查询：最近30天内，特定分类下，价格在100-500之间，库存>0的商品，按销量降序"

Skill: 分析复杂度 → 推荐 LambdaQueryWrapper

Generated Code:
```java
LambdaQueryWrapper<Product> wrapper = Wrappers.<Product>lambdaQuery()
    .eq(Product::getCategoryId, categoryId)
    .between(Product::getPrice, new BigDecimal("100"), new BigDecimal("500"))
    .gt(Product::getStock, 0)
    .ge(Product::getCreateTime, LocalDateTime.now().minusDays(30))
    .eq(Product::getDeletedFlag, false)
    .orderByDesc(Product::getSalesCount);
List<Product> products = productDao.selectList(wrapper);
```

## Validation & Testing

After code generation, the skill automatically:

1. **ArchUnit Validation**: Runs `ArchitectureTest` to verify layer dependencies
2. **Naming Check**: Validates class/method/field naming conventions
3. **SQL Syntax**: Checks XML Mapper SQL for common errors
4. **Package Structure**: Verifies files are in correct module directories

**Manual Verification (Recommended):**
```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:test --tests ArchitectureTest
./gradlew :smartadmin-app:bootRun  # Test in running application
```

## Troubleshooting

### Common Issues

**Issue**: Generated code fails ArchitectureTest
- **Cause**: Package naming mismatch
- **Fix**: Ensure module path matches `net.lab1024.sa.{module}.dao`

**Issue**: Mapper.xml not found at runtime
- **Cause**: Resource directory not scanned
- **Fix**: Verify `resources/mapper/` path matches Dao namespace

**Issue**: N+1 query detected in review
- **Cause**: Loop calling single-record query
- **Fix**: Use batch query with `IN` clause or JOIN

## Version History

- **1.0.0** (2026-01-23): Initial release
  - AI conversational code generation
  - Entity, Dao, Mapper.xml, QueryForm templates
  - ArchitectureTest integration
  - Performance optimization checks
  - SmartAdmin v4.0.0 foundation package support

## License

MIT License - See [LICENSE.txt](LICENSE.txt) for details.

## Related Documentation

- [SmartAdmin Patterns](../../shared/knowledge/smartadmin-patterns.md)
- [Project Architecture](../../shared/knowledge/project-architecture.md)
- [Quality Standards](../../shared/knowledge/quality-standards.md)
- [Architecture Rules](./../../../.agent/foundation/10-architecture-rules.md)
- [MyBatis Plus Core](./../../../.agent/rules/technology/database/09-mybatis-plus-core.md)