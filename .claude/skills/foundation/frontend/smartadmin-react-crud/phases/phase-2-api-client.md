# Phase 2: API 客戶端生成

**預計時間**: ~5 分鐘
**輸出文件**: `smart-admin-web-react/src/api/{module}/{entity}-api.ts`
**依賴**: Phase 1（TypeScript 類型定義）

---

## 概述

Phase 2 生成 API 客戶端，封裝所有 HTTP 請求邏輯，提供類型安全的 API 方法。

---

## 生成的 API 方法

### 標準 CRUD API 方法

1. **query** - 分頁查詢
2. **getById** - 根據 ID 獲取詳情
3. **add** - 新增
4. **update** - 更新
5. **delete** - 刪除
6. **batchDelete** - 批量刪除

---

## 代碼模板

```typescript
// smart-admin-web-react/src/api/{module}/{entity}-api.ts

import { postRequest, getRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type {
  {Entity}QueryForm,
  {Entity}VO,
  {Entity}AddForm,
  {Entity}UpdateForm,
  {Entity}BatchDeleteForm,
} from './{entity}-types';

/**
 * API 基礎路徑
 * 對應後端: @RequestMapping("/{module}/{entity}")
 */
const BASE_URL = '/{module}/{entity}';

/**
 * {Entity} API 客戶端
 */
export const {entity}Api = {
  /**
   * 分頁查詢
   *
   * @param queryForm 查詢表單
   * @returns 分頁結果
   *
   * 對應後端: POST /{module}/{entity}/query
   */
  query: (queryForm: {Entity}QueryForm) => {
    return postRequest<PageResult<{Entity}VO>>(`${BASE_URL}/query`, queryForm);
  },

  /**
   * 根據 ID 獲取詳情
   *
   * @param {entity}Id 主鍵 ID
   * @returns VO 對象
   *
   * 對應後端: GET /{module}/{entity}/get/{{id}}
   */
  getById: ({entity}Id: number) => {
    return getRequest<{Entity}VO>(`${BASE_URL}/get/${{{entity}Id}}`);
  },

  /**
   * 新增
   *
   * @param addForm 新增表單
   * @returns 無返回值
   *
   * 對應後端: POST /{module}/{entity}/add
   */
  add: (addForm: {Entity}AddForm) => {
    return postRequest<void>(`${BASE_URL}/add`, addForm);
  },

  /**
   * 更新
   *
   * @param updateForm 更新表單
   * @returns 無返回值
   *
   * 對應後端: POST /{module}/{entity}/update
   */
  update: (updateForm: {Entity}UpdateForm) => {
    return postRequest<void>(`${BASE_URL}/update`, updateForm);
  },

  /**
   * 刪除
   *
   * @param {entity}Id 主鍵 ID
   * @returns 無返回值
   *
   * 對應後端: POST /{module}/{entity}/delete/{{id}}
   */
  delete: ({entity}Id: number) => {
    return postRequest<void>(`${BASE_URL}/delete/${{{entity}Id}}`);
  },

  /**
   * 批量刪除
   *
   * @param batchDeleteForm 批量刪除表單
   * @returns 無返回值
   *
   * 對應後端: POST /{module}/{entity}/batchDelete
   */
  batchDelete: (batchDeleteForm: {Entity}BatchDeleteForm) => {
    return postRequest<void>(`${BASE_URL}/batchDelete`, batchDeleteForm);
  },
};
```

---

## 完整範例：Employee API 客戶端

```typescript
// smart-admin-web-react/src/api/system/employee-api.ts

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
   * 導出員工列表（可選）
   */
  export: (queryForm: EmployeeQueryForm) => {
    return postRequest<string>(`${BASE_URL}/export`, queryForm);
  },
};
```

---

## ResponseModel 處理

### 基礎響應模型

```typescript
// smart-admin-web-react/src/api/base/response.model.ts

export interface ResponseModel<T> {
  code: number;           // 響應碼（1=成功，其他=失敗）
  data: T;                // 響應數據（泛型）
  msg?: string;           // 響應消息
  success: boolean;       // 是否成功
}
```

### 分頁響應模型

```typescript
// smart-admin-web-react/src/api/base/page.model.ts

export interface PageResult<T> {
  list: T[];              // 數據列表
  total: number;          // 總記錄數
  pageNum: number;        // 當前頁碼
  pageSize: number;       // 每頁大小
  pages: number;          // 總頁數
  emptyFlag: boolean;     // 是否為空
}
```

### 使用範例

```typescript
// ✅ 正確：檢查 success 後再訪問 data
const response = await employeeApi.query(queryForm);
if (response.success) {
  const { list, total } = response.data;
  setData(list);
  setTotal(total);
} else {
  message.error(response.msg || '查詢失敗');
}

// ❌ 錯誤：未檢查 success 直接訪問 data
const response = await employeeApi.query(queryForm);
const { list, total } = response.data;  // 可能為 null
setData(list);  // 可能報錯
```

---

## postRequest vs getRequest

### 使用規則

| HTTP 方法 | 函數 | 何時使用 |
|-----------|------|---------|
| POST | `postRequest` | 查詢（query）、新增、更新、刪除 |
| GET | `getRequest` | 獲取詳情（getById）、下拉選項 |
| PUT | `putRequest` | 更新（REST 風格，較少使用） |
| DELETE | `deleteRequest` | 刪除（REST 風格，較少使用） |

### SmartAdmin 慣例

SmartAdmin **主要使用 POST**，即使是查詢操作：

```java
// 後端 Controller
@PostMapping("/query")  // ✅ SmartAdmin 慣例（使用 POST）
public ResponseDTO<PageResult<EmployeeVO>> query(@RequestBody EmployeeQueryForm form) { ... }

// @GetMapping("/query")  // ❌ 不推薦（GET 無法傳遞複雜查詢條件）
```

```typescript
// 前端 API 客戶端
employeeApi.query(queryForm)  // ✅ 使用 postRequest
```

**原因**：
- 查詢條件可能很複雜（多個過濾字段）
- GET 請求 URL 長度有限制
- POST 請求體可以傳遞 JSON 對象

---

## 擴展 API 方法（可選）

### 導出功能

```typescript
export: (queryForm: {Entity}QueryForm) => {
  return postRequest<string>(`${BASE_URL}/export`, queryForm);
}
```

### 導入功能

```typescript
import: (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return postRequest<void>(`${BASE_URL}/import`, formData);
}
```

### 下拉選項

```typescript
/**
 * 獲取下拉選項（不分頁）
 */
listAll: () => {
  return getRequest<{Entity}VO[]>(`${BASE_URL}/listAll`);
}
```

### 狀態切換

```typescript
/**
 * 啟用/禁用
 */
toggleStatus: ({entity}Id: number, status: number) => {
  return postRequest<void>(`${BASE_URL}/toggleStatus`, { {entity}Id, status });
}
```

---

## 驗證清單

生成 API 客戶端後，檢查以下項目：

- [ ] 文件路徑正確：`smart-admin-web-react/src/api/{module}/{entity}-api.ts`
- [ ] 導入 Phase 1 生成的類型
- [ ] BASE_URL 與後端 @RequestMapping 對應
- [ ] 所有 API 方法與後端 Controller 方法對應
- [ ] 使用正確的 HTTP 方法（POST vs GET）
- [ ] 使用正確的泛型類型（`PageResult<VO>`, `VO`, `void`）
- [ ] 添加 JSDoc 註釋（描述、參數、返回值）
- [ ] TypeScript 編譯無錯誤

---

## 常見問題

### 問題 1：後端使用 GET，前端該用 getRequest 還是 postRequest？

**答案**：遵循後端的 HTTP 方法

```java
// 後端
@GetMapping("/get/{id}")
public ResponseDTO<EmployeeVO> getById(@PathVariable Long id) { ... }
```

```typescript
// 前端
getById: (employeeId: number) => {
  return getRequest<EmployeeVO>(`${BASE_URL}/get/${employeeId}`);
}
```

### 問題 2：如何處理文件上傳？

**答案**：使用 FormData

```typescript
import: (file: File) => {
  const formData = new FormData();
  formData.append('file', file);

  return postRequest<void>(`${BASE_URL}/import`, formData);
}
```

### 問題 3：如何處理文件下載？

**答案**：使用 `postDownload` 或 `getDownload`

```typescript
import { postDownload } from '@/api/base/request';

export: async (queryForm: EmployeeQueryForm) => {
  const blob = await postDownload(`${BASE_URL}/export`, queryForm);

  // 創建下載鏈接
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'employees.xlsx';
  link.click();
  window.URL.revokeObjectURL(url);
}
```

---

**Phase 2 完成標準**：
- ✅ API 客戶端文件創建成功
- ✅ 所有 API 方法與後端對應
- ✅ TypeScript 編譯無錯誤
- ✅ 可以成功調用後端 API（手動測試）

**下一步**：進入 [Phase 3: 列表組件生成](phase-3-list-component.md)
