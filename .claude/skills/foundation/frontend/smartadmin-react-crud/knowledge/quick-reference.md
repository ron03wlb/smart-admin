# SmartAdmin React CRUD 快速參考

**版本**: 1.0.0
**技術棧**: React 19 + TypeScript + Ant Design 5 + Redux Toolkit + Vitest
**更新日期**: 2026-03-06

本文檔提供 SmartAdmin React 開發的核心模式、最佳實踐和完整範例。

---

## 目錄

1. [React 19 Hooks 模式](#react-19-hooks-模式)
2. [TypeScript 類型模式](#typescript-類型模式)
3. [Ant Design 5 組件速查](#ant-design-5-組件速查)
4. [權限控制模式](#權限控制模式)
5. [API 集成模式](#api-集成模式)
6. [測試模式](#測試模式)
7. [完整 CRUD 範例](#完整-crud-範例)
8. [Vue vs React 對照表](#vue-vs-react-對照表)
9. [常見問題解決](#常見問題解決)

---

## React 19 Hooks 模式

### 1. useState - 基礎狀態管理

```tsx
import { useState } from 'react';

// ✅ 正確：使用 TypeScript 類型推斷
const [count, setCount] = useState(0);
const [name, setName] = useState('');
const [visible, setVisible] = useState(false);

// ✅ 正確：明確指定類型（複雜對象）
const [user, setUser] = useState<UserVO | null>(null);
const [list, setList] = useState<EmployeeVO[]>([]);

// ✅ 正確：使用函數更新（基於前值）
setCount(prev => prev + 1);
setList(prev => [...prev, newItem]);

// ❌ 錯誤：直接修改狀態
list.push(newItem);  // 不會觸發重新渲染
setList(list);       // React 檢測不到變化
```

### 2. useEffect - 副作用管理

```tsx
import { useEffect } from 'react';

// ✅ 正確：組件掛載時獲取數據（依賴為空數組）
useEffect(() => {
  fetchData();
}, []);

// ✅ 正確：依賴變化時重新獲取數據
useEffect(() => {
  fetchData();
}, [queryForm]);  // queryForm 變化時執行

// ✅ 正確：清理函數（取消訂閱、清除定時器）
useEffect(() => {
  const timer = setInterval(() => {...}, 1000);
  return () => clearInterval(timer);  // 清理
}, []);

// ❌ 錯誤：缺少依賴（ESLint 會警告）
useEffect(() => {
  console.log(count);  // 使用了 count 但未列為依賴
}, []);  // 應該是 [count]
```

### 3. useCallback - 事件處理優化

```tsx
import { useCallback } from 'react';

// ✅ 正確：穩定的回調引用（傳遞給子組件）
const handleDelete = useCallback((id: number) => {
  api.delete(id).then(fetchData);
}, [fetchData]);

// ✅ 正確：依賴變化時重新創建
const handleQuery = useCallback(async () => {
  const response = await api.query(queryForm);
  setData(response.data);
}, [queryForm]);

// ❌ 錯誤：不必要的 useCallback（未傳遞給子組件）
const handleClick = useCallback(() => {
  console.log('clicked');
}, []);  // 浪費性能
```

### 4. useMemo - 計算優化

```tsx
import { useMemo } from 'react';

// ✅ 正確：昂貴計算的緩存
const filteredList = useMemo(() => {
  return list.filter(item => item.status === 1)
             .sort((a, b) => b.createTime.localeCompare(a.createTime));
}, [list]);

// ✅ 正確：對象/數組的穩定引用
const columns = useMemo(() => [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: 'Name', dataIndex: 'name', key: 'name' },
], []);

// ❌ 錯誤：簡單計算不需要 useMemo
const total = useMemo(() => a + b, [a, b]);  // 過度優化
```

### 5. useReducer - 複雜狀態管理

```tsx
import { useReducer } from 'react';

// State 和 Action 類型
type State = {
  data: EmployeeVO[];
  loading: boolean;
  error: string | null;
};

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: EmployeeVO[] }
  | { type: 'FETCH_ERROR'; payload: string };

// Reducer 函數
const reducer = (state: State, action: Action): State => {
  switch (action.type) {
    case 'FETCH_START':
      return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':
      return { ...state, loading: false, data: action.payload };
    case 'FETCH_ERROR':
      return { ...state, loading: false, error: action.payload };
    default:
      return state;
  }
};

// 使用
const [state, dispatch] = useReducer(reducer, {
  data: [],
  loading: false,
  error: null,
});

dispatch({ type: 'FETCH_START' });
dispatch({ type: 'FETCH_SUCCESS', payload: employees });
```

### 6. Custom Hooks - 可重用邏輯

```tsx
// useEmployeeCrud.ts
import { useState, useCallback } from 'react';
import { message } from 'antd';
import { employeeApi } from '@/api/system/employee-api';
import type { EmployeeVO, EmployeeQueryForm } from '@/api/system/employee-types';

export function useEmployeeCrud() {
  const [data, setData] = useState<EmployeeVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<EmployeeQueryForm>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
  });

  const query = useCallback(async () => {
    setLoading(true);
    try {
      const response = await employeeApi.query(queryForm);
      if (response.success) {
        setData(response.data.list);
        setTotal(response.data.total);
      } else {
        message.error(response.msg || '查詢失敗');
      }
    } finally {
      setLoading(false);
    }
  }, [queryForm]);

  const deleteEmployee = useCallback(async (employeeId: number) => {
    const response = await employeeApi.delete(employeeId);
    if (response.success) {
      message.success('刪除成功');
      query();  // 重新查詢
    } else {
      message.error(response.msg || '刪除失敗');
    }
  }, [query]);

  return {
    data,
    loading,
    total,
    queryForm,
    setQueryForm,
    query,
    deleteEmployee,
  };
}

// 使用 Custom Hook
const EmployeeList: React.FC = () => {
  const { data, loading, total, queryForm, setQueryForm, query, deleteEmployee } = useEmployeeCrud();

  useEffect(() => {
    query();
  }, [queryForm]);

  // ...
};
```

---

## TypeScript 類型模式

### 1. 後端 DTO 類型映射

```typescript
// Java → TypeScript 類型映射表
/**
 * Long/Integer → number
 * String → string
 * Boolean → boolean
 * LocalDateTime → string (ISO 8601格式)
 * BigDecimal → number
 * List<T> → T[]
 * Enum → string | number
 */

// 範例：後端 EmployeeVO
/**
 * public class EmployeeVO {
 *     private Long employeeId;
 *     private String employeeName;
 *     private String email;
 *     private Boolean deleted;
 *     private LocalDateTime createTime;
 *     private BigDecimal salary;
 *     private List<String> roles;
 *     private StatusEnum status;
 * }
 */

// 前端 TypeScript 類型
export interface EmployeeVO {
  employeeId: number;
  employeeName: string;
  email: string;
  deleted: boolean;
  createTime: string;  // "2026-03-06T10:30:00Z"
  salary: number;
  roles: string[];
  status: 1 | 2 | 3;  // 枚舉值
}
```

### 2. Form 類型定義

```typescript
// QueryForm - 查詢表單（+ 分頁參數）
export interface EmployeeQueryForm {
  keyword?: string;              // 搜索關鍵字（可選）
  departmentId?: number;         // 部門ID（可選）
  status?: 1 | 2 | 3;            // 狀態（可選）
  pageNum: number;               // 分頁頁碼（必填）
  pageSize: number;              // 分頁大小（必填）
  createTimeStart?: string;      // 創建時間開始（可選）
  createTimeEnd?: string;        // 創建時間結束（可選）
}

// AddForm - 新增表單（不含 ID）
export interface EmployeeAddForm {
  employeeName: string;
  email: string;
  phone?: string;
  departmentId: number;
  position: string;
  salary?: number;
}

// UpdateForm - 更新表單（含 ID）
export interface EmployeeUpdateForm {
  employeeId: number;            // 必須有 ID
  employeeName: string;
  email: string;
  phone?: string;
  departmentId: number;
  position: string;
  salary?: number;
}

// BatchDeleteForm - 批量刪除表單
export interface EmployeeBatchDeleteForm {
  employeeIdList: number[];
}
```

### 3. ResponseModel 類型

```typescript
// ResponseModel<T> - 通用響應模型
export interface ResponseModel<T> {
  code: number;           // 響應碼（1=成功，其他=失敗）
  data: T;                // 響應數據（泛型）
  msg?: string;           // 響應消息
  success: boolean;       // 是否成功
}

// PageResult<T> - 分頁結果模型
export interface PageResult<T> {
  list: T[];              // 數據列表
  total: number;          // 總記錄數
  pageNum: number;        // 當前頁碼
  pageSize: number;       // 每頁大小
  pages: number;          // 總頁數
  emptyFlag: boolean;     // 是否為空
}

// 使用範例
const response: ResponseModel<PageResult<EmployeeVO>> = await employeeApi.query(queryForm);
if (response.success) {
  const { list, total } = response.data;
  setData(list);
  setTotal(total);
}
```

### 4. Utility Types

```typescript
// Partial - 所有屬性變為可選
type PartialEmployee = Partial<EmployeeVO>;
// { employeeId?: number; employeeName?: string; ... }

// Pick - 選取部分屬性
type EmployeeBasicInfo = Pick<EmployeeVO, 'employeeId' | 'employeeName' | 'email'>;
// { employeeId: number; employeeName: string; email: string }

// Omit - 排除部分屬性
type EmployeeWithoutId = Omit<EmployeeVO, 'employeeId'>;
// 與 EmployeeAddForm 類似

// Required - 所有屬性變為必填
type RequiredEmployee = Required<EmployeeVO>;

// Readonly - 所有屬性變為只讀
type ReadonlyEmployee = Readonly<EmployeeVO>;
```

---

## Ant Design 5 組件速查

### 1. Table - 數據表格

```tsx
import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';

// 列定義（推薦使用 ColumnsType<T>）
const columns: ColumnsType<EmployeeVO> = [
  {
    title: 'ID',
    dataIndex: 'employeeId',
    key: 'employeeId',
    width: 80,
    fixed: 'left',  // 固定列
  },
  {
    title: '姓名',
    dataIndex: 'employeeName',
    key: 'employeeName',
    width: 150,
  },
  {
    title: '郵箱',
    dataIndex: 'email',
    key: 'email',
    ellipsis: true,  // 文本溢出省略號
  },
  {
    title: '創建時間',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    sorter: (a, b) => a.createTime.localeCompare(b.createTime),  // 排序
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right',
    render: (_, record) => (
      <Space>
        <Button type="link" onClick={() => handleEdit(record)}>編輯</Button>
        <Button type="link" danger onClick={() => handleDelete(record)}>刪除</Button>
      </Space>
    ),
  },
];

// Table 組件
<Table
  dataSource={data}
  columns={columns}
  loading={loading}
  rowKey="employeeId"
  scroll={{ x: 1200 }}  // 橫向滾動
  rowSelection={{
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  }}
  pagination={{
    current: queryForm.pageNum,
    pageSize: queryForm.pageSize,
    total: total,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 條`,
    onChange: (pageNum, pageSize) =>
      setQueryForm({ ...queryForm, pageNum, pageSize }),
  }}
/>
```

### 2. Form - 表單

```tsx
import { Form, Input, Select, DatePicker, InputNumber, Button } from 'antd';

const [form] = Form.useForm();

<Form
  form={form}
  labelCol={{ span: 6 }}
  wrapperCol={{ span: 16 }}
  onFinish={handleSubmit}
  autoComplete="off"
>
  {/* 文本輸入 */}
  <Form.Item
    label="姓名"
    name="employeeName"
    rules={[
      { required: true, message: '請輸入姓名' },
      { min: 2, max: 50, message: '長度為 2-50 個字符' },
    ]}
  >
    <Input placeholder="請輸入姓名" />
  </Form.Item>

  {/* 郵箱輸入 */}
  <Form.Item
    label="郵箱"
    name="email"
    rules={[
      { required: true, message: '請輸入郵箱' },
      { type: 'email', message: '郵箱格式不正確' },
    ]}
  >
    <Input placeholder="請輸入郵箱" />
  </Form.Item>

  {/* 下拉選擇 */}
  <Form.Item
    label="部門"
    name="departmentId"
    rules={[{ required: true, message: '請選擇部門' }]}
  >
    <Select placeholder="請選擇部門">
      <Select.Option value={1}>技術部</Select.Option>
      <Select.Option value={2}>產品部</Select.Option>
      <Select.Option value={3}>運營部</Select.Option>
    </Select>
  </Form.Item>

  {/* 數字輸入 */}
  <Form.Item label="薪資" name="salary">
    <InputNumber
      min={0}
      max={1000000}
      precision={2}
      style={{ width: '100%' }}
      placeholder="請輸入薪資"
    />
  </Form.Item>

  {/* 日期選擇 */}
  <Form.Item label="入職日期" name="entryDate">
    <DatePicker style={{ width: '100%' }} />
  </Form.Item>

  {/* 提交按鈕 */}
  <Form.Item wrapperCol={{ offset: 6, span: 16 }}>
    <Button type="primary" htmlType="submit">
      提交
    </Button>
    <Button onClick={() => form.resetFields()} style={{ marginLeft: 8 }}>
      重置
    </Button>
  </Form.Item>
</Form>
```

### 3. Modal - 模態框

```tsx
import { Modal, message } from 'antd';

// 基礎模態框
<Modal
  title="新增員工"
  open={visible}
  onOk={handleSubmit}
  onCancel={() => setVisible(false)}
  confirmLoading={loading}
  width={600}
  destroyOnClose  // 關閉時銷毀子元素
>
  <Form form={form}>
    {/* 表單字段 */}
  </Form>
</Modal>

// 確認對話框
Modal.confirm({
  title: '確認刪除',
  content: `確定要刪除「${record.employeeName}」嗎？`,
  onOk: async () => {
    const response = await employeeApi.delete(record.employeeId);
    if (response.success) {
      message.success('刪除成功');
      fetchData();
    } else {
      message.error(response.msg || '刪除失敗');
    }
  },
});

// 信息提示框
Modal.info({
  title: '提示',
  content: '這是一個信息提示框',
});

// 成功提示框
Modal.success({
  title: '成功',
  content: '操作已成功完成',
});

// 警告提示框
Modal.warning({
  title: '警告',
  content: '請注意這個操作的影響',
});

// 錯誤提示框
Modal.error({
  title: '錯誤',
  content: '操作失敗，請稍後重試',
});
```

### 4. Button - 按鈕

```tsx
import { Button } from 'antd';
import { PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';

// 基礎按鈕
<Button>默認按鈕</Button>
<Button type="primary">主按鈕</Button>
<Button type="dashed">虛線按鈕</Button>
<Button type="text">文本按鈕</Button>
<Button type="link">鏈接按鈕</Button>

// 危險按鈕
<Button danger>危險按鈕</Button>
<Button type="primary" danger>主危險按鈕</Button>

// 帶圖標
<Button type="primary" icon={<PlusOutlined />}>新增</Button>
<Button danger icon={<DeleteOutlined />}>刪除</Button>
<Button icon={<SearchOutlined />}>搜索</Button>

// 加載狀態
<Button type="primary" loading>加載中</Button>
<Button type="primary" loading={loading} onClick={handleClick}>
  提交
</Button>

// 禁用狀態
<Button disabled>禁用按鈕</Button>
<Button type="primary" disabled={selectedRowKeys.length === 0}>
  批量刪除
</Button>

// 尺寸
<Button size="large">大按鈕</Button>
<Button size="middle">中按鈕（默認）</Button>
<Button size="small">小按鈕</Button>
```

### 5. Message - 全局提示

```tsx
import { message } from 'antd';

// 成功提示
message.success('操作成功');
message.success('操作成功', 3);  // 持續3秒

// 錯誤提示
message.error('操作失敗');
message.error(response.msg || '操作失敗');

// 警告提示
message.warning('請注意');

// 信息提示
message.info('這是一條信息');

// 加載中提示
const hide = message.loading('加載中...', 0);  // 0 表示不自動關閉
// ... 異步操作
hide();  // 手動關閉

// Promise 接口
message.success('提交成功').then(() => {
  console.log('提示已關閉');
});
```

### 6. Space - 間距組件

```tsx
import { Space, Button } from 'antd';

// 水平間距
<Space>
  <Button>按鈕1</Button>
  <Button>按鈕2</Button>
  <Button>按鈕3</Button>
</Space>

// 垂直間距
<Space direction="vertical" style={{ width: '100%' }}>
  <Input placeholder="輸入框1" />
  <Input placeholder="輸入框2" />
  <Input placeholder="輸入框3" />
</Space>

// 自定義間距大小
<Space size="large">  {/* small | middle | large | number */}
  <Button>按鈕1</Button>
  <Button>按鈕2</Button>
</Space>

// 對齊方式
<Space align="center">  {/* start | end | center | baseline */}
  <Button>按鈕</Button>
  <span>文本</span>
</Space>
```

---

## 權限控制模式

### 1. usePrivilege Hook

```tsx
// 文件位置：smart-admin-web-react/src/hooks/usePrivilege.ts
import { useAppSelector } from '@/store/hooks';
import { selectAdministratorFlag, selectPointsList } from '@/store/slices/userSlice';

/**
 * 權限檢查 Hook（對應 Vue 的 v-privilege 指令）
 *
 * @param permissionCode 權限編碼（格式：{module}:{entity}:{operation}）
 * @returns 是否有權限
 */
export function usePrivilege(permissionCode: string): boolean {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  // 超級管理員：擁有所有權限
  if (administratorFlag) {
    return true;
  }

  // 檢查 pointsList 中是否包含該權限
  return pointsList.some((point) => point.webPerms === permissionCode);
}

// 批量權限檢查
export function usePrivileges(permissionCodes: string[]): Record<string, boolean> {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  if (administratorFlag) {
    return Object.fromEntries(permissionCodes.map((code) => [code, true]));
  }

  return Object.fromEntries(
    permissionCodes.map((code) => [
      code,
      pointsList.some((point) => point.webPerms === code),
    ])
  );
}
```

### 2. PrivilegeButton 組件

```tsx
// 文件位置：smart-admin-web-react/src/components/framework/privilege/PrivilegeButton.tsx
import { Button } from 'antd';
import type { ButtonProps } from 'antd';
import { usePrivilege } from '@/hooks/usePrivilege';

interface PrivilegeButtonProps extends ButtonProps {
  permissionCode: string;  // 權限編碼
  mode?: 'hide' | 'disable';  // 無權限時的處理方式
}

export const PrivilegeButton: React.FC<PrivilegeButtonProps> = ({
  permissionCode,
  mode = 'hide',
  children,
  disabled,
  ...buttonProps
}) => {
  const hasPermission = usePrivilege(permissionCode);

  // 無權限且隱藏模式：返回 null（對應 Vue 的 removeChild）
  if (!hasPermission && mode === 'hide') {
    return null;
  }

  // 無權限且禁用模式：禁用按鈕
  if (!hasPermission && mode === 'disable') {
    return (
      <Button {...buttonProps} disabled>
        {children}
      </Button>
    );
  }

  // 有權限：正常渲染（保留原始 disabled 狀態）
  return (
    <Button {...buttonProps} disabled={disabled}>
      {children}
    </Button>
  );
};
```

### 3. PrivilegeDiv 和 PrivilegeFragment

```tsx
// PrivilegeDiv - 條件渲染 DIV
export const PrivilegeDiv: React.FC<{
  permissionCode: string;
  children: React.ReactNode;
}> = ({ permissionCode, children }) => {
  const hasPermission = usePrivilege(permissionCode);
  return hasPermission ? <div>{children}</div> : null;
};

// PrivilegeFragment - 條件渲染 Fragment
export const PrivilegeFragment: React.FC<{
  permissionCode: string;
  children: React.ReactNode;
}> = ({ permissionCode, children }) => {
  const hasPermission = usePrivilege(permissionCode);
  return hasPermission ? <>{children}</> : null;
};
```

### 4. 權限使用範例

```tsx
import { PrivilegeButton, PrivilegeDiv, PrivilegeFragment } from '@/components/framework/privilege';
import { usePrivilege } from '@/hooks/usePrivilege';

export const EmployeeList: React.FC = () => {
  // Hook 方式（條件渲染）
  const canEdit = usePrivilege('system:employee:update');
  const canDelete = usePrivilege('system:employee:delete');

  return (
    <div>
      {/* Button 權限控制（推薦方式） */}
      <PrivilegeButton
        permissionCode="system:employee:add"
        type="primary"
        icon={<PlusOutlined />}
      >
        新增員工
      </PrivilegeButton>

      {/* 批量操作權限 */}
      <PrivilegeButton
        permissionCode="system:employee:delete"
        danger
        disabled={selectedRowKeys.length === 0}
      >
        批量刪除
      </PrivilegeButton>

      {/* Hook 方式（自定義邏輯） */}
      {canEdit && (
        <Button type="link" onClick={() => handleEdit(record)}>
          編輯
        </Button>
      )}

      {/* Div 權限控制 */}
      <PrivilegeDiv permissionCode="system:employee:export">
        <Button icon={<DownloadOutlined />}>導出</Button>
      </PrivilegeDiv>

      {/* Fragment 權限控制 */}
      <PrivilegeFragment permissionCode="system:employee:manage">
        <Button>高級設置</Button>
        <Dropdown>...</Dropdown>
      </PrivilegeFragment>
    </div>
  );
};
```

### 5. 後端權限對齊

**CRITICAL**: 前端 `permissionCode` 必須與後端 `@SaCheckPermission` **完全匹配**。

```java
// 後端 Controller（Java）
@SaCheckPermission("system:employee:query")
@PostMapping("/system/employee/query")
public ResponseDTO<PageResult<EmployeeVO>> queryEmployee(@RequestBody @Valid EmployeeQueryForm form) {
    // ...
}

@SaCheckPermission("system:employee:add")
@PostMapping("/system/employee/add")
public ResponseDTO<Void> addEmployee(@RequestBody @Valid EmployeeAddForm form) {
    // ...
}

@SaCheckPermission("system:employee:update")
@PostMapping("/system/employee/update")
public ResponseDTO<Void> updateEmployee(@RequestBody @Valid EmployeeUpdateForm form) {
    // ...
}

@SaCheckPermission("system:employee:delete")
@PostMapping("/system/employee/delete/{employeeId}")
public ResponseDTO<Void> deleteEmployee(@PathVariable Long employeeId) {
    // ...
}
```

```tsx
// 前端組件（React）
<PrivilegeButton permissionCode="system:employee:add">新增</PrivilegeButton>
<PrivilegeButton permissionCode="system:employee:update">編輯</PrivilegeButton>
<PrivilegeButton permissionCode="system:employee:delete">刪除</PrivilegeButton>
```

**權限字符串格式**: `{module}:{entity}:{operation}`

- `module`: system, business, oa
- `entity`: employee, department, role
- `operation`: query, add, update, delete, export, import

---

## API 集成模式

### 1. API 客戶端封裝

```typescript
// 文件位置：smart-admin-web-react/src/api/base/request.ts
import axios from 'axios';
import type { ResponseModel } from './response.model';
import { message, Modal } from 'antd';
import { ErrorCode } from './error-code';

// Axios 實例
const instance = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL || '/api',
  timeout: 30000,
});

// 請求攔截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('smart_admin_user_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 多租戶支持
    const tenantId = localStorage.getItem('smart_admin_tenant_id');
    const tenantTimezone = localStorage.getItem('smart_admin_tenant_timezone');
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId;
    }
    if (tenantTimezone) {
      config.headers['X-Timezone'] = tenantTimezone;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 響應攔截器
instance.interceptors.response.use(
  (response) => {
    const data: ResponseModel<any> = response.data;

    // Token 過期處理
    if (data.code === ErrorCode.TOKEN_EXPIRED) {
      message.error('登錄已過期，請重新登錄');
      // 清除 Token 並跳轉到登錄頁
      localStorage.removeItem('smart_admin_user_token');
      window.location.href = '/';
      return Promise.reject(new Error('Token expired'));
    }

    // 賬號別處登錄
    if (data.code === ErrorCode.ACCOUNT_LOGIN_ELSEWHERE) {
      Modal.warning({
        title: '賬號已在別處登錄',
        content: '您的賬號已在其他地方登錄，請重新登錄',
        onOk: () => {
          localStorage.removeItem('smart_admin_user_token');
          window.location.href = '/';
        },
      });
      return Promise.reject(new Error('Account login elsewhere'));
    }

    return response;
  },
  (error) => {
    message.error('網絡錯誤，請稍後重試');
    return Promise.reject(error);
  }
);

// 通用 POST 請求
export async function postRequest<T>(url: string, data?: any): Promise<ResponseModel<T>> {
  const response = await instance.post<ResponseModel<T>>(url, data);
  return response.data;
}

// 通用 GET 請求
export async function getRequest<T>(url: string, params?: any): Promise<ResponseModel<T>> {
  const response = await instance.get<ResponseModel<T>>(url, { params });
  return response.data;
}

// PUT 請求
export async function putRequest<T>(url: string, data?: any): Promise<ResponseModel<T>> {
  const response = await instance.put<ResponseModel<T>>(url, data);
  return response.data;
}

// DELETE 請求
export async function deleteRequest<T>(url: string, params?: any): Promise<ResponseModel<T>> {
  const response = await instance.delete<ResponseModel<T>>(url, { params });
  return response.data;
}

// 文件下載（GET）
export async function getDownload(url: string, params?: any): Promise<Blob> {
  const response = await instance.get(url, { params, responseType: 'blob' });
  return response.data;
}

// 文件下載（POST）
export async function postDownload(url: string, data?: any): Promise<Blob> {
  const response = await instance.post(url, data, { responseType: 'blob' });
  return response.data;
}
```

### 2. ResponseModel 錯誤處理

```typescript
// 標準錯誤處理模式
const response = await employeeApi.query(queryForm);

if (response.success) {
  // 成功：提取數據
  const { list, total } = response.data;
  setData(list);
  setTotal(total);
} else {
  // 失敗：顯示錯誤消息
  message.error(response.msg || '查詢失敗');
}

// Try-Finally 模式（加載狀態）
setLoading(true);
try {
  const response = await employeeApi.query(queryForm);
  if (response.success) {
    setData(response.data.list);
    setTotal(response.data.total);
  } else {
    message.error(response.msg || '查詢失敗');
  }
} catch (error) {
  message.error('網絡錯誤，請稍後重試');
} finally {
  setLoading(false);
}

// Promise Chain 模式
employeeApi.query(queryForm)
  .then((response) => {
    if (response.success) {
      setData(response.data.list);
      setTotal(response.data.total);
    } else {
      message.error(response.msg || '查詢失敗');
    }
  })
  .catch(() => {
    message.error('網絡錯誤');
  })
  .finally(() => {
    setLoading(false);
  });
```

### 3. 實體 API 客戶端範例

```typescript
// 文件位置：smart-admin-web-react/src/api/system/employee-api.ts
import { postRequest, getRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type {
  EmployeeQueryForm,
  EmployeeVO,
  EmployeeAddForm,
  EmployeeUpdateForm,
  EmployeeBatchDeleteForm,
} from './employee-types';

const BASE_URL = '/system/employee';

/**
 * Employee API 客戶端
 */
export const employeeApi = {
  /**
   * 分頁查詢員工列表
   */
  query: (queryForm: EmployeeQueryForm) => {
    return postRequest<PageResult<EmployeeVO>>(`${BASE_URL}/query`, queryForm);
  },

  /**
   * 根據 ID 獲取員工詳情
   */
  getById: (employeeId: number) => {
    return getRequest<EmployeeVO>(`${BASE_URL}/get/${employeeId}`);
  },

  /**
   * 新增員工
   */
  add: (addForm: EmployeeAddForm) => {
    return postRequest<void>(`${BASE_URL}/add`, addForm);
  },

  /**
   * 更新員工
   */
  update: (updateForm: EmployeeUpdateForm) => {
    return postRequest<void>(`${BASE_URL}/update`, updateForm);
  },

  /**
   * 刪除員工
   */
  delete: (employeeId: number) => {
    return postRequest<void>(`${BASE_URL}/delete/${employeeId}`);
  },

  /**
   * 批量刪除員工
   */
  batchDelete: (batchDeleteForm: EmployeeBatchDeleteForm) => {
    return postRequest<void>(`${BASE_URL}/batchDelete`, batchDeleteForm);
  },

  /**
   * 導出員工列表
   */
  export: (queryForm: EmployeeQueryForm) => {
    return postRequest<string>(`${BASE_URL}/export`, queryForm);
  },
};
```

---

## 測試模式

### 1. Vitest 配置

```typescript
// 文件位置：smart-admin-web-react/vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react-swc';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/test/',
        '**/*.d.ts',
        '**/*.config.*',
        '**/mockData',
        '**/dist',
      ],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

### 2. Test Setup

```typescript
// 文件位置：smart-admin-web-react/src/test/setup.ts
import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

// 擴展 Vitest 的 expect 匹配器
expect.extend(matchers);

// 每個測試後清理
afterEach(() => {
  cleanup();
});

// Mock window.matchMedia（Ant Design Grid 需要）
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock ResizeObserver（Ant Design Table 需要）
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));
```

### 3. 組件測試範例

```tsx
// 文件位置：smart-admin-web-react/src/views/system/employee/__tests__/EmployeeList.spec.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { EmployeeList } from '../EmployeeList';
import { employeeApi } from '@/api/system/employee-api';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/slices/userSlice';

// Mock API
vi.mock('@/api/system/employee-api');

// 創建測試 Store
const createTestStore = () => {
  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: {
        token: 'test-token',
        employeeId: '1',
        administratorFlag: true,
        pointsList: [],
        menuTree: [],
      },
    },
  });
};

describe('EmployeeList Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch and display employee data on mount', async () => {
    const mockData = {
      success: true,
      data: {
        list: [
          {
            employeeId: 1,
            employeeName: 'John Doe',
            email: 'john@example.com',
            createTime: '2026-03-06T10:00:00Z',
          },
        ],
        total: 1,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        emptyFlag: false,
      },
    };
    vi.mocked(employeeApi.query).mockResolvedValue(mockData);

    const store = createTestStore();
    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    // 等待數據加載完成
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('john@example.com')).toBeInTheDocument();
    });

    // 驗證 API 調用
    expect(employeeApi.query).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 10,
      keyword: '',
    });
  });

  it('should handle search keyword input and query', async () => {
    const user = userEvent.setup();
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    // 輸入搜索關鍵字
    const searchInput = screen.getByPlaceholderText('請輸入搜索關鍵字');
    await user.type(searchInput, 'test');

    // 點擊查詢按鈕
    const queryButton = screen.getByRole('button', { name: '查詢' });
    await user.click(queryButton);

    // 驗證 API 調用
    await waitFor(() => {
      expect(employeeApi.query).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: 'test' })
      );
    });
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    // 點擊新增按鈕
    const addButton = screen.getByRole('button', { name: /新增/i });
    await user.click(addButton);

    // 驗證模態框打開
    await waitFor(() => {
      expect(screen.getByText(/新增員工/i)).toBeInTheDocument();
    });
  });

  it('should call delete API and refresh data when delete confirmed', async () => {
    const mockQueryData = {
      success: true,
      data: {
        list: [
          {
            employeeId: 1,
            employeeName: 'John Doe',
            email: 'john@example.com',
            createTime: '2026-03-06T10:00:00Z',
          },
        ],
        total: 1,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        emptyFlag: false,
      },
    };
    vi.mocked(employeeApi.query).mockResolvedValue(mockQueryData);
    vi.mocked(employeeApi.delete).mockResolvedValue({ success: true });

    const user = userEvent.setup();
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    // 等待數據加載
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
    });

    // 點擊刪除按鈕
    const deleteButton = screen.getByRole('button', { name: /刪除/i });
    await user.click(deleteButton);

    // 確認刪除
    const confirmButton = screen.getByRole('button', { name: '確定' });
    await user.click(confirmButton);

    // 驗證 API 調用
    await waitFor(() => {
      expect(employeeApi.delete).toHaveBeenCalledWith(1);
    });
  });
});
```

### 4. API Mock 模式

```typescript
// Mock API 模塊
vi.mock('@/api/system/employee-api');

// Mock 成功響應
vi.mocked(employeeApi.query).mockResolvedValue({
  success: true,
  data: {
    list: [/* mock data */],
    total: 1,
  },
});

// Mock 失敗響應
vi.mocked(employeeApi.add).mockResolvedValue({
  success: false,
  code: 500,
  msg: '添加失敗',
});

// Mock 網絡錯誤
vi.mocked(employeeApi.query).mockRejectedValue(new Error('Network error'));

// Mock 函數調用次數驗證
expect(employeeApi.query).toHaveBeenCalledTimes(1);
expect(employeeApi.query).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 });
```

### 5. 用戶交互測試

```typescript
import userEvent from '@testing-library/user-event';

// Setup user-event
const user = userEvent.setup();

// 點擊按鈕
const button = screen.getByRole('button', { name: '新增' });
await user.click(button);

// 輸入文本
const input = screen.getByLabelText('姓名');
await user.type(input, 'John Doe');

// 清空輸入框
await user.clear(input);

// 選擇下拉選項
const select = screen.getByLabelText('部門');
await user.click(select);
const option = screen.getByText('技術部');
await user.click(option);

// 勾選複選框
const checkbox = screen.getByRole('checkbox');
await user.click(checkbox);

// 鍵盤事件
await user.keyboard('{Enter}');
await user.keyboard('{Escape}');
```

---

## 完整 CRUD 範例

由於長度限制，完整範例已經在上面的章節中分段展示。以下是完整文件清單：

### Employee CRUD 模塊文件結構

```
smart-admin-web-react/src/
├── api/system/
│   ├── employee-types.ts              # TypeScript 類型定義
│   └── employee-api.ts                # API 客戶端
└── views/system/employee/
    ├── EmployeeList.tsx               # 列表組件
    ├── EmployeeFormModal.tsx          # 表單模態框
    └── __tests__/
        ├── EmployeeList.spec.tsx      # 列表組件測試
        └── EmployeeFormModal.spec.tsx # 表單模態框測試
```

請參考前面章節的代碼範例來創建這些文件。

---

## Vue vs React 對照表

| 功能 | Vue 3 | React 19 |
|------|-------|---------|
| **狀態管理** | `reactive({ count: 0 })` | `useState(0)` |
| **雙向綁定** | `v-model="form.name"` | `value={form.name} onChange={e => setForm({...form, name: e.target.value})}` |
| **列表渲染** | `v-for="item in list" :key="item.id"` | `list.map(item => <div key={item.id}>{item.name}</div>)` |
| **條件渲染** | `v-if="visible"` | `{visible && <Component />}` |
| **事件處理** | `@click="handleClick"` | `onClick={handleClick}` |
| **組件 Props** | `<MyComponent :prop="value" />` | `<MyComponent prop={value} />` |
| **Emit 事件** | `emit('update', data)` | `onUpdate(data)` (回調函數) |
| **計算屬性** | `computed(() => ...)` | `useMemo(() => ..., [deps])` |
| **監聽器** | `watch(() => ..., () => ...)` | `useEffect(() => ..., [deps])` |
| **生命週期** | `onMounted(() => ...)` | `useEffect(() => ..., [])` |
| **權限控制** | `v-privilege="'system:user:add'"` | `<PrivilegeButton permissionCode="system:user:add">` |

---

## 常見問題解決

### 問題 1：TypeScript 類型錯誤

**症狀**: `Type 'X' is not assignable to type 'Y'`

**解決方案**:
```typescript
// ❌ 錯誤：類型不匹配
const employee: EmployeeVO = {
  employeeId: "1",  // 應該是 number
  employeeName: "John",
};

// ✅ 正確：使用正確的類型
const employee: EmployeeVO = {
  employeeId: 1,
  employeeName: "John",
};

// ✅ 正確：使用類型斷言（謹慎使用）
const employee = response.data as EmployeeVO;
```

### 問題 2：權限不生效

**症狀**: 按鈕顯示但應該被隱藏

**解決方案**:
```tsx
// 檢查 1：權限字符串拼寫
<PrivilegeButton permissionCode="system:employee:add">  {/* ✅ 正確 */}
<PrivilegeButton permissionCode="system:employe:add">   {/* ❌ 拼寫錯誤 */}

// 檢查 2：使用 React DevTools 檢查 Redux State
// state.user.pointsList 應該包含該權限

// 檢查 3：super admin bypasses all checks
// state.user.administratorFlag === true 時所有按鈕都會顯示
```

### 問題 3：API 調用失敗

**症狀**: `response.success === false`

**解決方案**:
```typescript
// 檢查 1：端點路徑是否正確
const response = await employeeApi.query(queryForm);
// 後端: POST /system/employee/query
// 前端: postRequest('/system/employee/query', queryForm)  ✅

// 檢查 2：請求體結構是否匹配
// 後端: @RequestBody EmployeeQueryForm
// 前端: { pageNum: 1, pageSize: 10, keyword: '' }  ✅

// 檢查 3：查看 Network tab
// - HTTP status code（200 OK?）
// - Request payload（JSON 格式正確?）
// - Response body（錯誤消息?）

// 檢查 4：後端日誌
// - Controller 是否接收到請求?
// - Service 是否拋出異常?
```

### 問題 4：測試失敗

**症狀**: Vitest 測試超時或失敗

**解決方案**:
```typescript
// 問題：忘記 Mock API
vi.mock('@/api/system/employee-api');  // ✅ 必須 Mock

// 問題：異步操作未等待
await waitFor(() => {
  expect(screen.getByText('John')).toBeInTheDocument();
});  // ✅ 使用 waitFor

// 問題：未清理 Mock
beforeEach(() => {
  vi.clearAllMocks();  // ✅ 每個測試前清理
});

// 問題：查詢選擇器錯誤
screen.getByText('John Doe');  // ✅ 完整文本
screen.getByText(/John/i);     // ✅ 正則表達式
screen.getByRole('button', { name: /新增/i });  // ✅ 角色查詢
```

---

**文檔版本**: 1.0.0
**最後更新**: 2026-03-06
**總頁數**: 約 1200 行

此文檔涵蓋了 SmartAdmin React 開發的核心模式和最佳實踐。如需更多信息，請參考：
- [SKILL.md](../SKILL.md) - 技能定義
- [SmartAdmin Patterns](../../shared/knowledge/smartadmin-patterns.md) - 後端模式
- [Architecture Rules](../../../.agent/rules/foundation/F04-architecture-rules.md) - 架構規則
