# Example 1: Complete CRUD Module Generation (--all-phases)

**Scenario**: Create a complete Employee management module from scratch with backend, frontend, tests, and documentation.

**Time**: ~20 minutes
**Mode**: --all-phases

---

## User Request

```
User: "Create an Employee CRUD module with the following fields:
- Employee name (required, max 100 chars)
- Email (required, unique, email format)
- Phone number (optional, 11 digits)
- Department ID (required, foreign key to Department)
- Job title (required, max 50 chars)
- Status (required, enum: 1=Active, 2=Inactive)
- Hire date (required)
- Salary (optional, decimal)"
```

---

## Step 1: Requirements Confirmation (2 minutes)

**AI Response**:
```
Let me confirm the Employee module requirements:

**Entity**: Employee
**Module**: system (admin management)
**Table**: t_employee

**Fields**:
1. employeeId: Long (primary key, auto-increment)
2. employeeName: String (required, max 100 chars)
3. email: String (required, unique, email validation)
4. phoneNumber: String (optional, 11 digits, pattern validation)
5. departmentId: Long (required, FK to Department)
6. jobTitle: String (required, max 50 chars)
7. status: Integer (required, enum: 1=Active, 2=Inactive, default 1)
8. hireDate: LocalDate (required)
9. salary: BigDecimal (optional, positive, scale 2)
10. deletedFlag: Boolean (soft delete, default false)
11. createTime: LocalDateTime (auto)
12. updateTime: LocalDateTime (auto)

**Unique Constraints**: email
**Foreign Keys**: departmentId → t_department(department_id)

**CRUD Operations**:
- Query (pagination, search by name/email)
- Add (validate email uniqueness)
- Update (validate email uniqueness, exclude self)
- Delete (soft delete)
- Get by ID
- Batch delete

Proceed with generation?
```

**User**: "Yes, proceed"

---

## Step 2: Phase 1 - Backend Generation (8 minutes)

### 2.1 Entity

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/employee/domain/entity/EmployeeEntity.java`

```java
package net.lab1024.sa.admin.module.system.employee.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee Entity
 *
 * @author SmartAdmin Generator
 * @date 2026-01-27
 */
@Data
@TableName("t_employee")
public class EmployeeEntity {

    /**
     * Employee ID
     */
    @TableId(type = IdType.AUTO)
    private Long employeeId;

    /**
     * Employee name
     */
    private String employeeName;

    /**
     * Email
     */
    private String email;

    /**
     * Phone number
     */
    private String phoneNumber;

    /**
     * Department ID
     */
    private Long departmentId;

    /**
     * Job title
     */
    private String jobTitle;

    /**
     * Status (1: Active, 2: Inactive)
     */
    private Integer status;

    /**
     * Hire date
     */
    private LocalDate hireDate;

    /**
     * Salary
     */
    private BigDecimal salary;

    /**
     * Deleted flag (false: not deleted, true: deleted)
     */
    @TableLogic
    private Boolean deletedFlag;

    /**
     * Create time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;
}
```

### 2.2 Domain Objects

**QueryForm**:
```java
@Data
@EqualsAndHashCode(callSuper = false)
public class EmployeeQueryForm extends PageParam {
    @Schema(description = "Search keyword (matches name or email)", example = "John")
    private String keyword;

    @Schema(description = "Department ID filter", example = "1")
    private Long departmentId;

    @Schema(description = "Status filter (1: Active, 2: Inactive)", example = "1")
    private Integer status;

    @Schema(description = "Hire date start", example = "2024-01-01")
    private LocalDate hireDateBegin;

    @Schema(description = "Hire date end", example = "2024-12-31")
    private LocalDate hireDateEnd;
}
```

**AddForm**:
```java
@Data
public class EmployeeAddForm {
    @Schema(description = "Employee name", required = true, example = "John Doe")
    @NotBlank(message = "Employee name cannot be empty")
    @Size(max = 100, message = "Employee name cannot exceed 100 characters")
    private String employeeName;

    @Schema(description = "Email", required = true, example = "john.doe@example.com")
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Phone number (11 digits)", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Schema(description = "Department ID", required = true, example = "1")
    @NotNull(message = "Department ID cannot be null")
    private Long departmentId;

    @Schema(description = "Job title", required = true, example = "Software Engineer")
    @NotBlank(message = "Job title cannot be empty")
    @Size(max = 50, message = "Job title cannot exceed 50 characters")
    private String jobTitle;

    @Schema(description = "Status (1: Active, 2: Inactive)", required = true, example = "1")
    @NotNull(message = "Status cannot be null")
    private Integer status;

    @Schema(description = "Hire date", required = true, example = "2024-01-01")
    @NotNull(message = "Hire date cannot be null")
    private LocalDate hireDate;

    @Schema(description = "Salary (positive, 2 decimal places)", example = "50000.00")
    @DecimalMin(value = "0.0", message = "Salary must be positive")
    @Digits(integer = 10, fraction = 2, message = "Salary must have max 2 decimal places")
    private BigDecimal salary;
}
```

**UpdateForm** (includes employeeId + all AddForm fields)

**VO**:
```java
@Data
public class EmployeeVO {
    private Long employeeId;
    private String employeeName;
    private String email;
    private String phoneNumber;
    private Long departmentId;
    private String departmentName;  // From JOIN
    private String jobTitle;
    private Integer status;
    private LocalDate hireDate;
    private BigDecimal salary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 2.3 Dao Layer

**File**: `EmployeeDao.java`
```java
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {

    /**
     * Query with pagination
     */
    List<EmployeeVO> query(
        @Param("page") Page page,
        @Param("queryForm") EmployeeQueryForm queryForm
    );

    /**
     * Query by ID
     */
    EmployeeVO queryById(@Param("employeeId") Long employeeId);

    /**
     * Check email uniqueness
     */
    EmployeeEntity getByEmail(
        @Param("email") String email,
        @Param("excludeId") Long excludeId
    );
}
```

**File**: `EmployeeDao.xml`
```xml
<select id="query" resultType="net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO">
    SELECT
        e.employee_id,
        e.employee_name,
        e.email,
        e.phone_number,
        e.department_id,
        d.department_name,
        e.job_title,
        e.status,
        e.hire_date,
        e.salary,
        e.create_time,
        e.update_time
    FROM t_employee e
    LEFT JOIN t_department d ON e.department_id = d.department_id
    <where>
        e.deleted_flag = false
        <if test="queryForm.keyword != null and queryForm.keyword != ''">
            AND (e.employee_name LIKE CONCAT('%', #{queryForm.keyword}, '%')
                 OR e.email LIKE CONCAT('%', #{queryForm.keyword}, '%'))
        </if>
        <if test="queryForm.departmentId != null">
            AND e.department_id = #{queryForm.departmentId}
        </if>
        <if test="queryForm.status != null">
            AND e.status = #{queryForm.status}
        </if>
        <if test="queryForm.hireDateBegin != null">
            AND e.hire_date &gt;= #{queryForm.hireDateBegin}
        </if>
        <if test="queryForm.hireDateEnd != null">
            AND e.hire_date &lt;= #{queryForm.hireDateEnd}
        </if>
    </where>
    ORDER BY e.create_time DESC
</select>
```

### 2.4 Manager Layer

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeManager {

    private final EmployeeDao employeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void add(EmployeeEntity entity) {
        employeeDao.insert(entity);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void update(EmployeeEntity entity) {
        employeeDao.updateById(entity);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void delete(Long employeeId) {
        employeeDao.deleteById(employeeId);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void batchDelete(List<Long> employeeIdList) {
        employeeDao.deleteBatchIds(employeeIdList);
    }
}
```

### 2.5 Service Layer

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;

    public ResponseDTO<PageResult<EmployeeVO>> query(EmployeeQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<EmployeeVO> list = employeeDao.query(page, queryForm);
        PageResult<EmployeeVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
        return ResponseDTO.ok(pageResult);
    }

    public ResponseDTO<EmployeeVO> getById(Long employeeId) {
        return Option.of(employeeDao.queryById(employeeId))
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.userErrorParam("Employee not found"));
    }

    public ResponseDTO<Void> add(EmployeeAddForm addForm) {
        // Validate email uniqueness
        EmployeeEntity existing = employeeDao.getByEmail(addForm.getEmail(), null);
        if (existing != null) {
            return ResponseDTO.userErrorParam("Email already exists");
        }

        EmployeeEntity entity = SmartBeanUtil.copy(addForm, EmployeeEntity.class);
        employeeManager.add(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<Void> update(EmployeeUpdateForm updateForm) {
        return Option.of(employeeDao.selectById(updateForm.getEmployeeId()))
            .map(existingEntity -> {
                // Validate email uniqueness (exclude self)
                EmployeeEntity emailCheck = employeeDao.getByEmail(
                    updateForm.getEmail(),
                    updateForm.getEmployeeId()
                );
                if (emailCheck != null) {
                    return ResponseDTO.userErrorParam("Email already exists");
                }

                EmployeeEntity entity = SmartBeanUtil.copy(updateForm, EmployeeEntity.class);
                employeeManager.update(entity);
                return ResponseDTO.ok();
            })
            .getOrElse(() -> ResponseDTO.userErrorParam("Employee not found"));
    }

    public ResponseDTO<Void> delete(Long employeeId) {
        return Option.of(employeeDao.selectById(employeeId))
            .map(entity -> {
                employeeManager.delete(employeeId);
                return ResponseDTO.ok();
            })
            .getOrElse(() -> ResponseDTO.userErrorParam("Employee not found"));
    }

    public ResponseDTO<Void> batchDelete(EmployeeBatchDeleteForm batchDeleteForm) {
        employeeManager.batchDelete(batchDeleteForm.getEmployeeIdList());
        return ResponseDTO.ok();
    }
}
```

### 2.6 Controller Layer

```java
@RestController
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/system/employee/query")
    @SaCheckPermission("system:employee:query")
    public ResponseDTO<PageResult<EmployeeVO>> query(@RequestBody @Valid EmployeeQueryForm queryForm) {
        return employeeService.query(queryForm);
    }

    @GetMapping("/system/employee/get/{employeeId}")
    @SaCheckPermission("system:employee:query")
    public ResponseDTO<EmployeeVO> getById(@PathVariable Long employeeId) {
        return employeeService.getById(employeeId);
    }

    @PostMapping("/system/employee/add")
    @SaCheckPermission("system:employee:add")
    public ResponseDTO<Void> add(@RequestBody @Valid EmployeeAddForm addForm) {
        return employeeService.add(addForm);
    }

    @PostMapping("/system/employee/update")
    @SaCheckPermission("system:employee:update")
    public ResponseDTO<Void> update(@RequestBody @Valid EmployeeUpdateForm updateForm) {
        return employeeService.update(updateForm);
    }

    @PostMapping("/system/employee/delete/{employeeId}")
    @SaCheckPermission("system:employee:delete")
    public ResponseDTO<Void> delete(@PathVariable Long employeeId) {
        return employeeService.delete(employeeId);
    }

    @PostMapping("/system/employee/batchDelete")
    @SaCheckPermission("system:employee:delete")
    public ResponseDTO<Void> batchDelete(@RequestBody @Valid EmployeeBatchDeleteForm batchDeleteForm) {
        return employeeService.batchDelete(batchDeleteForm);
    }
}
```

**Phase 1 Complete** ✅ (~8 minutes)

---

## Step 3: Phase 2 - Frontend Generation (7 minutes)

### 3.1 TypeScript Types

**File**: `smart-admin-web/src/api/system/employee-types.ts`

```typescript
/**
 * Employee Types
 *
 * @author SmartAdmin Generator
 * @date 2026-01-27
 */

// Query Form
export interface EmployeeQueryForm {
  keyword?: string;
  departmentId?: number;
  status?: number;
  hireDateBegin?: string;
  hireDateEnd?: string;
  pageNum: number;
  pageSize: number;
}

// VO
export interface EmployeeVO {
  employeeId: number;
  employeeName: string;
  email: string;
  phoneNumber?: string;
  departmentId: number;
  departmentName: string;
  jobTitle: string;
  status: number;
  hireDate: string;
  salary?: number;
  createTime: string;
  updateTime: string;
}

// Add Form
export interface EmployeeAddForm {
  employeeName: string;
  email: string;
  phoneNumber?: string;
  departmentId: number;
  jobTitle: string;
  status: number;
  hireDate: string;
  salary?: number;
}

// Update Form
export interface EmployeeUpdateForm {
  employeeId: number;
  employeeName: string;
  email: string;
  phoneNumber?: string;
  departmentId: number;
  jobTitle: string;
  status: number;
  hireDate: string;
  salary?: number;
}

// Batch Delete Form
export interface EmployeeBatchDeleteForm {
  employeeIdList: number[];
}

// Status Enum
export const EmployeeStatusEnum = {
  ACTIVE: { value: 1, label: 'Active' },
  INACTIVE: { value: 2, label: 'Inactive' },
} as const;
```

### 3.2 API Client

**File**: `smart-admin-web/src/api/system/employee-api.ts`

```typescript
import { postRequest, getRequest } from '@/lib/axios';
import type {
  EmployeeQueryForm,
  EmployeeVO,
  EmployeeAddForm,
  EmployeeUpdateForm,
  EmployeeBatchDeleteForm,
} from './employee-types';

const BASE_URL = '/system/employee';

/**
 * Employee API
 */
export const employeeApi = {
  /**
   * Query with pagination
   */
  query: (queryForm: EmployeeQueryForm) => {
    return postRequest<PageResult<EmployeeVO>>(`${BASE_URL}/query`, queryForm);
  },

  /**
   * Get by ID
   */
  getById: (employeeId: number) => {
    return getRequest<EmployeeVO>(`${BASE_URL}/get/${employeeId}`);
  },

  /**
   * Add employee
   */
  add: (addForm: EmployeeAddForm) => {
    return postRequest<void>(`${BASE_URL}/add`, addForm);
  },

  /**
   * Update employee
   */
  update: (updateForm: EmployeeUpdateForm) => {
    return postRequest<void>(`${BASE_URL}/update`, updateForm);
  },

  /**
   * Delete employee
   */
  delete: (employeeId: number) => {
    return postRequest<void>(`${BASE_URL}/delete/${employeeId}`);
  },

  /**
   * Batch delete employees
   */
  batchDelete: (batchDeleteForm: EmployeeBatchDeleteForm) => {
    return postRequest<void>(`${BASE_URL}/batchDelete`, batchDeleteForm);
  },
};
```

### 3.3 List Component

**File**: `smart-admin-web/src/views/system/employee/employee-list.vue`

(648 lines - see [phase-2-frontend.md](../phases/phase-2-frontend.md) for complete pattern)

**Key Features**:
- a-table with pagination
- Search form (keyword, department, status, date range)
- Add/Edit/Delete/Batch Delete buttons with permission checks
- Department dropdown (loaded from API)
- Status badges (Active: green, Inactive: red)

### 3.4 Form Modal

**File**: `smart-admin-web/src/views/system/employee/employee-form-modal.vue`

(Complete form with all fields, validation rules, department selector, date picker)

**Phase 2 Complete** ✅ (~7 minutes)

---

## Step 4: Phase 3 - API Documentation (3 minutes)

Add Swagger annotations to existing classes:

**EmployeeController.java**:
```java
@RestController
@Tag(name = AdminSwaggerTagConst.System.MANAGER_EMPLOYEE)  // ← Added
@RequiredArgsConstructor
public class EmployeeController {

    @Operation(summary = "Query employees with pagination")  // ← Added
    @PostMapping("/system/employee/query")
    @SaCheckPermission("system:employee:query")
    public ResponseDTO<PageResult<EmployeeVO>> query(@RequestBody @Valid EmployeeQueryForm queryForm) {
        return employeeService.query(queryForm);
    }

    @Operation(summary = "Get employee by ID")  // ← Added
    @GetMapping("/system/employee/get/{employeeId}")
    @SaCheckPermission("system:employee:query")
    public ResponseDTO<EmployeeVO> getById(@PathVariable Long employeeId) {
        return employeeService.getById(employeeId);
    }

    // ... (all methods annotated)
}
```

**EmployeeAddForm.java** (add @Schema to all fields with examples)

**Phase 3 Complete** ✅ (~3 minutes)

---

## Step 5: Phase 4 - Integration Tests (5 minutes)

**File**: `EmployeeIntegrationTest.java`

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeDao employeeDao;

    private static Long testEmployeeId;

    @Test
    @Order(1)
    @DisplayName("Add employee - should succeed")
    @Transactional
    void testAdd() {
        EmployeeAddForm form = new EmployeeAddForm();
        form.setEmployeeName("Test Employee");
        form.setEmail("test@example.com");
        form.setPhoneNumber("13800138000");
        form.setDepartmentId(1L);
        form.setJobTitle("Software Engineer");
        form.setStatus(1);
        form.setHireDate(LocalDate.now());
        form.setSalary(new BigDecimal("50000.00"));

        ResponseDTO<Void> response = employeeService.add(form);
        assertTrue(response.getOk());

        // Verify database
        EmployeeEntity entity = employeeDao.getByEmail("test@example.com", null);
        assertNotNull(entity);
        assertEquals("Test Employee", entity.getEmployeeName());
        testEmployeeId = entity.getEmployeeId();
    }

    @Test
    @Order(2)
    @DisplayName("Query employees - should return results")
    @Transactional
    void testQuery() {
        EmployeeQueryForm queryForm = new EmployeeQueryForm();
        queryForm.setKeyword("Test");
        queryForm.setPageNum(1);
        queryForm.setPageSize(10);

        ResponseDTO<PageResult<EmployeeVO>> response = employeeService.query(queryForm);
        assertTrue(response.getOk());
        assertFalse(response.getData().getList().isEmpty());
    }

    // ... (more test methods)
}
```

**Phase 4 Complete** ✅ (~5 minutes)

---

## Step 6: Final Validation (2 minutes)

### Run ArchitectureTest
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected**: All tests pass ✅

### Run Integration Tests
```bash
./gradlew :sa-admin:test --tests EmployeeIntegrationTest
```

**Expected**: All tests pass ✅

### Verify Knife4j UI
1. Start application: `./gradlew :sa-admin:bootRun`
2. Visit: http://localhost:1024/doc.html
3. Navigate to: System → Employee Management
4. Verify all endpoints documented ✅

### Test Frontend
1. Start frontend: `cd smart-admin-web && npm run dev`
2. Login and navigate to: System → Employee Management
3. Test CRUD operations:
   - Add employee ✅
   - Search/filter ✅
   - Edit employee ✅
   - Delete employee ✅

---

## Summary

**Total Time**: ~20 minutes (vs 45 minutes with separate skills)

**Files Generated**:
- Backend: 11 files (Entity, Dao, Dao.xml, Manager, Service, Controller, 4 Forms, VO)
- Frontend: 4 files (types.ts, api.ts, list.vue, form-modal.vue)
- Tests: 1 file (EmployeeIntegrationTest.java)
- **Total: 16 files** + documentation annotations

**Features Delivered**:
- ✅ Complete CRUD operations
- ✅ Pagination and search
- ✅ Email uniqueness validation
- ✅ Department foreign key relationship
- ✅ Soft delete support
- ✅ Permission controls
- ✅ API documentation
- ✅ Integration tests

**Quality Assurance**:
- ✅ ArchitectureTest passes
- ✅ Integration tests pass
- ✅ Frontend-backend integration verified
- ✅ API documentation accessible in Knife4j

**Next Steps**:
1. Configure menu permissions in admin panel
2. Assign permissions to roles
3. Test with different user roles
4. Deploy to staging environment
