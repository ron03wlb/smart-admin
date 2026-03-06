# Phase 1: TypeScript 類型生成

**預計時間**: ~3 分鐘
**輸出文件**: `smart-admin-web-react/src/api/{module}/{entity}-types.ts`
**依賴**: 後端 DTO 定義

---

## 概述

Phase 1 生成 TypeScript 類型定義，與後端 Java DTOs **100% 對應**。這些類型是後續所有 Phase 的基礎。

---

## 生成的類型

### 1. QueryForm - 查詢表單類型

對應後端的 `{Entity}QueryForm.java`，包含：
- 分頁參數（`pageNum`, `pageSize`）
- 搜索關鍵字（`keyword`）
- 過濾字段（由後端 QueryForm 決定）

```typescript
/**
 * {Entity} 查詢表單
 * 對應後端: net.lab1024.sa.{module}.{entity}.domain.form.{Entity}QueryForm
 */
export interface {Entity}QueryForm {
  keyword?: string;              // 搜索關鍵字
  pageNum: number;               // 分頁頁碼
  pageSize: number;              // 分頁大小

  // 根據實體添加過濾字段
  status?: number;               // 狀態（可選）
  categoryId?: number;           // 分類ID（可選）
  createTimeStart?: string;      // 創建時間開始（可選）
  createTimeEnd?: string;        // 創建時間結束（可選）
}
```

### 2. VO - 視圖對象類型

對應後端的 `{Entity}VO.java`，包含所有查詢結果字段（含 JOIN 字段）。

```typescript
/**
 * {Entity} VO（視圖對象）
 * 對應後端: net.lab1024.sa.{module}.{entity}.domain.vo.{Entity}VO
 */
export interface {Entity}VO {
  {entity}Id: number;            // 主鍵（對應 Long）

  // 根據實體添加字段
  name: string;
  code: string;
  status: number;
  remark?: string;

  // JOIN 字段（如果有）
  categoryName?: string;         // 分類名稱（來自 JOIN）
  creatorName?: string;          // 創建人姓名（來自 JOIN）

  // 審計字段
  createTime: string;            // ISO 8601 格式
  updateTime: string;
  deleted: boolean;
}
```

### 3. AddForm - 新增表單類型

對應後端的 `{Entity}AddForm.java`，**不含主鍵 ID**。

```typescript
/**
 * {Entity} 新增表單
 * 對應後端: net.lab1024.sa.{module}.{entity}.domain.form.{Entity}AddForm
 */
export interface {Entity}AddForm {
  // 根據實體添加字段（不含 ID）
  name: string;
  code: string;
  status: number;
  remark?: string;
  categoryId?: number;
}
```

### 4. UpdateForm - 更新表單類型

對應後端的 `{Entity}UpdateForm.java`，**必須含主鍵 ID**。

```typescript
/**
 * {Entity} 更新表單
 * 對應後端: net.lab1024.sa.{module}.{entity}.domain.form.{Entity}UpdateForm
 */
export interface {Entity}UpdateForm {
  {entity}Id: number;            // 主鍵（必填）

  // 與 AddForm 相同的字段
  name: string;
  code: string;
  status: number;
  remark?: string;
  categoryId?: number;
}
```

### 5. BatchDeleteForm - 批量刪除表單類型

```typescript
/**
 * {Entity} 批量刪除表單
 */
export interface {Entity}BatchDeleteForm {
  {entity}IdList: number[];      // ID 列表
}
```

---

## Java ↔ TypeScript 類型映射

| Java 類型 | TypeScript 類型 | 範例 | 說明 |
|-----------|----------------|------|------|
| Long | number | `employeeId: number` | 主鍵通常用 Long |
| Integer | number | `status: number` | 枚舉值、數量等 |
| String | string | `name: string` | 文本字段 |
| Boolean | boolean | `deleted: boolean` | 布爾標誌 |
| LocalDateTime | string | `createTime: string` | ISO 8601 格式（如 `"2026-03-06T10:30:00Z"`） |
| LocalDate | string | `birthDate: string` | ISO 8601 日期（如 `"2026-03-06"`） |
| BigDecimal | number | `price: number` | 價格、金額（前端用 number） |
| List&lt;T&gt; | T[] | `roles: string[]` | 數組類型 |
| Enum | string \| number | `status: 1 \| 2 \| 3` | 枚舉值（推薦用 union type） |

---

## 可選字段處理

### 規則

1. **後端 `@NotNull` / `@NotBlank`** → 前端**必填**（不加 `?`）
2. **後端無驗證註解** → 前端**可選**（加 `?`）
3. **後端 JOIN 字段** → 前端**可選**（加 `?`，查詢結果可能為 null）

### 範例

```java
// 後端 EmployeeAddForm.java
public class EmployeeAddForm {
    @NotBlank(message = "姓名不能為空")
    private String employeeName;  // 必填

    private String phone;          // 可選

    @NotNull(message = "部門ID不能為空")
    private Long departmentId;     // 必填
}
```

```typescript
// 前端 employee-types.ts
export interface EmployeeAddForm {
  employeeName: string;      // 必填（無 ?）
  phone?: string;            // 可選（有 ?）
  departmentId: number;      // 必填（無 ?）
}
```

---

## 枚舉類型處理

### 方式 1：Union Type（推薦）

```typescript
// 後端 Java Enum
public enum StatusEnum {
    AVAILABLE(1, "可用"),
    INACTIVE(2, "不可用"),
    DELETED(3, "已刪除");
}

// 前端 TypeScript
export type Status = 1 | 2 | 3;

export interface ProductVO {
  productId: number;
  status: Status;  // 類型安全
}
```

### 方式 2：常量對象

```typescript
export const StatusEnum = {
  AVAILABLE: { value: 1, label: '可用' },
  INACTIVE: { value: 2, label: '不可用' },
  DELETED: { value: 3, label: '已刪除' },
} as const;

export type Status = typeof StatusEnum[keyof typeof StatusEnum]['value'];

// 使用
<Select>
  <Select.Option value={StatusEnum.AVAILABLE.value}>
    {StatusEnum.AVAILABLE.label}
  </Select.Option>
</Select>
```

---

## 完整範例：Employee 類型

```typescript
// smart-admin-web-react/src/api/system/employee-types.ts

/**
 * Employee 查詢表單
 */
export interface EmployeeQueryForm {
  keyword?: string;
  pageNum: number;
  pageSize: number;
  departmentId?: number;
  status?: 1 | 2 | 3;
  createTimeStart?: string;
  createTimeEnd?: string;
}

/**
 * Employee VO
 */
export interface EmployeeVO {
  employeeId: number;
  employeeName: string;
  email: string;
  phone?: string;
  departmentId: number;
  departmentName?: string;        // JOIN 字段
  position: string;
  status: 1 | 2 | 3;              // 1=在職，2=離職，3=禁用
  salary?: number;
  entryDate?: string;
  createTime: string;
  updateTime: string;
  deleted: boolean;
}

/**
 * Employee 新增表單
 */
export interface EmployeeAddForm {
  employeeName: string;
  email: string;
  phone?: string;
  departmentId: number;
  position: string;
  status: 1 | 2 | 3;
  salary?: number;
  entryDate?: string;
}

/**
 * Employee 更新表單
 */
export interface EmployeeUpdateForm {
  employeeId: number;
  employeeName: string;
  email: string;
  phone?: string;
  departmentId: number;
  position: string;
  status: 1 | 2 | 3;
  salary?: number;
  entryDate?: string;
}

/**
 * Employee 批量刪除表單
 */
export interface EmployeeBatchDeleteForm {
  employeeIdList: number[];
}

/**
 * Employee 狀態枚舉
 */
export const EmployeeStatusEnum = {
  ACTIVE: { value: 1, label: '在職' },
  RESIGNED: { value: 2, label: '離職' },
  DISABLED: { value: 3, label: '禁用' },
} as const;
```

---

## 驗證清單

生成 TypeScript 類型後，檢查以下項目：

- [ ] 文件路徑正確：`smart-admin-web-react/src/api/{module}/{entity}-types.ts`
- [ ] 類型名稱遵循 PascalCase（如 `EmployeeVO`）
- [ ] 字段名稱遵循 camelCase（如 `employeeId`）
- [ ] 必填/可選字段與後端驗證註解對應
- [ ] 主鍵類型為 `number`（對應 Java `Long`）
- [ ] 時間字段類型為 `string`（ISO 8601 格式）
- [ ] 枚舉值使用 Union Type 或常量對象
- [ ] 批量刪除表單包含 ID 列表字段

---

## 常見問題

### 問題 1：時間字段用 Date 還是 string？

**答案**：使用 `string`（ISO 8601 格式）

**理由**：
- 後端返回 `LocalDateTime` 序列化為 ISO 8601 字符串
- 前端 Ant Design DatePicker 接受 `dayjs` 或字符串
- 避免時區問題

```typescript
// ✅ 正確
createTime: string;  // "2026-03-06T10:30:00Z"

// ❌ 錯誤
createTime: Date;    // 會導致類型不匹配
```

### 問題 2：BigDecimal 用 number 還是 string？

**答案**：通常使用 `number`

**理由**：
- 前端展示和計算用 `number` 更方便
- 如果需要高精度計算，使用庫（如 `bignumber.js`）
- 提交到後端時，後端會自動轉換為 `BigDecimal`

```typescript
// ✅ 正確（一般場景）
price: number;       // 99.99

// ✅ 正確（高精度場景）
price: string;       // "99.99"（使用 bignumber.js）
```

### 問題 3：UpdateForm 可以繼承 AddForm 嗎？

**答案**：可以，使用 TypeScript utility types

```typescript
// 方式 1：繼承（推薦）
export interface EmployeeUpdateForm extends EmployeeAddForm {
  employeeId: number;
}

// 方式 2：Intersection Type
export type EmployeeUpdateForm = EmployeeAddForm & {
  employeeId: number;
};

// 方式 3：顯式定義（如果字段不完全相同）
export interface EmployeeUpdateForm {
  employeeId: number;
  employeeName: string;
  // ...
}
```

---

**Phase 1 完成標準**：
- ✅ TypeScript 類型文件創建成功
- ✅ 所有類型與後端 DTO 對應
- ✅ TypeScript 編譯無錯誤（`npm run type-check`）

**下一步**：進入 [Phase 2: API 客戶端生成](phase-2-api-client.md)
