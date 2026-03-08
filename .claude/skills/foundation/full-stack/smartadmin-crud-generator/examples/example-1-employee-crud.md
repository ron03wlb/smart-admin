# 範例 1: 員工管理 CRUD 生成

**技能**: smartadmin-crud-generator
**難度**: ⭐⭐⭐⭐☆（高等）
**預估時間**: 30-45 分鐘（完整 CRUD）

---

## 場景描述

為 SmartAdmin 生成完整的員工管理 CRUD功能，包含：
- ✅ 後端: Controller → Service → Dao → Entity
- ✅ 前端: Vue 3 列表頁 + 表單彈窗
- ✅ API 文檔: Knife4j/Swagger
- ✅ 測試: 單元測試 + 整合測試

---

## 輸入（數據模型）

**需求**:
- 表名: `t_employee`
- 欄位: 員工ID、姓名、部門、職位、入職日期、狀態

**數據庫 Schema**:
```sql
CREATE TABLE t_employee (
    employee_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '員工ID',
    employee_name VARCHAR(50) NOT NULL COMMENT '姓名',
    department_id BIGINT NOT NULL COMMENT '部門ID',
    position VARCHAR(50) COMMENT '職位',
    join_date DATE NOT NULL COMMENT '入職日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '狀態(1:在職 2:離職)',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '刪除標記',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='員工表';
```

---

## 生成的代碼結構

```
smartadmin-modules/smartadmin-business/src/main/java/net/lab1024/sa/business/employee/
├── controller/
│   └── EmployeeController.java        # REST API 端點
├── service/
│   └── EmployeeService.java           # 業務邏輯
├── dao/
│   └── EmployeeDao.java                # MyBatis Mapper
├── domain/
│   ├── entity/
│   │   └── EmployeeEntity.java        # 數據實體
│   ├── form/
│   │   ├── EmployeeAddForm.java       # 新增表單
│   │   ├── EmployeeUpdateForm.java    # 更新表單
│   │   └── EmployeeQueryForm.java     # 查詢表單
│   └── vo/
│       └── EmployeeVO.java            # 視圖對象

smartadmin-modules/smartadmin-business/src/main/resources/mapper/business/employee/
└── EmployeeMapper.xml                  # MyBatis XML

smartadmin-app/src/test/java/net/lab1024/sa/app/business/employee/
├── EmployeeServiceTest.java            # 單元測試
└── EmployeeControllerTest.java         # 整合測試
```

**前端代碼**:
```
smart-admin-web/src/views/business/employee/
├── employee-list.vue                   # 列表頁
├── employee-form.vue                   # 表單彈窗
└── employee-api.ts                     # API 請求
```

---

## 生成的核心代碼示例

### Controller (REST API)

```java
@RestController
@RequestMapping("/api/employee")
@Tag(name = "員工管理")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/query")
    @Operation(summary = "分頁查詢員工")
    public ResponseDTO<PageResult<EmployeeVO>> query(@Valid EmployeeQueryForm form) {
        return employeeService.query(form);
    }

    @PostMapping("/add")
    @Operation(summary = "新增員工")
    public ResponseDTO<Long> add(@Valid @RequestBody EmployeeAddForm form) {
        return employeeService.add(form);
    }

    @PostMapping("/update")
    @Operation(summary = "更新員工")
    public ResponseDTO<Void> update(@Valid @RequestBody EmployeeUpdateForm form) {
        return employeeService.update(form);
    }

    @GetMapping("/delete/{employeeId}")
    @Operation(summary = "刪除員工")
    public ResponseDTO<Void> delete(@PathVariable Long employeeId) {
        return employeeService.delete(employeeId);
    }
}
```

---

### Service (業務邏輯)

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;

    public ResponseDTO<PageResult<EmployeeVO>> query(EmployeeQueryForm form) {
        Page<EmployeeEntity> page = SmartPageUtil.convert2PageQuery(form);
        List<EmployeeEntity> list = employeeDao.query(page, form);

        PageResult<EmployeeVO> pageResult = SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
        return ResponseDTO.ok(pageResult);
    }

    public ResponseDTO<Long> add(EmployeeAddForm form) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.insert(entity);
        return ResponseDTO.ok(entity.getEmployeeId());
    }

    public ResponseDTO<Void> update(EmployeeUpdateForm form) {
        EmployeeEntity entity = employeeDao.selectById(form.getEmployeeId());
        if (entity == null) {
            return ResponseDTO.error(EmployeeErrorCode.DATA_NOT_EXIST);
        }

        SmartBeanUtil.copyProperties(form, entity);
        employeeDao.updateById(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<Void> delete(Long employeeId) {
        employeeDao.deleteById(employeeId);
        return ResponseDTO.ok();
    }
}
```

---

### Entity (數據實體)

```java
@Data
@TableName("t_employee")
public class EmployeeEntity {

    @TableId(type = IdType.AUTO)
    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String position;

    private LocalDate joinDate;

    /**
     * 狀態(1:在職 2:離職)
     */
    private Integer status;

    @TableLogic
    private Boolean deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

---

## 前端代碼示例

### employee-list.vue (列表頁)

```vue
<template>
  <a-card>
    <!-- 查詢表單 -->
    <a-form :model="queryForm" layout="inline">
      <a-form-item label="姓名">
        <a-input v-model:value="queryForm.employeeName" placeholder="請輸入姓名" />
      </a-form-item>
      <a-form-item label="狀態">
        <a-select v-model:value="queryForm.status" placeholder="請選擇狀態">
          <a-select-option :value="1">在職</a-select-option>
          <a-select-option :value="2">離職</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="query">查詢</a-button>
        <a-button @click="reset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 操作按鈕 -->
    <div class="table-operator">
      <a-button type="primary" @click="add">新增員工</a-button>
    </div>

    <!-- 數據表格 -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :pagination="pagination"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="edit(record)">編輯</a-button>
            <a-popconfirm title="確定刪除?" @confirm="deleteRow(record)">
              <a-button size="small" danger>刪除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 表單彈窗 -->
    <employee-form
      v-model:visible="formVisible"
      :form-data="formData"
      @success="query"
    />
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { employeeApi } from './employee-api';
import EmployeeForm from './employee-form.vue';

const queryForm = ref({
  employeeName: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10,
});

const tableData = ref([]);
const pagination = ref({ current: 1, pageSize: 10, total: 0 });

async function query() {
  const res = await employeeApi.query(queryForm.value);
  if (res.ok) {
    tableData.value = res.data.list;
    pagination.value.total = res.data.total;
  }
}

// ... 其他方法
</script>
```

---

## 執行結果

**API 端點**:
- `GET /api/employee/query` - 分頁查詢
- `POST /api/employee/add` - 新增員工
- `POST /api/employee/update` - 更新員工
- `GET /api/employee/delete/{id}` - 刪除員工

**Knife4j 文檔**: `http://localhost:1024/doc.html#/default/員工管理`

**前端頁面**: `http://localhost:5173/business/employee`

---

## 時間節省

| 任務 | 手動開發 | CRUD 生成器 | 節省 |
|------|---------|-------------|------|
| 後端 CRUD | 4 小時 | 15 分鐘 | 93.75% |
| 前端頁面 | 3 小時 | 10 分鐘 | 94.44% |
| 測試代碼 | 2 小時 | 5 分鐘 | 95.83% |
| **總計** | **9 小時** | **30 分鐘** | **94.44%** |

---

## 相關規則

- **[Architecture Rules](../../../../../.agent/rules/foundation/F04-architecture-rules.md)**
  - Controller → Service → Dao 分層架構
  - ResponseDTO 統一響應格式
  - 布林欄位命名: `deleted` 不是 `isDeleted`

- **[Naming Conventions](../../../../../.agent/rules/foundation/F01-naming-conventions.md)**
  - 類別命名: `EmployeeController`, `EmployeeService`, `EmployeeDao`

---

## 參考資料

- [smartadmin-crud-generator SKILL.md](../SKILL.md)
- [SmartAdmin Patterns](../../../../../.claude/shared/knowledge/smartadmin-patterns.md)
