# Edge Cases and Advanced Scenarios

## Test Coverage for Test Fixture Generator Skill

This document validates the skill handles complex entity structures and edge cases.

---

## Edge Case 1: Complex Relationships (OneToMany)

**Scenario**: `NoticeEntity` has OneToMany relationship with `NoticeVisibleRangeEntity`

**Entity Structure**:
```java
@TableName("t_notice")
public class NoticeEntity {
    @TableId(type = IdType.AUTO)
    private Long noticeId;
    private Long noticeTypeId;      // FK to NoticeType
    private String title;
    private Integer visibleFlag;     // 0=all, 1=custom
    // ... other fields
}

@TableName("t_notice_visible_range")
public class NoticeVisibleRangeEntity {
    private Long noticeId;           // FK to Notice
    private Integer dataType;        // 1=department, 2=employee
    private Long dataId;
}
```

**Fixture Solution**:
```java
public class NoticeTestFixture {

    // Main entity
    public static NoticeEntity createNotice(Long noticeTypeId) {
        int id = counter.incrementAndGet();
        NoticeEntity entity = new NoticeEntity();
        entity.setTitle("通知-" + id);
        entity.setNoticeTypeId(noticeTypeId);
        entity.setVisibleFlag(0); // Default: all visible
        return entity;
    }

    // Related collection entity
    public static NoticeVisibleRangeEntity createVisibleRange(Long noticeId, Long dataId) {
        NoticeVisibleRangeEntity range = new NoticeVisibleRangeEntity();
        range.setNoticeId(noticeId);
        range.setDataType(1); // Department
        range.setDataId(dataId);
        return range;
    }

    // Parent entity factory
    public static NoticeTypeEntity createNoticeType(String name) {
        NoticeTypeEntity type = new NoticeTypeEntity();
        type.setTypeName(name);
        return type;
    }
}

// Usage in test:
@Test
void testNoticeWithVisibleRanges() {
    // Create parent FK
    NoticeTypeEntity type = NoticeTestFixture.createNoticeType("测试类型");
    noticeTypeDao.insert(type);

    // Create main entity
    NoticeEntity notice = NoticeTestFixture.createNotice(type.getNoticeTypeId());
    notice.setVisibleFlag(1); // Custom visibility
    noticeDao.insert(notice);

    // Create related collection
    NoticeVisibleRangeEntity range1 = NoticeTestFixture.createVisibleRange(
        notice.getNoticeId(), 1L); // Department 1
    NoticeVisibleRangeEntity range2 = NoticeTestFixture.createVisibleRange(
        notice.getNoticeId(), 2L); // Department 2

    visibleRangeDao.insert(range1);
    visibleRangeDao.insert(range2);

    // Test service...
}
```

**Pattern**: Provide separate factory methods for main entity and related collection entities.

---

## Edge Case 2: Entities with Validation Annotations

**Scenario**: `EmployeeEntity` has validation on phone format, email format

**Entity Structure**:
```java
public class EmployeeEntity {
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;            // Must be valid mobile

    @Email
    private String email;            // Must be valid email

    @NotBlank
    private String loginName;        // Cannot be empty
}
```

**Fixture Solution**:
```java
public class EmployeeTestFixture {

    public static EmployeeEntity createEntity() {
        int id = counter.incrementAndGet();
        long timestamp = System.currentTimeMillis();

        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName("test_emp_" + timestamp); // Unique + not blank
        entity.setPhone(uniquePhone());               // Validation-compliant
        entity.setEmail(uniqueEmail("employee" + id)); // Validation-compliant
        return entity;
    }

    /** Generate unique phone number matching validation regex */
    private static String uniquePhone() {
        int id = counter.get();
        // Format: 138 + 8 digits (mod to keep within bounds)
        return "138" + String.format("%08d", id % 100000000);
    }

    /** Generate unique email matching @Email format */
    private static String uniqueEmail(String name) {
        return name + "_" + System.currentTimeMillis() + "@test.com";
    }
}
```

**Pattern**: Create private helper methods that generate validation-compliant values.

---

## Edge Case 3: Entities with Enum Fields

**Scenario**: `GoodsEntity` has status as Integer enum (not Java Enum)

**Entity Structure**:
```java
public class GoodsEntity {
    /** 商品状态:[1:预约中, 2:售卖中, 3:售罄] */
    private Integer goodsStatus;
}
```

**Fixture Solution**:
```java
public class GoodsTestFixture {

    // Option 1: Default to most common state
    public static GoodsEntity createGoods() {
        entity.setGoodsStatus(2); // Default: 售卖中 (most common)
        return entity;
    }

    // Option 2: Provide variant for each state
    public static GoodsEntity createPreorderGoods(Long categoryId) {
        GoodsEntity entity = createGoods(categoryId);
        entity.setGoodsStatus(1); // 预约中
        return entity;
    }

    public static GoodsEntity createSoldOutGoods(Long categoryId) {
        GoodsEntity entity = createGoods(categoryId);
        entity.setGoodsStatus(3); // 售罄
        return entity;
    }

    // Option 3: Parameter override (recommended)
    public static GoodsEntity createGoods(Long categoryId, Integer goodsStatus) {
        GoodsEntity entity = createGoods(categoryId);
        entity.setGoodsStatus(goodsStatus);
        return entity;
    }
}
```

**Pattern**: Default to most common enum value, provide parameter override for edge cases.

---

## Edge Case 4: Entities with JSON Fields

**Scenario**: Entity has JSON field stored as String or Map

**Entity Structure**:
```java
public class ConfigEntity {
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata; // JSON field
}
```

**Fixture Solution**:
```java
public class ConfigTestFixture {

    public static ConfigEntity createConfig() {
        int id = counter.incrementAndGet();

        ConfigEntity entity = new ConfigEntity();
        entity.setMetadata(createDefaultMetadata(id)); // Helper for JSON
        return entity;
    }

    /** Create default JSON metadata */
    private static Map<String, Object> createDefaultMetadata(int id) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0");
        metadata.put("env", "test");
        metadata.put("id", id); // Make unique
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }

    /** Override metadata for custom tests */
    public static ConfigEntity createConfigWithMetadata(Map<String, Object> metadata) {
        ConfigEntity entity = createConfig();
        entity.setMetadata(metadata);
        return entity;
    }
}
```

**Pattern**: Provide default JSON structure via private helper, allow override via parameter.

---

## Edge Case 5: Entities with Composite Keys

**Scenario**: `RoleMenuEntity` has composite key (roleId + menuId)

**Entity Structure**:
```java
@TableName("t_role_menu")
public class RoleMenuEntity {
    private Long roleId;    // Part of composite key
    private Long menuId;    // Part of composite key
}
```

**Fixture Solution**:
```java
public class RoleMenuTestFixture {

    // No default factory - composite key MUST be provided
    public static RoleMenuEntity createRoleMenu(Long roleId, Long menuId) {
        RoleMenuEntity entity = new RoleMenuEntity();
        entity.setRoleId(roleId);
        entity.setMenuId(menuId);
        return entity;
    }

    // Batch creation helper
    public static List<RoleMenuEntity> createRoleMenus(Long roleId, List<Long> menuIds) {
        return menuIds.stream()
            .map(menuId -> createRoleMenu(roleId, menuId))
            .collect(Collectors.toList());
    }
}

// Usage:
@Test
void testRoleMenuAssignment() {
    List<RoleMenuEntity> roleMenus = RoleMenuTestFixture.createRoleMenus(
        roleId, List.of(1L, 2L, 3L));
    roleMenuDao.insertBatch(roleMenus);
}
```

**Pattern**: No default factory, require all composite key parts as parameters.

---

## Edge Case 6: Self-Referencing Entities (Tree Structure)

**Scenario**: `DepartmentEntity` has parentId (self-FK)

**Entity Structure**:
```java
public class DepartmentEntity {
    @TableId(type = IdType.AUTO)
    private Long departmentId;
    private Long parentId;       // Self-FK (tree structure)
    private String departmentName;
}
```

**Fixture Solution**:
```java
public class DepartmentTestFixture {

    // Root department (parentId = 0)
    public static DepartmentEntity createRootDepartment(String name) {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDepartmentName(name);
        dept.setParentId(0L);    // Root marker
        dept.setSort(1);
        return dept;
    }

    // Child department (with parent FK)
    public static DepartmentEntity createChildDepartment(String name, Long parentId) {
        DepartmentEntity dept = createRootDepartment(name);
        dept.setParentId(parentId); // Override root marker
        return dept;
    }
}

// Usage:
@Test
void testDepartmentHierarchy() {
    // Create root
    DepartmentEntity root = DepartmentTestFixture.createRootDepartment("总部");
    departmentDao.insert(root);

    // Create children
    DepartmentEntity hr = DepartmentTestFixture.createChildDepartment(
        "人力资源部", root.getDepartmentId());
    DepartmentEntity it = DepartmentTestFixture.createChildDepartment(
        "技术部", root.getDepartmentId());

    departmentDao.insert(hr);
    departmentDao.insert(it);
}
```

**Pattern**: Separate factory methods for root and child nodes.

---

## Edge Case 7: Entities with Unique Constraints

**Scenario**: `EmployeeEntity` has unique constraints on loginName, phone, email

**Entity Structure**:
```java
@TableName("t_employee")
public class EmployeeEntity {
    @TableField(unique = true)
    private String loginName;    // Unique constraint

    @TableField(unique = true)
    private String phone;        // Unique constraint

    @TableField(unique = true)
    private String email;        // Unique constraint
}
```

**Fixture Solution**:
```java
public class EmployeeTestFixture {

    public static EmployeeEntity createEntity() {
        int id = counter.incrementAndGet();
        long timestamp = System.currentTimeMillis();

        EmployeeEntity entity = new EmployeeEntity();
        // Uniqueness via timestamp (most reliable)
        entity.setLoginName("test_emp_" + timestamp);

        // Uniqueness via counter (readable)
        entity.setActualName("测试员工 " + id);

        // Uniqueness via helper (validation-compliant)
        entity.setPhone(uniquePhone());
        entity.setEmail(uniqueEmail("employee" + id));

        return entity;
    }

    private static String uniquePhone() {
        int id = counter.get();
        return "138" + String.format("%08d", id % 100000000);
    }

    private static String uniqueEmail(String name) {
        return name + "_" + System.currentTimeMillis() + "@test.com";
    }
}
```

**Pattern**: Use timestamp for high-collision fields (loginName), counter for readable fields (name).

---

## Edge Case 8: Entities with Large Text Fields

**Scenario**: `NoticeEntity` has content field (TEXT/CLOB)

**Entity Structure**:
```java
public class NoticeEntity {
    @TableField(jdbcType = JdbcType.CLOB)
    private String content;      // Large text field
}
```

**Fixture Solution**:
```java
public class NoticeTestFixture {

    public static NoticeEntity createNotice(Long noticeTypeId) {
        int id = counter.incrementAndGet();

        NoticeEntity entity = new NoticeEntity();
        entity.setTitle("通知-" + id);
        entity.setContent(createDefaultContent(id)); // Helper for large text
        entity.setNoticeTypeId(noticeTypeId);
        return entity;
    }

    /** Create default content (avoid huge strings in every test) */
    private static String createDefaultContent(int id) {
        return "这是测试通知内容 " + id + "。\n"
            + "包含多行文本。\n"
            + "模拟实际通知内容格式。";
    }

    /** Override content for specific tests */
    public static NoticeEntity createNoticeWithLongContent(Long noticeTypeId) {
        NoticeEntity entity = createNotice(noticeTypeId);
        entity.setContent(createLongContent()); // 10KB+ content
        return entity;
    }

    private static String createLongContent() {
        return "测试内容".repeat(1000); // Simulate large text
    }
}
```

**Pattern**: Default to concise content, provide variant for large text edge cases.

---

## Edge Case 9: Entities with Calculated/Derived Fields

**Scenario**: `EmployeeEntity` has derived field computed from other fields

**Entity Structure**:
```java
public class EmployeeEntity {
    private String firstName;
    private String lastName;

    // Derived field (not stored in DB, computed at runtime)
    @TableField(exist = false)
    private String fullName;
}
```

**Fixture Solution**:
```java
public class EmployeeTestFixture {

    public static EmployeeEntity createEntity() {
        int id = counter.incrementAndGet();

        EmployeeEntity entity = new EmployeeEntity();
        entity.setFirstName("张");
        entity.setLastName("三" + id);

        // Do NOT set derived fields (let entity compute them)
        // entity.setFullName(...); // ❌ WRONG

        return entity;
    }
}
```

**Pattern**: NEVER set derived fields in fixtures, let entity's logic compute them.

---

## Edge Case 10: Entities with Audit Fields (BaseEntity)

**Scenario**: Entity extends `BaseEntity` with common audit fields

**Entity Structure**:
```java
public abstract class BaseEntity {
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUserId;
    private Long updateUserId;
}

public class GoodsEntity extends BaseEntity {
    // Business fields...
}
```

**Fixture Solution**:
```java
public class GoodsTestFixture {

    public static GoodsEntity createGoods() {
        GoodsEntity entity = new GoodsEntity();

        // Business fields
        entity.setGoodsName("商品-" + counter.incrementAndGet());

        // Audit fields - let DB/MyBatis handle
        // entity.setCreateTime(LocalDateTime.now()); // ❌ WRONG - DB auto-fills
        // entity.setCreateUserId(1L);                 // ❌ WRONG - set by interceptor

        return entity;
    }

    // If test needs specific audit fields (edge case):
    public static GoodsEntity createGoodsWithAudit(Long createUserId) {
        GoodsEntity entity = createGoods();
        entity.setCreateUserId(createUserId);
        entity.setCreateTime(LocalDateTime.now()); // Only for special tests
        return entity;
    }
}
```

**Pattern**: Let DB/MyBatis auto-fill audit fields, only override for specific tests.

---

## Rationalization Table for Edge Cases

| Edge Case | Strategy | Rationale |
|-----------|----------|-----------|
| **OneToMany relationships** | Separate factory per entity | Caller controls persistence order |
| **Validation annotations** | Private helpers generate compliant values | Avoids validation errors in all tests |
| **Enum fields (Integer)** | Default to most common value | Reduces test boilerplate |
| **JSON fields** | Default map + override variant | Balance simplicity and flexibility |
| **Composite keys** | Require all parts as parameters | No sensible defaults for composite keys |
| **Self-referencing (tree)** | Separate root/child factories | Explicit tree structure in tests |
| **Unique constraints** | Timestamp + counter strategy | Guarantees uniqueness across parallel tests |
| **Large text fields** | Concise default + override variant | Fast tests, explicit large data tests |
| **Derived fields** | Never set, let entity compute | Avoids inconsistent state |
| **Audit fields (BaseEntity)** | Let DB/interceptor handle | Matches production behavior |

---

## Common Mistake Patterns and Counters

### Mistake: Overcomplicating Fixtures

```java
// ❌ WRONG - Too complex, too coupled
public static GoodsEntity createGoodsAndPersist(
    GoodsDao dao, CategoryDao categoryDao, String categoryName) {

    CategoryEntity category = new CategoryEntity();
    category.setCategoryName(categoryName);
    categoryDao.insert(category);

    GoodsEntity goods = new GoodsEntity();
    goods.setCategoryId(category.getCategoryId());
    dao.insert(goods);

    return goods;
}
```

**Fix**: Keep fixtures simple, let test control persistence
```java
// ✅ CORRECT - Simple, decoupled
public static GoodsEntity createGoods(Long categoryId) {
    GoodsEntity entity = new GoodsEntity();
    entity.setCategoryId(categoryId);
    return entity;
}

public static CategoryEntity createCategory(String name) {
    CategoryEntity entity = new CategoryEntity();
    entity.setCategoryName(name);
    return entity;
}

// In test:
CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
categoryDao.insert(category);
GoodsEntity goods = GoodsTestFixture.createGoods(category.getCategoryId());
goodsDao.insert(goods);
```

---

### Mistake: Not Handling NULL Values

```java
// ❌ WRONG - Optional fields left null (may cause NPE)
entity.setPlace(null);
entity.setRemark(null);
```

**Fix**: Provide sensible defaults for ALL fields
```java
// ✅ CORRECT
entity.setPlace("测试产地-" + id);      // Optional but populated
entity.setRemark("测试备注-" + timestamp); // Prevents NPE
```

---

### Mistake: Hard-Coding Test Data

```java
// ❌ WRONG - Hard-coded values (not unique)
entity.setGoodsName("iPhone 15");
entity.setPrice(new BigDecimal("5999.00"));
```

**Fix**: Use dynamic values
```java
// ✅ CORRECT
entity.setGoodsName("商品-" + id);
entity.setPrice(new BigDecimal("99.99").add(BigDecimal.valueOf(id)));
```

---

## Final Checklist for Edge Cases

When handling complex entities, ensure fixture:

- [ ] Handles all validation annotations (@NotNull, @Pattern, @Email, etc.)
- [ ] Generates unique values for unique constraints
- [ ] Provides separate factories for OneToMany relationships
- [ ] Uses sensible defaults for enum/status fields
- [ ] Creates default JSON structure for JSON fields
- [ ] Supports tree structures (root/child factories)
- [ ] Lets DB/MyBatis handle audit fields (unless testing audit logic)
- [ ] Never sets derived/computed fields
- [ ] Provides parameter overrides for edge cases
- [ ] Keeps factories simple, lets test control persistence
