# SmartAdmin CRUD Generator - Examples

## 範例 1: 基本 CRUD 生成

**User Request:**
```
生成 Employee CRUD 模塊，包含 firstName, lastName, email, departmentId 欄位
```

**AI Actions:**
1. 創建 `EmployeeEntity.java`
2. 創建 `EmployeeDao.java`
3. 創建 `EmployeeManager.java` (含 @Transactional)
4. 創建 `EmployeeService.java` (含 Vavr Option)
5. 創建 `EmployeeController.java`
6. 創建 Form/VO 類
7. 運行 `./gradlew :smartadmin-app:test --tests ArchitectureTest`

## 範例 2: Entity 生成

```java
@Data
@TableName("t_employee")
public class EmployeeEntity {

    @TableId(type = IdType.AUTO)
    private Long employeeId;

    private String firstName;

    private String lastName;

    private String email;

    private Long departmentId;

    private Boolean deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

## 範例 3: Service 層 (Vavr)

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;

    public Option<EmployeeVO> getById(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .map(entity -> SmartBeanUtil.copy(entity, EmployeeVO.class));
    }

    public ResponseDTO<String> add(EmployeeAddForm form) {
        // 唯一性檢查
        EmployeeEntity existing = employeeDao.getByEmail(form.getEmail());
        if (existing != null) {
            return ResponseDTO.userErrorParam("Email 已存在");
        }

        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.insert(entity);
        return ResponseDTO.ok();
    }
}
```

## 範例 4: Controller 層

```java
@RestController
@RequestMapping("/admin/employee")
@RequiredArgsConstructor
@Tag(name = AdminSwaggerTagConst.Business.MANAGER_EMPLOYEE)
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/query")
    @Operation(summary = "分頁查詢員工")
    public ResponseDTO<PageResult<EmployeeVO>> query(@RequestBody EmployeeQueryForm form) {
        return employeeService.query(form);
    }

    @PostMapping("/add")
    @Operation(summary = "新增員工")
    @SaCheckPermission("employee:add")
    public ResponseDTO<String> add(@RequestBody @Valid EmployeeAddForm form) {
        return employeeService.add(form);
    }
}
```

## 範例 5: Vue 前端組件

```vue
<!-- employee-list.vue -->
<template>
  <div class="page-container">
    <a-card>
      <a-form layout="inline" :model="queryForm">
        <a-form-item label="姓名">
          <a-input v-model:value="queryForm.name" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">查詢</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-table
      :dataSource="tableData"
      :columns="columns"
      :pagination="pagination"
      @change="handleTableChange"
    />

    <EmployeeFormModal ref="formModal" @success="search" />
  </div>
</template>
```

## 常見問題

### Q1: ArchUnit 測試失敗 - Service 使用 Optional

**錯誤:** `Service layer should use Vavr Option instead of java.util.Optional`

**解決:**
```java
// ❌ 錯誤
import java.util.Optional;
public Optional<EmployeeVO> getById(Long id) { ... }

// ✅ 正確
import io.vavr.control.Option;
public Option<EmployeeVO> getById(Long id) { ... }
```

### Q2: SmartBeanUtil 找不到

**錯誤:** `Cannot resolve symbol 'SmartBeanUtil'`

**解決:**
```java
// v4.1.0 正確 import
import net.lab1024.sa.common.core.util.SmartBeanUtil;
```
