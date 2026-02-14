# Test Fixture Generator - Quick Reference Card

**For**: Developers writing SmartAdmin integration tests
**Pattern**: EmployeeTestFixture.java
**Status**: Production-Ready

---

## The Problem

**Before** (Verbose, Error-Prone):
```java
@Test
void testAddGoods() {
    // 10+ lines of boilerplate per test
    GoodsEntity goods = new GoodsEntity();
    goods.setGoodsName("Test");               // ❌ Not unique
    goods.setGoodsStatus(2);
    goods.setPrice(new BigDecimal(99.99));    // ❌ Precision loss
    goods.setPlace("China");
    goods.setShelvesFlag(true);
    goods.setDeletedFlag(false);
    goods.setCategoryId(1L);
    goods.setRemark("Test");

    goodsDao.insert(goods);
    // ... test logic
}
```

**After** (One-Liner, Safe):
```java
@Test
void testAddGoods() {
    GoodsEntity goods = GoodsTestFixture.createGoods(categoryId);
    goodsDao.insert(goods);
    // ... test logic
}
```

---

## The Pattern (30-Second Overview)

**File**: `{module}/service/{Entity}TestFixture.java`

**Structure**:
```java
public class GoodsTestFixture {
    private static final AtomicInteger counter = new AtomicInteger(0);

    public static GoodsEntity createGoods() { /* defaults */ }
    public static GoodsEntity createGoods(Long categoryId) { /* override */ }
    public static GoodsAddForm createAddForm(Long categoryId) { /* form */ }
    public static CategoryEntity createCategory(String name) { /* FK */ }
    public static void resetCounter() { /* isolation */ }
}
```

**Key Elements**:
- ✅ **AtomicInteger counter** → Uniqueness
- ✅ **Static factories** → No constructors
- ✅ **Parameter overrides** → NOT @Builder
- ✅ **Related entity helpers** → FK setup
- ✅ **resetCounter()** → Test isolation

---

## Cheat Sheet: Default Value Strategies

| Field Type | Pattern | Example |
|-----------|---------|---------|
| **String** | `"{name}-" + id` | `"商品-1"`, `"商品-2"` |
| **String (unique)** | `"{name}_" + timestamp` | `"login_1737799200000"` |
| **Integer/Long** | `counter.incrementAndGet()` | `1`, `2`, `3` |
| **BigDecimal** | `new BigDecimal("99.99")` | ⚠️ Use String constructor |
| **Boolean** | `false` for flags | `deletedFlag = false` |
| **Enum (Integer)** | Most common value | `goodsStatus = 2` (售卖中) |
| **FK (Long)** | `1L` (override via param) | `categoryId = 1L` |
| **LocalDateTime** | Let DB handle | ❌ Don't set manually |

---

## Quick Start: Generate Fixture in 3 Steps

### Step 1: Analyze Entity
```java
@Data
@TableName("t_goods")
public class GoodsEntity {
    @TableId(type = IdType.AUTO) private Long goodsId;      // PK
    private Integer goodsStatus;                             // Enum
    private Long categoryId;                                 // FK
    private String goodsName;                                // String
    private BigDecimal price;                                // Decimal
    private Boolean deletedFlag;                             // Boolean
}
```

### Step 2: Create Fixture Class
**Location**: `business/goods/service/GoodsTestFixture.java`

**Copy from**: `EmployeeTestFixture.java` (replace fields)

### Step 3: Use in Test
```java
@SpringBootTest
@Transactional
class GoodsServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired private GoodsService goodsService;
    @Autowired private CategoryDao categoryDao;

    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        // Create FK dependency
        CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
        categoryDao.insert(category);
        testCategoryId = category.getCategoryId();
    }

    @Test
    void testAddGoods() {
        // One-liner!
        GoodsAddForm form = GoodsTestFixture.createAddForm(testCategoryId);

        ResponseDTO<String> response = goodsService.addGoods(form);

        assertTrue(response.getOk());
    }
}
```

---

## Common Mistakes (DON'T DO THIS)

### ❌ Mistake 1: Using @Builder
```java
// WRONG
@Builder
public static GoodsEntity createGoods() { ... }
```
**Fix**: Use parameter overrides
```java
// CORRECT
public static GoodsEntity createGoods(Long categoryId) {
    GoodsEntity entity = createGoods();
    entity.setCategoryId(categoryId);
    return entity;
}
```

### ❌ Mistake 2: Wrong BigDecimal
```java
// WRONG - Precision loss
entity.setPrice(new BigDecimal(99.99));
```
**Fix**: Use String constructor
```java
// CORRECT
entity.setPrice(new BigDecimal("99.99"));
```

### ❌ Mistake 3: Non-Unique Values
```java
// WRONG - Will fail on second test
entity.setGoodsName("Test Goods");
```
**Fix**: Use counter
```java
// CORRECT
int id = counter.incrementAndGet();
entity.setGoodsName("商品-" + id);
```

### ❌ Mistake 4: Persisting in Fixture
```java
// WRONG - Fixture shouldn't handle persistence
public static GoodsEntity createGoods(GoodsDao dao) {
    GoodsEntity entity = createGoods();
    dao.insert(entity); // ❌
    return entity;
}
```
**Fix**: Return unpersisted
```java
// CORRECT - Let test control persistence
public static GoodsEntity createGoods() {
    return entity; // Test calls dao.insert()
}
```

---

## Edge Cases Quick Guide

| Scenario | Pattern |
|----------|---------|
| **OneToMany** | Separate factory per entity |
| **Validation (@Email)** | Private helper (uniqueEmail()) |
| **Enum fields** | Default to common value |
| **JSON fields** | Default map + override variant |
| **Composite keys** | Require all parts as params |
| **Tree (self-FK)** | Separate root/child factories |
| **Unique constraints** | Timestamp + counter |
| **Audit fields** | Let DB/MyBatis handle |

**Full edge cases**: See `EDGE-CASES.md`

---

## Template (Copy-Paste Ready)

```java
package net.lab1024.sa.admin.module.{module}.{submodule}.service;

import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.{module}.{submodule}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.{module}.{submodule}.domain.form.{Entity}AddForm;
import net.lab1024.sa.admin.module.{module}.{submodule}.domain.form.{Entity}UpdateForm;
import net.lab1024.sa.admin.module.{module}.{submodule}.domain.form.{Entity}QueryForm;

/**
 * Test fixtures for {Entity} integration tests
 *
 * @author Claude Code (test-fixture-generator skill)
 * @since {date}
 */
public class {Entity}TestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  public static {Entity}Entity create{Entity}() {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    {Entity}Entity entity = new {Entity}Entity();
    // Set all required fields with unique values
    entity.setName("{Entity}-" + id);
    entity.setDeletedFlag(false);
    // ... other fields
    return entity;
  }

  public static {Entity}Entity create{Entity}(Long relatedId) {
    {Entity}Entity entity = create{Entity}();
    entity.setRelatedId(relatedId);
    return entity;
  }

  public static {Entity}AddForm createAddForm(Long relatedId) {
    int id = counter.incrementAndGet();

    {Entity}AddForm form = new {Entity}AddForm();
    form.setName("{Entity}-" + id);
    form.setRelatedId(relatedId);
    // ... other fields
    return form;
  }

  public static {Entity}UpdateForm createUpdateForm(Long entityId, Long relatedId) {
    int id = counter.incrementAndGet();

    {Entity}UpdateForm form = new {Entity}UpdateForm();
    form.setEntityId(entityId);
    form.setName("更新后{Entity}-" + id);
    form.setRelatedId(relatedId);
    return form;
  }

  public static {Entity}QueryForm createQueryForm() {
    {Entity}QueryForm form = new {Entity}QueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  public static {Related}Entity create{Related}(String name) {
    {Related}Entity entity = new {Related}Entity();
    entity.setName(name);
    // ... minimal required fields
    return entity;
  }

  public static void resetCounter() {
    counter.set(0);
  }
}
```

---

## Integration Test Template

```java
@SpringBootTest
@Transactional
@DisplayName("{Entity}Service Integration Tests")
class {Entity}ServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired private {Entity}Service service;
  @Autowired private {Entity}Dao dao;
  @Autowired private {Related}Dao relatedDao;

  private Long testRelatedId;

  @Override
  @BeforeEach
  protected void setUp() {
    {Related}Entity related = {Entity}TestFixture.create{Related}("测试数据");
    relatedDao.insert(related);
    testRelatedId = related.get{Related}Id();
  }

  @Test
  @DisplayName("Should add {entity} and persist to database")
  void add{Entity}_ValidForm_PersistsToDatabase() {
    // Given
    {Entity}AddForm form = {Entity}TestFixture.createAddForm(testRelatedId);

    // When
    ResponseDTO<String> response = service.add{Entity}(form);

    // Then
    assertTrue(response.getOk());
    // Verify DB state...
  }
}
```

---

## Reference Files

**Pattern Source**: `/smart-admin-api-java21-springboot3/sa-admin/src/test/java/net/lab1024/sa/admin/module/system/employee/service/EmployeeTestFixture.java`

**Integration Test Example**: `EmployeeServiceIntegrationTest.java` (same directory)

**Full Skill Documentation**: `.claude/skills/test-fixture-generator/SKILL.md`

**Edge Cases**: `.claude/skills/test-fixture-generator/EDGE-CASES.md`

---

## When to Use

✅ **Use fixture when**:
- Creating integration tests (`@SpringBootTest`)
- Testing with real DB and transactions
- Need multiple tests with same entity type
- Entity has FK relationships

❌ **Skip fixture when**:
- Unit tests with mocks (not integration tests)
- One-off test with unique setup
- Testing infrastructure (not business logic)

---

## Checklist: Fixture Completeness

Before committing fixture, verify:

- [ ] `AtomicInteger counter` declared
- [ ] `create{Entity}()` with ALL required fields
- [ ] `create{Entity}(Long fkId)` override methods
- [ ] `createAddForm(Long fkId)` for service tests
- [ ] `createUpdateForm(Long id, Long fkId)` for updates
- [ ] `createQueryForm()` with pagination defaults
- [ ] `create{Related}(String name)` for FK dependencies
- [ ] `resetCounter()` utility method
- [ ] BigDecimal fields use String constructor
- [ ] Boolean flags have defaults (false)
- [ ] String fields use counter for uniqueness
- [ ] Javadoc with usage examples

---

**Last Updated**: 2026-01-25
**Skill Version**: 1.0.0
**Questions**: See full `SKILL.md` or `README.md`
