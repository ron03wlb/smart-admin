# Documentation Patterns - SmartAdmin Reference

Extracted from `documentation-engineer.md` for reuse across agents and skills.

---

## Transaction Management Documentation Pattern

**Manager Layer Transaction Pattern:**
```markdown
## Transaction Management

SmartAdmin requires @Transactional ONLY in Manager layer (enforced by ArchitectureTest):

**Correct Pattern:**
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final EmployeeRoleDao employeeRoleDao;

    @Transactional(rollbackFor = Throwable.class)
    public void updateWithRoles(EmployeeEntity employee, List<Long> roleIds) {
        // 1. Update employee
        employeeDao.updateById(employee);

        // 2. Delete old role associations
        employeeRoleDao.deleteByEmployeeId(employee.getEmployeeId());

        // 3. Insert new role associations
        List<EmployeeRoleEntity> roleRelations = roleIds.stream()
            .map(roleId -> new EmployeeRoleEntity(employee.getEmployeeId(), roleId))
            .toList();
        employeeRoleDao.insertBatch(roleRelations);

        // If any operation fails, ALL operations rollback
    }
}
```

**Service Layer (No @Transactional):**

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;

    public ResponseDTO<Void> updateEmployeeRoles(EmployeeUpdateForm form, List<Long> roleIds) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);

        // Call Manager layer which handles transaction
        employeeManager.updateWithRoles(entity, roleIds);

        return ResponseDTO.ok();
    }
}
```

**Rules:**
- Use `rollbackFor = Throwable.class` (NOT `Exception.class`)
- Only in Manager layer
- Service orchestrates, Manager handles transactions
- Test with ArchitectureTest to validate
```

---

## Manager Layer Caching Documentation Pattern

**Manager Layer Caching Pattern:**
```markdown
## Manager Layer Caching

SmartAdmin uses Redisson for distributed caching in the Manager layer:

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {

    @Cacheable(value = "employee", key = "#id")
    public EmployeeEntity getById(Long id) {
        return employeeDao.selectById(id);
    }

    @CacheEvict(value = "employee", key = "#entity.employeeId")
    public void update(EmployeeEntity entity) {
        employeeDao.updateById(entity);
    }

    @CacheEvict(value = "employee", allEntries = true)
    public void deleteById(Long id) {
        employeeDao.deleteById(id);
    }
}
```

**Cache Configuration:**
- Cache Name: `employee`
- TTL: 1 hour (default, configurable in application.yml)
- Storage: Redis (Redisson)
- Eviction: LRU

**Cache Keys:**
- Format: `employee::{id}`
- Example: `employee::123`

**When Cache is Evicted:**
- Manual eviction: `@CacheEvict` on update/delete
- TTL expiration: After configured time (default 1 hour)
- Memory pressure: LRU eviction
- All entries: `@CacheEvict(allEntries = true)` on list changes
```

---

## Common Documentation Scenarios

### Scenario 1: Document New SmartAdmin REST API

**Context:** java-architect just implemented new EmployeeController with CRUD operations

**Your Task:** Create comprehensive API documentation

**Output:**

1. **API Reference Document:**
```markdown
# Employee Management API

## Add Employee

**Endpoint:** `POST /employee/add`

**Description:** Create a new employee record in the system

**Request Body:** `EmployeeAddForm`
```json
{
  "name": "张三",
  "phone": "13800138000",
  "email": "zhangsan@company.com",
  "departmentId": 10,
  "positionId": 5,
  "gender": 1
}
```

**Response:** `ResponseDTO<Long>`
```json
{
  "code": 1,
  "success": true,
  "data": 123,
  "msg": "success"
}
```

**Permissions Required:** `system:employee:add`

**Error Codes:**
- `10002`: Invalid parameters (name, phone, email format validation failed)
- `30002`: Department not found
- `30003`: Position not found
- `30004`: Duplicate phone number

**Swagger Annotation:**
```java
@ApiOperation(value = "Add employee", notes = "Create new employee record with validation")
@ApiImplicitParams({
    @ApiImplicitParam(name = "name", value = "Employee name", required = true, dataType = "string"),
    @ApiImplicitParam(name = "phone", value = "Phone number", required = true, dataType = "string"),
    @ApiImplicitParam(name = "email", value = "Email address", required = true, dataType = "string")
})
```

**Example cURL:**
```bash
curl -X POST http://localhost:1024/employee/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "张三",
    "phone": "13800138000",
    "email": "zhangsan@company.com",
    "departmentId": 10,
    "positionId": 5,
    "gender": 1
  }'
```
```

### Scenario 2: Document Manager Layer Transaction Pattern

**Context:** User asks "How do we handle transactions in SmartAdmin?"

**Your Task:** Document transaction management rules and patterns

**Output:**
```markdown
# Transaction Management in SmartAdmin

## Overview

SmartAdmin enforces transaction management ONLY in the Manager layer. This is validated by ArchitectureTest and will fail the build if violated.

## Rules

1. **Location:** `@Transactional` ONLY in Manager layer classes
2. **Rollback:** Always use `rollbackFor = Throwable.class` (NOT `Exception.class`)
3. **Scope:** Manager methods that modify multiple entities or require atomicity
4. **Testing:** ArchitectureTest validates this rule automatically

## Pattern Example

**Correct: Transaction in Manager Layer**

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final EmployeeRoleDao employeeRoleDao;

    @Transactional(rollbackFor = Throwable.class)
    public void updateWithRoles(EmployeeEntity employee, List<Long> roleIds) {
        // 1. Update employee
        employeeDao.updateById(employee);

        // 2. Delete old role associations
        employeeRoleDao.deleteByEmployeeId(employee.getEmployeeId());

        // 3. Insert new role associations
        List<EmployeeRoleEntity> roleRelations = roleIds.stream()
            .map(roleId -> new EmployeeRoleEntity(employee.getEmployeeId(), roleId))
            .toList();
        employeeRoleDao.insertBatch(roleRelations);

        // If any operation fails, ALL operations rollback
    }
}
```

**Service Layer (No @Transactional):**

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;

    public ResponseDTO<Void> updateEmployeeRoles(EmployeeUpdateForm form, List<Long> roleIds) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);

        // Call Manager layer which handles transaction
        employeeManager.updateWithRoles(entity, roleIds);

        return ResponseDTO.ok();
    }
}
```

## Why Manager Layer Only?

1. **Separation of Concerns:** Service orchestrates business logic, Manager handles data consistency
2. **Testability:** Easier to test transactional behavior in isolation
3. **Performance:** Finer-grained transaction control, shorter transaction spans
4. **Architecture Enforcement:** ArchitectureTest catches violations automatically

## Common Mistakes

**Wrong: Transaction in Service**
```java
@Service
public class EmployeeService {
    @Transactional  // ArchitectureTest will FAIL
    public ResponseDTO<Void> update(EmployeeUpdateForm form) {
        // ...
    }
}
```

**Wrong: Using Exception.class**
```java
@Transactional(rollbackFor = Exception.class)  // Should be Throwable.class
public void save(EmployeeEntity entity) {
    // ...
}
```

**Correct:**
```java
@Service  // Manager layer
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)
    public void save(EmployeeEntity entity) {
        // ...
    }
}
```

## Testing

Run ArchitectureTest to validate:
```bash
./gradlew :smartadmin-app:test --tests ArchitectureTest
```

If violations exist, build will fail with clear error message pointing to the violating class.
```

### Scenario 3: Document Vue Component API Integration

**Context:** vue-expert implemented employee-list.vue, needs documentation

**Your Task:** Document API integration patterns

**Output:**
```markdown
# Employee List Component - API Integration Guide

## Component Overview

**File:** `src/views/business/employee/employee-list.vue`

**Purpose:** Display and manage employee records with pagination, search, and CRUD operations

## API Integration

### 1. Import API Module

```typescript
import { employeeApi } from '@/api/employee-api';
import type { EmployeeQueryForm, EmployeeVO } from '@/api/employee-types';
```

### 2. Query Employee List with Pagination

```typescript
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const tableData = ref<EmployeeVO[]>([]);

const queryData = async () => {
  const params: EmployeeQueryForm = {
    pageNum: page.value,
    pageSize: pageSize.value,
    departmentId: searchForm.departmentId,
    name: searchForm.name
  };

  const response = await employeeApi.query(params);

  // Always check success flag
  if (response.success) {
    tableData.value = response.data.list;
    total.value = response.data.total;
  } else {
    message.error(response.msg);
  }
};
```

### 3. Handle ResponseModel

SmartAdmin APIs return `ResponseDTO<T>`. Always check `success` flag:

```typescript
// Correct
const response = await employeeApi.add(form);
if (response.success) {
  message.success('添加成功');
  queryData();  // Refresh list
} else {
  message.error(response.msg);  // Show error message
}

// Wrong - doesn't check success
const response = await employeeApi.add(form);
message.success('添加成功');  // Will show even if failed
```

### 4. Permission Controls

Use `v-privilege` directive to show/hide buttons based on user permissions:

```vue
<template>
  <!-- Add button - only visible if user has permission -->
  <a-button
    v-privilege="'system:employee:add'"
    type="primary"
    @click="showAddModal">
    <PlusOutlined /> 新增员工
  </a-button>

  <!-- Action buttons in table -->
  <a-space>
    <a-button
      v-privilege="'system:employee:update'"
      size="small"
      @click="showEditModal(record)">
      编辑
    </a-button>

    <a-popconfirm
      title="确定删除吗?"
      @confirm="handleDelete(record.employeeId)">
      <a-button
        v-privilege="'system:employee:delete'"
        size="small"
        danger>
        删除
      </a-button>
    </a-popconfirm>
  </a-space>
</template>
```

### 5. Error Handling

Handle different error scenarios:

```typescript
const handleDelete = async (id: number) => {
  try {
    const response = await employeeApi.delete(id);

    if (response.success) {
      message.success('删除成功');
      queryData();
    } else {
      // Backend returned error
      message.error(response.msg);
    }
  } catch (error) {
    // Network error or unexpected exception
    message.error('删除失败，请稍后重试');
    console.error('Delete error:', error);
  }
};
```

## Permission Alignment Checklist

Ensure frontend permissions match backend:

| Operation | Backend Permission | Frontend v-privilege | Status |
|-----------|-------------------|---------------------|--------|
| Query list | `@SaCheckPermission("system:employee:query")` | `v-privilege="'system:employee:query'"` | Match |
| Add employee | `@SaCheckPermission("system:employee:add")` | `v-privilege="'system:employee:add'"` | Match |
| Update employee | `@SaCheckPermission("system:employee:update")` | `v-privilege="'system:employee:update'"` | Match |
| Delete employee | `@SaCheckPermission("system:employee:delete")` | `v-privilege="'system:employee:delete'"` | Match |

## Testing

**Unit Test:**
```typescript
import { mount } from '@vue/test-utils';
import EmployeeList from './employee-list.vue';

describe('EmployeeList API Integration', () => {
  it('should handle successful query', async () => {
    const mockResponse = {
      success: true,
      data: {
        list: [{ employeeId: 1, name: '张三' }],
        total: 1
      }
    };

    vi.spyOn(employeeApi, 'query').mockResolvedValue(mockResponse);

    const wrapper = mount(EmployeeList);
    await wrapper.vm.queryData();

    expect(wrapper.vm.tableData).toHaveLength(1);
    expect(wrapper.vm.total).toBe(1);
  });

  it('should handle error response', async () => {
    const mockResponse = {
      success: false,
      msg: '查询失败'
    };

    vi.spyOn(employeeApi, 'query').mockResolvedValue(mockResponse);
    const messageSpy = vi.spyOn(message, 'error');

    const wrapper = mount(EmployeeList);
    await wrapper.vm.queryData();

    expect(messageSpy).toHaveBeenCalledWith('查询失败');
  });
});
```
```

### Scenario 4: Document Permission System Alignment

**Context:** User asks "How do I ensure frontend and backend permissions align?"

**Your Task:** Create alignment guide with validation checklist

**Output:** (See "Permission System Documentation" section in documentation-engineer agent)

### Scenario 5: Document Pagination Pattern

**Context:** User asks "How does pagination work in SmartAdmin?"

**Your Task:** Document end-to-end pagination pattern

**Output:**
```markdown
# Pagination in SmartAdmin

## Overview

SmartAdmin uses `SmartPageUtil` for consistent pagination across backend and frontend.

## Backend Implementation

### 1. QueryForm with PageParam

```java
@Data
public class EmployeeQueryForm extends PageParam {
    @ApiModelProperty("部门ID")
    private Long departmentId;

    @ApiModelProperty("员工姓名")
    private String name;

    // Inherits from PageParam:
    // - pageNum (page number, starting from 1)
    // - pageSize (records per page)
    // - sortCode (sort field)
    // - sortType (ASC/DESC)
}
```

### 2. Service Layer

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;

    public ResponseDTO<PageResult<EmployeeVO>> query(EmployeeQueryForm form) {
        // Convert QueryForm to MyBatis Plus Page
        Page<?> page = SmartPageUtil.convert2PageQuery(form);

        // Build query wrapper
        LambdaQueryWrapper<EmployeeEntity> wrapper = Wrappers.<EmployeeEntity>lambdaQuery()
            .eq(form.getDepartmentId() != null, EmployeeEntity::getDepartmentId, form.getDepartmentId())
            .like(StringUtils.isNotBlank(form.getName()), EmployeeEntity::getName, form.getName())
            .orderByDesc(EmployeeEntity::getCreateTime);

        // Execute query with pagination
        List<EmployeeEntity> list = employeeDao.selectList(page, wrapper);

        // Convert to PageResult<VO>
        PageResult<EmployeeVO> pageResult = SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);

        return ResponseDTO.ok(pageResult);
    }
}
```

### 3. Response Format

```json
{
  "code": 1,
  "success": true,
  "data": {
    "list": [
      {
        "employeeId": 1,
        "name": "张三",
        "departmentName": "研发部"
      }
    ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

## Frontend Implementation

### 1. Request

```typescript
const queryData = async () => {
  const params = {
    pageNum: page.value,
    pageSize: pageSize.value,
    departmentId: searchForm.departmentId,
    name: searchForm.name
  };

  const response = await employeeApi.query(params);

  if (response.success) {
    tableData.value = response.data.list;
    total.value = response.data.total;
  }
};
```

### 2. Table with Pagination

```vue
<template>
  <a-table
    :dataSource="tableData"
    :pagination="{
      current: page,
      pageSize: pageSize,
      total: total,
      showTotal: (total) => `共 ${total} 条`,
      showSizeChanger: true,
      pageSizeOptions: ['10', '20', '50', '100']
    }"
    @change="handleTableChange">
    <!-- columns -->
  </a-table>
</template>

<script setup lang="ts">
const handleTableChange = (pagination: any) => {
  page.value = pagination.current;
  pageSize.value = pagination.pageSize;
  queryData();
};
</script>
```

## Complete Example

**Backend:**
```java
@RestController
@RequiredArgsConstructor
public class EmployeeController {

    @PostMapping("/employee/query")
    @SaCheckPermission("system:employee:query")
    public ResponseDTO<PageResult<EmployeeVO>> query(@RequestBody @Valid EmployeeQueryForm form) {
        return employeeService.query(form);
    }
}

@Service
@RequiredArgsConstructor
public class EmployeeService {
    public ResponseDTO<PageResult<EmployeeVO>> query(EmployeeQueryForm form) {
        Page<?> page = SmartPageUtil.convert2PageQuery(form);
        List<EmployeeEntity> list = employeeDao.selectList(page, wrapper);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class));
    }
}
```

**Frontend:**
```vue
<script setup lang="ts">
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const tableData = ref<EmployeeVO[]>([]);

const queryData = async () => {
  const response = await employeeApi.query({
    pageNum: page.value,
    pageSize: pageSize.value,
    name: searchForm.name
  });

  if (response.success) {
    tableData.value = response.data.list;
    total.value = response.data.total;
  }
};

onMounted(() => {
  queryData();
});
</script>
```
```
