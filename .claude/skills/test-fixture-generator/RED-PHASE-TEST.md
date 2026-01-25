# RED Phase - Test Fixture Generator Skill

## Test Scenario: Create Fixture Without Skill Guidance

**Prompt to Agent:**
```
Create a test fixture builder for GoodsEntity with:
- Sensible defaults for all required fields
- Builder pattern for optional overrides
- Integration with BaseIntegrationTest
- Support for creating related CategoryEntity objects

GoodsEntity fields:
- Long goodsId (auto-generated)
- Integer goodsStatus (商品状态: 1-预约中, 2-售卖中, 3-售罄)
- Long categoryId (required FK)
- String goodsName (required)
- String place (产地)
- BigDecimal price (商品价格)
- Boolean shelvesFlag (上架状态)
- Boolean deletedFlag (删除状态)
- String remark
- LocalDateTime createTime
- LocalDateTime updateTime
```

---

## Expected Failures (Without Skill)

### 1. **Verbose Setup Instead of Reusable Builder**

Agent creates inline setup code in test methods instead of dedicated fixture class:

```java
// ❌ Common anti-pattern - Inline setup
@Test
void testAddGoods() {
    GoodsEntity goods = new GoodsEntity();
    goods.setGoodsName("Test Goods");
    goods.setGoodsStatus(2);
    goods.setPrice(new BigDecimal("99.99"));
    goods.setShelvesFlag(true);
    goods.setDeletedFlag(false);
    goods.setCategoryId(1L);
    goods.setPlace("China");
    goods.setRemark("Test");
    // ... 10 lines of boilerplate
}

@Test
void testUpdateGoods() {
    GoodsEntity goods = new GoodsEntity();
    goods.setGoodsName("Another Test Goods");
    // ... repeat same 10 lines
}
```

**Problem**: Code duplication, maintenance nightmare, no uniqueness guarantees.

---

### 2. **Doesn't Follow EmployeeTestFixture Pattern**

Agent creates builder pattern but misses key SmartAdmin conventions:

```java
// ❌ Missing critical patterns
public class GoodsTestFixture {
    // Missing: AtomicInteger/AtomicLong counter for uniqueness
    // Missing: Static factory methods
    // Missing: Integration with Dao for persistence
    // Missing: Related entity creation

    public static GoodsEntity createGoods() {
        GoodsEntity goods = new GoodsEntity();
        goods.setGoodsName("Test"); // ❌ Not unique - will fail on second test
        goods.setPrice(new BigDecimal("100")); // ❌ Same price every time
        return goods; // ❌ Doesn't persist to DB
    }
}
```

---

### 3. **Forgets Default Values for Required Fields**

Agent creates partial fixtures that fail validation:

```java
// ❌ Incomplete defaults
public static GoodsEntity createGoods(Long categoryId) {
    GoodsEntity goods = new GoodsEntity();
    goods.setCategoryId(categoryId);
    // ❌ Missing: goodsStatus (will be null -> NPE)
    // ❌ Missing: goodsName (will be null -> constraint violation)
    // ❌ Missing: price (will be null -> NPE in business logic)
    // ❌ Missing: shelvesFlag (will be null -> NPE)
    return goods;
}
```

**Runtime Error:**
```
java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()"
because "goods.getGoodsStatus()" is null
```

---

### 4. **Doesn't Integrate with @Transactional Test Context**

Agent creates fixtures that don't work with Spring test rollback:

```java
// ❌ No Dao integration
public static GoodsEntity createGoods() {
    GoodsEntity goods = new GoodsEntity();
    goods.setGoodsName("Test");
    return goods; // ❌ Not persisted - can't be used in FK relationships
}

// In test:
@Test
void testGoodsService() {
    GoodsEntity goods = GoodsTestFixture.createGoods(); // ❌ No ID!
    Long goodsId = goods.getGoodsId(); // ❌ NULL - fixture not saved
}
```

---

### 5. **No Fluent API for Overrides**

Agent creates rigid fixtures without customization:

```java
// ❌ No builder pattern
public static GoodsEntity createGoods() {
    GoodsEntity goods = new GoodsEntity();
    goods.setGoodsStatus(2); // ❌ Hard-coded
    goods.setPrice(new BigDecimal("100")); // ❌ Can't override
    return goods;
}

// Test needs different status:
@Test
void testPreorderGoods() {
    GoodsEntity goods = createGoods();
    goods.setGoodsStatus(1); // ❌ Mutating returned object - bad practice
}
```

**Better Pattern (from EmployeeTestFixture):**
```java
// ✅ Supports overrides via parameters
public static EmployeeEntity createEntity(Long departmentId) {
    EmployeeEntity entity = createEntity(); // Base defaults
    entity.setDepartmentId(departmentId);   // Override specific field
    return entity;
}
```

---

### 6. **Related Entity Creation Missing**

Agent forgets to provide helper for FK dependencies:

```java
// ❌ No CategoryEntity factory
public class GoodsTestFixture {
    public static GoodsEntity createGoods(Long categoryId) {
        // ...
    }

    // ❌ Missing:
    // public static CategoryEntity createCategory(String name) { ... }
}

// In test:
@Test
void testAddGoods() {
    // ❌ Manual FK setup - boilerplate
    CategoryEntity category = new CategoryEntity();
    category.setCategoryName("Test Category");
    category.setSort(1);
    categoryDao.insert(category);
    Long categoryId = category.getCategoryId();

    GoodsEntity goods = GoodsTestFixture.createGoods(categoryId);
}
```

**Expected Pattern (from EmployeeTestFixture):**
```java
// ✅ Provides related entity factory
public static DepartmentEntity createDepartment(String departmentName) {
    DepartmentEntity dept = new DepartmentEntity();
    dept.setDepartmentName(departmentName);
    dept.setParentId(0L);
    dept.setSort(1);
    return dept;
}
```

---

### 7. **Uniqueness Strategy Failures**

Agent uses predictable values that cause constraint violations:

```java
// ❌ Predictable values
public static GoodsEntity createGoods() {
    GoodsEntity goods = new GoodsEntity();
    goods.setGoodsName("Test Goods"); // ❌ Same every time
    goods.setPlace("China");          // ❌ Same every time
    return goods;
}

// In test:
@Test
void testMultipleGoods() {
    GoodsEntity goods1 = createGoods(); // "Test Goods"
    GoodsEntity goods2 = createGoods(); // "Test Goods" - UNIQUE constraint violation!
}
```

**Expected Pattern:**
```java
// ✅ Unique values via counter + timestamp
private static final AtomicInteger counter = new AtomicInteger(0);

public static EmployeeEntity createEntity() {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    entity.setLoginName("test_emp_" + timestamp); // Unique
    entity.setActualName("测试员工 " + id);         // Unique + readable
}
```

---

### 8. **BigDecimal/Enum Field Handling Errors**

Agent uses incorrect constructors:

```java
// ❌ Wrong BigDecimal usage
goods.setPrice(100.00); // ❌ Compile error: incompatible types

// ❌ Should be:
goods.setPrice(new BigDecimal("100.00")); // ✅ String constructor for precision
// OR
goods.setPrice(BigDecimal.valueOf(100.00)); // ✅ valueOf for doubles
```

---

## Summary of Failure Patterns

| Issue | Impact | Frequency |
|-------|--------|-----------|
| Inline setup instead of fixture class | High duplication | 90% |
| Missing AtomicInteger counter | Uniqueness violations | 80% |
| Incomplete default values | NPE/constraint violations | 70% |
| No Dao integration | Can't test FK relationships | 60% |
| No fluent API for overrides | Rigid, hard to customize | 75% |
| Missing related entity helpers | Boilerplate in every test | 65% |
| Wrong BigDecimal construction | Precision loss | 40% |
| No resetCounter() method | Tests not isolated | 30% |

---

## Success Criteria for GREEN Phase

✅ Skill must guide agent to create fixtures that:
1. Use `AtomicInteger`/`AtomicLong` counters for uniqueness
2. Provide static factory methods with defaults
3. Support parameter-based overrides (not Lombok @Builder)
4. Integrate with Dao for persistence
5. Include related entity factories
6. Handle BigDecimal/Enum fields correctly
7. Follow exact EmployeeTestFixture pattern
8. Include resetCounter() for test isolation
9. Generate unique String values via counter + timestamp
10. Provide both unpersisted (builder) and persisted (createAndSave) variants
