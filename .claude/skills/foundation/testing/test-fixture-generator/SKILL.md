---
name: test-fixture-generator
description: [P0 - Critical] Use when writing integration tests and need test data builders for complex domain objects (Entity, Form, VO), or when agent creates inline test data setup instead of reusable fixture class, or when implementing test data preparation for BaseIntegrationTest subclasses
---

# Test Fixture Generator Skill

## Purpose

Generate reusable test fixture builders following SmartAdmin's **EmployeeTestFixture pattern** for:
- **Entities**: Domain objects with auto-generated IDs, FK relationships, timestamps
- **Forms**: AddForm, UpdateForm, QueryForm with validation
- **VOs**: Read-only view objects

**Benefits**:
- Eliminates inline test data boilerplate (10+ lines → 1 line)
- Guarantees uniqueness via counters (no constraint violations)
- Provides fluent API for overrides
- Integrates with `@Transactional` test rollback
- Supports complex object graphs (related entities)

---

## When to Use This Skill

**Triggers**:
1. Creating integration tests (`*IntegrationTest.java`)
2. Need test data for `@SpringBootTest` with real DB
3. Entity has FK relationships requiring related object setup
4. Agent creates inline setup code instead of fixture class
5. Multiple tests need same domain object with different values
6. User mentions: "test fixture", "test data builder", "integration test setup"

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "test fixture" - Generate test fixture builder class
- "test data builder" - Create reusable test data builders
- "generate fixture" - Explicit fixture generation request
- "EmployeeTestFixture" - Reference to SmartAdmin fixture pattern
- "integration test setup" - Test data setup for integration tests

**Secondary Keywords** (Medium confidence):
- "create test data" - Context: for integration tests
- "AtomicInteger" - Context: unique test values generation
- "unique test values" - Context: avoiding constraint violations
- "builder pattern" - Context: test data builders
- "BaseIntegrationTest" - Context: SmartAdmin integration test base class

**Phrase Patterns**:
- "Generate test fixture for [Entity]" - Example: "Generate test fixture for Product"
- "Create [Entity]TestFixture" - Example: "Create EmployeeTestFixture"
- "I need test data builder for [Entity]" - Example: "I need test data builder for Order"

**Example User Requests**:
```
User: "Generate test fixture for Product entity"
User: "Create CustomerTestFixture following EmployeeTestFixture pattern"
User: "I need a test data builder for Order with unique values"
User: "Setup integration test data for Employee module"
```

**Note**: This skill can also be manually invoked via `/test-fixture-generator` command.

---

## Core Pattern: EmployeeTestFixture Analysis

### Key Components

1. **Uniqueness Counter**: `AtomicInteger` for sequential IDs
2. **Static Factory Methods**: No constructors, only static methods
3. **Default Builder**: Creates entity with all required fields
4. **Override Variants**: Parameter-based customization
5. **Related Entity Factories**: Create FK dependencies
6. **Utility Helpers**: resetCounter(), uniquePhone(), uniqueEmail()

### Pattern Template

```java
public class {Entity}TestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  // 1. Default builder (unpersisted)
  public static {Entity}Entity create{Entity}() {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    {Entity}Entity entity = new {Entity}Entity();
    // Set ALL required fields with unique values
    return entity;
  }

  // 2. Override variant (FK customization)
  public static {Entity}Entity create{Entity}(Long relatedId) {
    {Entity}Entity entity = create{Entity}();
    entity.setRelatedId(relatedId);
    return entity;
  }

  // 3. Form builders (for service tests)
  public static {Entity}AddForm createAddForm(Long relatedId) {
    // Similar pattern
  }

  // 4. Related entity factory
  public static RelatedEntity createRelated(String name) {
    // Minimal setup for FK dependency
  }

  // 5. Counter reset (test isolation)
  public static void resetCounter() {
    counter.set(0);
  }

  // 6. Private helpers (unique values)
  private static String unique{Field}() {
    // Generate unique constrained values
  }
}
```

---

## Step-by-Step Generation Guide

### Step 1: Analyze Entity Structure

**Read entity file** to identify:
- Primary key field (auto-generated ID)
- Required vs optional fields (nullable, validation annotations)
- FK relationships (DepartmentId, CategoryId, etc.)
- Special field types (BigDecimal, Enum, JSON, Boolean)
- Constraints (unique indexes, length limits)

**Example**: GoodsEntity
```java
@Data
@TableName("t_goods")
public class GoodsEntity {
  @TableId(type = IdType.AUTO) private Long goodsId;      // ← Primary key
  private Integer goodsStatus;                             // ← Enum-like (1,2,3)
  private Long categoryId;                                 // ← FK to Category
  private String goodsName;                                // ← Required string
  private String place;                                    // ← Optional string
  private BigDecimal price;                                // ← Decimal field
  private Boolean shelvesFlag;                             // ← Boolean field
  private Boolean deletedFlag;                             // ← Soft delete flag
  private String remark;                                   // ← Optional
  private LocalDateTime createTime, updateTime;            // ← Timestamps
}
```

**Identified Requirements**:
- Counter for uniqueness: `AtomicInteger counter`
- FK factory: `createCategory(String name)`
- BigDecimal handling: Use `new BigDecimal("...")` string constructor
- Boolean defaults: `false` for flags
- Unique names: `"Goods-" + id` pattern

---

### Step 2: Create Fixture Class

**File Path**: `{module}/service/{Entity}TestFixture.java`

**Example**: `business/goods/service/GoodsTestFixture.java`

```java
package net.lab1024.sa.business.goods.service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.business.goods.domain.form.GoodsQueryForm;
import net.lab1024.sa.business.goods.domain.form.GoodsUpdateForm;

/**
 * Test fixtures for Goods integration tests
 *
 * <p>Provides reusable test data builders for Goods entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * GoodsEntity entity = GoodsTestFixture.createGoods();
 *
 * // Create entity with specific category
 * GoodsEntity entity = GoodsTestFixture.createGoods(categoryId);
 *
 * // Create add form
 * GoodsAddForm form = GoodsTestFixture.createAddForm(categoryId);
 * }</pre>
 *
 * @author Claude Code (test-fixture-generator skill)
 * @since {currentDate}
 */
public class GoodsTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create GoodsEntity with unique test data
   *
   * @return Entity with all required fields set
   */
  public static GoodsEntity createGoods() {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsName("商品-" + id);
    entity.setGoodsStatus(2); // Default: 售卖中
    entity.setPrice(new BigDecimal("99.99").add(BigDecimal.valueOf(id))); // Unique price
    entity.setPlace("测试产地-" + id);
    entity.setShelvesFlag(true);
    entity.setDeletedFlag(false);
    entity.setCategoryId(1L); // Default category (override via parameter)
    entity.setRemark("测试备注-" + timestamp);

    return entity;
  }

  /**
   * Create GoodsEntity with specific category ID
   *
   * @param categoryId Category ID
   * @return Entity with category set
   */
  public static GoodsEntity createGoods(Long categoryId) {
    GoodsEntity entity = createGoods();
    entity.setCategoryId(categoryId);
    return entity;
  }

  /**
   * Create GoodsAddForm with default test data
   *
   * @param categoryId Category ID (required FK)
   * @return Form ready for service.addGoods()
   */
  public static GoodsAddForm createAddForm(Long categoryId) {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    GoodsAddForm form = new GoodsAddForm();
    form.setGoodsName("商品-" + id);
    form.setGoodsStatus(2);
    form.setPrice(new BigDecimal("99.99").add(BigDecimal.valueOf(id)));
    form.setPlace("测试产地-" + id);
    form.setShelvesFlag(true);
    form.setCategoryId(categoryId);
    form.setRemark("测试备注-" + timestamp);

    return form;
  }

  /**
   * Create GoodsUpdateForm for updating existing goods
   *
   * @param goodsId ID of goods to update
   * @param categoryId Category ID
   * @return Form ready for service.updateGoods()
   */
  public static GoodsUpdateForm createUpdateForm(Long goodsId, Long categoryId) {
    int id = counter.incrementAndGet();

    GoodsUpdateForm form = new GoodsUpdateForm();
    form.setGoodsId(goodsId);
    form.setGoodsName("更新后商品-" + id);
    form.setGoodsStatus(2);
    form.setPrice(new BigDecimal("199.99"));
    form.setPlace("更新后产地-" + id);
    form.setShelvesFlag(true);
    form.setCategoryId(categoryId);
    form.setRemark("更新后备注-" + id);

    return form;
  }

  /**
   * Create GoodsQueryForm with pagination defaults
   *
   * @return Form ready for service.queryGoods()
   */
  public static GoodsQueryForm createQueryForm() {
    GoodsQueryForm form = new GoodsQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create CategoryEntity for FK dependency
   *
   * @param categoryName Category name
   * @return Category entity
   */
  public static CategoryEntity createCategory(String categoryName) {
    CategoryEntity category = new CategoryEntity();
    category.setCategoryName(categoryName);
    category.setCategoryType(1); // Default type
    category.setParentId(0L);
    category.setSort(1);
    category.setDisabledFlag(false);
    category.setDeletedFlag(false);
    return category;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
```

---

### Step 3: Default Value Strategies

#### String Fields
- **Pattern**: `"{fieldName}-" + id` (Chinese or English)
- **Example**: `"商品-1"`, `"商品-2"`, ... (unique, readable)
- **For unique constraints**: Add timestamp: `"login_" + timestamp`

#### Numeric Fields (Integer/Long)
- **Sequential**: Use counter: `counter.incrementAndGet()`
- **Enums**: Use valid constant: `goodsStatus = 2`
- **Sorting**: Default `1`

#### BigDecimal Fields
- **Use String constructor**: `new BigDecimal("99.99")` (precision-safe)
- **Make unique**: Add counter: `basePrice.add(BigDecimal.valueOf(id))`
- **NEVER**: `new BigDecimal(99.99)` (double precision loss)

#### Boolean Fields
- **Flags**: Default `false` (deleted, disabled, shelves)
- **Active states**: Context-dependent (`shelvesFlag = true` for active goods)

#### LocalDateTime Fields
- **createTime/updateTime**: Let DB handle (auto-fill) OR `LocalDateTime.now()`
- **NEVER**: Use static dates (breaks time-sensitive tests)

#### Foreign Key Fields
- **Default**: `1L` (override via parameter)
- **Pattern**: Always provide override method: `createEntity(Long fkId)`

---

### Step 4: Related Entity Factories

**Always include** factories for FK dependencies:

```java
/**
 * Create {RelatedEntity} for FK dependency
 *
 * @param name Entity name
 * @return Related entity (NOT persisted - caller must insert via Dao)
 */
public static CategoryEntity createCategory(String categoryName) {
    CategoryEntity category = new CategoryEntity();
    category.setCategoryName(categoryName);
    category.setParentId(0L);
    category.setSort(1);
    category.setDisabledFlag(false);
    category.setDeletedFlag(false);
    return category;
}
```

**Usage in Test**:
```java
@BeforeEach
void setUp() {
    // Create FK dependency
    CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
    categoryDao.insert(category);
    testCategoryId = category.getCategoryId();
}
```

---

### Step 5: Integration Test Usage

**File Path**: `{module}/service/{Entity}ServiceIntegrationTest.java`

**Example**:
```java
@SpringBootTest
@Transactional
@DisplayName("GoodsService Integration Tests")
class GoodsServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired private GoodsService goodsService;
  @Autowired private GoodsDao goodsDao;
  @Autowired private CategoryDao categoryDao;

  private Long testCategoryId;

  @Override
  @BeforeEach
  protected void setUp() {
    // Create FK dependency using fixture
    CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
    categoryDao.insert(category);
    testCategoryId = category.getCategoryId();
  }

  @Test
  @DisplayName("Should add goods and persist to database")
  void addGoods_ValidForm_PersistsToDatabase() {
    // Given - One-liner fixture usage!
    GoodsAddForm form = GoodsTestFixture.createAddForm(testCategoryId);

    // When
    ResponseDTO<String> response = goodsService.addGoods(form);

    // Then
    assertTrue(response.getOk());
    // Verify DB state...
  }

  @Test
  @DisplayName("Should update goods price")
  void updateGoods_NewPrice_PersistsChanges() {
    // Given - Create initial goods
    GoodsAddForm addForm = GoodsTestFixture.createAddForm(testCategoryId);
    goodsService.addGoods(addForm);
    GoodsEntity saved = goodsDao.getByName(addForm.getGoodsName());

    // Create update form with custom price
    GoodsUpdateForm updateForm = GoodsTestFixture.createUpdateForm(
        saved.getGoodsId(), testCategoryId);
    updateForm.setPrice(new BigDecimal("299.99"));

    // When
    goodsService.updateGoods(updateForm);

    // Then
    GoodsEntity updated = goodsDao.selectById(saved.getGoodsId());
    assertEquals(new BigDecimal("299.99"), updated.getPrice());
  }
}
```

---

## Common Mistakes and Fixes

### Mistake 1: Using Lombok @Builder
```java
// ❌ WRONG - Not SmartAdmin pattern
@Builder
public static GoodsEntity createGoods() {
    return GoodsEntity.builder()
        .goodsName("Test")
        .build();
}
```

**Fix**: Use parameter-based overrides
```java
// ✅ CORRECT - Parameter override pattern
public static GoodsEntity createGoods(Long categoryId) {
    GoodsEntity entity = createGoods();
    entity.setCategoryId(categoryId);
    return entity;
}
```

---

### Mistake 2: Forgetting AtomicInteger Counter
```java
// ❌ WRONG - Static counter (not thread-safe)
private static int counter = 0;

public static GoodsEntity createGoods() {
    counter++; // ❌ Not atomic
    entity.setGoodsName("Goods-" + counter);
}
```

**Fix**: Use AtomicInteger
```java
// ✅ CORRECT
private static final AtomicInteger counter = new AtomicInteger(0);

public static GoodsEntity createGoods() {
    int id = counter.incrementAndGet();
    entity.setGoodsName("商品-" + id);
}
```

---

### Mistake 3: Wrong BigDecimal Construction
```java
// ❌ WRONG - Precision loss
entity.setPrice(new BigDecimal(99.99)); // Double precision issues
```

**Fix**: Use String constructor
```java
// ✅ CORRECT
entity.setPrice(new BigDecimal("99.99"));
// OR for dynamic values:
entity.setPrice(BigDecimal.valueOf(99.99));
```

---

### Mistake 4: No Related Entity Factory
```java
// ❌ WRONG - Manual FK setup in every test
@BeforeEach
void setUp() {
    CategoryEntity category = new CategoryEntity();
    category.setCategoryName("Test");
    category.setParentId(0L);
    category.setSort(1);
    categoryDao.insert(category); // 5 lines of boilerplate
}
```

**Fix**: Add factory method
```java
// ✅ CORRECT - One-liner
@BeforeEach
void setUp() {
    CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
    categoryDao.insert(category);
    testCategoryId = category.getCategoryId();
}
```

---

### Mistake 5: Non-Unique String Values
```java
// ❌ WRONG - Same value every time
entity.setGoodsName("Test Goods"); // Unique constraint violation
```

**Fix**: Use counter + timestamp
```java
// ✅ CORRECT
int id = counter.incrementAndGet();
long timestamp = System.currentTimeMillis();
entity.setGoodsName("商品-" + id);          // Readable + unique
entity.setRemark("备注-" + timestamp);     // Timestamp for extra uniqueness
```

---

### Mistake 6: Missing resetCounter() Method
```java
// ❌ WRONG - Counter keeps incrementing across test classes
// GoodsTestFixture counter: 1, 2, 3...
// Next test class: counter still at 3, causing unexpected IDs
```

**Fix**: Always include reset method
```java
// ✅ CORRECT
public static void resetCounter() {
    counter.set(0);
}

// Optional usage in test (usually not needed with @Transactional):
@BeforeEach
void setUp() {
    GoodsTestFixture.resetCounter();
}
```

---

### Mistake 7: Returning Persisted Entities
```java
// ❌ WRONG - Fixture shouldn't handle persistence
public static GoodsEntity createGoods(GoodsDao dao) {
    GoodsEntity entity = createGoods();
    dao.insert(entity); // ❌ Persistence is test's responsibility
    return entity;
}
```

**Fix**: Return unpersisted entities
```java
// ✅ CORRECT - Caller controls persistence
public static GoodsEntity createGoods() {
    return entity; // Let test insert via Dao
}

// In test:
GoodsEntity goods = GoodsTestFixture.createGoods(categoryId);
goodsDao.insert(goods); // ✅ Test controls when to persist
```

**Exception**: EmployeeTestFixture has `createDepartment()` helper that doesn't persist, caller must insert.

---

## Advanced Scenarios

### Scenario 1: Enum Fields
```java
// Entity has GoodsStatusEnum
private GoodsStatusEnum status;

// ✅ Fixture uses valid enum constant
entity.setStatus(GoodsStatusEnum.ON_SALE); // Not magic number
```

### Scenario 2: JSON Fields
```java
// Entity has JSON field
@TableField(typeHandler = JacksonTypeHandler.class)
private Map<String, Object> metadata;

// ✅ Fixture provides default map
entity.setMetadata(Map.of("key", "value-" + id));
```

### Scenario 3: Complex FK Graphs
```java
// Goods → Category → Parent Category (3-level hierarchy)

public static CategoryEntity createRootCategory() {
    CategoryEntity root = new CategoryEntity();
    root.setCategoryName("根分类");
    root.setParentId(0L); // Root marker
    return root;
}

public static CategoryEntity createSubCategory(Long parentId) {
    CategoryEntity sub = createRootCategory();
    sub.setParentId(parentId);
    return sub;
}

// In test:
CategoryEntity root = GoodsTestFixture.createRootCategory();
categoryDao.insert(root);

CategoryEntity sub = GoodsTestFixture.createSubCategory(root.getCategoryId());
categoryDao.insert(sub);

GoodsEntity goods = GoodsTestFixture.createGoods(sub.getCategoryId());
```

---

## Rationalization Table

| Pattern | Rationale |
|---------|-----------|
| **AtomicInteger counter** | Thread-safe uniqueness, prevents constraint violations |
| **Static factory methods** | No state, pure utility class, testable |
| **Parameter-based overrides** | Simple, type-safe, follows SmartAdmin convention |
| **BigDecimal String constructor** | Avoids double precision loss (financial data) |
| **Counter + timestamp for strings** | Guarantees uniqueness, readable IDs |
| **Boolean defaults = false** | Safe default (not deleted, not disabled) |
| **FK default = 1L** | Valid placeholder, override via parameter |
| **No Dao injection** | Fixture is pure data builder, test controls persistence |
| **resetCounter() method** | Test isolation (optional with @Transactional) |
| **Related entity factories** | Reduces boilerplate, consistent FK setup |
| **Javadoc @author = skill name** | Traceability for generated code |

---

## Deliverables Checklist

When generating fixture, ensure:

- [ ] `AtomicInteger counter` declared
- [ ] `create{Entity}()` method with ALL required fields
- [ ] `create{Entity}(Long fkId)` override methods
- [ ] `createAddForm(Long fkId)` for service tests
- [ ] `createUpdateForm(Long id, Long fkId)` for update tests
- [ ] `createQueryForm()` with pagination defaults
- [ ] `create{RelatedEntity}(String name)` for FK dependencies
- [ ] `resetCounter()` utility method
- [ ] BigDecimal fields use String constructor
- [ ] Boolean flags have sensible defaults
- [ ] String fields use counter for uniqueness
- [ ] Javadoc with usage examples
- [ ] File location: `{module}/service/{Entity}TestFixture.java`

---

## References

**Pattern Source**: `/smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/net/lab1024/sa/app/system/employee/service/EmployeeTestFixture.java`

**Integration Test Example**: `/smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/net/lab1024/sa/app/system/employee/service/EmployeeServiceIntegrationTest.java`

**Base Class**: `/smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/net/lab1024/sa/app/BaseIntegrationTest.java`

---

## 相關規則

本技能生成的測試固件必須符合以下規範：

### 強制要求

- **[Testing Strategy](./../../../docs/testing/testing-strategy.md)**
  - 測試資料生成模式（Factory Method）
  - AtomicInteger 計數器保證唯一性
  - 測試隔離與資料清理策略

- **[Integration Testing Quick Reference](./../../../docs/testing/integration-testing-quick-reference.md)**
  - Fixture 使用場景（Service 測試、Controller 測試）
  - @BeforeEach 中的 FK 依賴設置模式
  - 參數化覆寫方法模式

  - 測試固件類命名：`{Entity}TestFixture`（不是 `{Entity}Builder`）
  - Factory 方法命名：`create{Entity}()`、`create{Form}()`
  - 參數命名：具體業務名稱（`categoryId` 不是 `fkId`）

### 參考指引

- **[SmartAdmin Integration Test Skill](./../full-stack/smartadmin-integration-test/)**
  - 整合測試框架（配套技能）
  - Fixture 在整合測試中的使用範例
  - BaseIntegrationTest 配置

- **[Domain Objects Pattern](./../../../.claude/shared/knowledge/smartadmin-patterns.md#domain-object-pattern)**
  - Entity, Form, VO 對象命名規範
  - AddForm vs UpdateForm vs QueryForm 區別
  - 布林欄位預設值規範

---

## 參考資料

- [EmployeeTestFixture.java](./../../../../smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/net/lab1024/sa/app/system/employee/service/EmployeeTestFixture.java) - 完整範例
- [EmployeeServiceIntegrationTest.java](./../../../../smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/net/lab1024/sa/app/system/employee/service/EmployeeServiceIntegrationTest.java) - 使用範例
- [Test Fixture Pattern](https://martinfowler.com/bliki/TestFixture.html) - Martin Fowler 文章
