# Test Fixture Generator - Quick Reference

## 基本模式

```java
public class {Entity}TestFixture {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static {Entity}Entity createEntity() {
        int id = COUNTER.incrementAndGet();
        {Entity}Entity entity = new {Entity}Entity();
        entity.setName("Test" + id);
        entity.setEmail("test" + id + "@example.com");
        entity.setDeleted(false);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    public static {Entity}AddForm createAddForm() {
        int id = COUNTER.incrementAndGet();
        {Entity}AddForm form = new {Entity}AddForm();
        form.setName("Test" + id);
        form.setEmail("test" + id + "@example.com");
        return form;
    }

    public static {Entity}UpdateForm createUpdateForm(Long entityId) {
        {Entity}UpdateForm form = new {Entity}UpdateForm();
        form.setEntityId(entityId);
        form.setName("Updated");
        return form;
    }

    public static {Entity}QueryForm createQueryForm() {
        {Entity}QueryForm form = new {Entity}QueryForm();
        form.setPageNum(1);
        form.setPageSize(10);
        return form;
    }
}
```

## 欄位類型對應

| 欄位類型 | 生成方式 |
|----------|----------|
| String (name) | `"Test" + id` |
| String (email) | `"test" + id + "@example.com"` |
| String (phone) | `"1380000" + String.format("%04d", id)` |
| Long (FK) | `1L` or parameter |
| Boolean | `false` |
| LocalDateTime | `LocalDateTime.now()` |
| BigDecimal | `BigDecimal.valueOf(100.00 + id)` |
| Integer | `id` |
| Enum | `EnumType.DEFAULT` |

## 使用範例

```java
@Test
void add_ValidForm_CreatesEntity() {
    // 使用 fixture 創建測試數據
    EmployeeAddForm form = EmployeeTestFixture.createAddForm();

    ResponseDTO<String> result = employeeService.add(form);

    assertThat(result.getOk()).isTrue();
}

@Test
void update_ExistingEntity_Updates() {
    // 先創建實體
    EmployeeEntity entity = EmployeeTestFixture.createEntity();
    employeeDao.insert(entity);

    // 創建更新表單
    EmployeeUpdateForm form = EmployeeTestFixture.createUpdateForm(entity.getEmployeeId());

    ResponseDTO<String> result = employeeService.update(form);

    assertThat(result.getOk()).isTrue();
}
```

## 客製化方法

```java
// 帶特定部門的員工
public static EmployeeEntity createEntityWithDepartment(Long departmentId) {
    EmployeeEntity entity = createEntity();
    entity.setDepartmentId(departmentId);
    return entity;
}

// 帶特定狀態的員工
public static EmployeeEntity createEntityWithStatus(EmployeeStatus status) {
    EmployeeEntity entity = createEntity();
    entity.setStatus(status);
    return entity;
}

// 批量創建
public static List<EmployeeEntity> createEntities(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> createEntity())
        .collect(Collectors.toList());
}
```
