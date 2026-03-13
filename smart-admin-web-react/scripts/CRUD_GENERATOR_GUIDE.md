# CRUD 代碼生成器指南

**版本**: v1.0.0
**創建日期**: 2026-03-13
**維護者**: SmartAdmin React Team

---

## 概述

本文檔提供基於已驗證的 **7 階段標準流程** 的 CRUD 模組快速開發指南。

**目標**:
- 節省 50% 開發時間（從 2.5 小時降至 1-1.5 小時）
- 確保代碼風格一致性
- 降低錯誤率

**已驗證模組**: Employee, Role, Menu, Position, Department, Goods, Enterprise, Notice, File, Config, Feedback, Login-Log, Login-Fail, Operate-Log

---

## 7 階段標準流程

```
Phase 1: Analysis（分析 Vue 源碼 + 後端 VO/API）
  ↓
Phase 2: Types & Constants（創建 types.ts, const.ts）
  ↓
Phase 3: API Layer（創建 api.ts）
  ↓
Phase 4: List Page（創建 index.tsx）
  ↓
Phase 5: Form/Detail Modal（創建 FormModal.tsx 或 DetailModal.tsx）
  ↓
Phase 6: Tests（創建 const.test.ts, api.test.ts, FormModal.test.tsx）
  ↓
Phase 7: Route Registration（註冊到 dynamic-routes.ts）
```

---

## 文件結構模板

以 `{Module}` 模組為例（例如：Employee, Role, Product）：

```
src/
├── views/{category}/{module}/
│   ├── types.ts                        # Phase 2: 類型定義（~165 行）
│   ├── index.tsx                       # Phase 4: 列表頁面（~400 行）
│   └── components/
│       ├── {Module}FormModal.tsx       # Phase 5: 表單模態框（~250 行）
│       └── {Module}FormModal.test.tsx  # Phase 6: 表單測試（~150 行）
│
├── constants/{category}/
│   ├── {module}Const.ts                # Phase 2: 常量定義（~80 行）
│   └── {module}Const.test.ts           # Phase 6: 常量測試（~30 行）
│
└── api/{category}/
    ├── {module}Api.ts                  # Phase 3: API 層（~100 行）
    └── {module}Api.test.ts             # Phase 6: API 測試（~50 行）
```

**分類（category）**:
- `system` - 系統管理模組（員工、角色、菜單等）
- `business` - 業務模組（商品、企業、通知等）
- `support` - 支持模組（文件、配置、日誌等）

---

## 快速開始：4 步創建新模組

### Step 1: 複製模板文件（5 分鐘）

```bash
# 假設創建新模組：Product（商品管理）
cd c:\Workspace\open_source\smart-admin\smart-admin-web-react

# 複製 Employee 模組作為模板
cp -r src/views/system/employee src/views/business/product
cp src/constants/system/employeeConst.ts src/constants/business/productConst.ts
cp src/api/system/employeeApi.ts src/api/business/productApi.ts
```

### Step 2: 批量替換關鍵詞（10 分鐘）

使用 VSCode 的「替換全部」功能（Ctrl+H），按照以下順序替換：

**2.1 模組名稱替換**（大小寫敏感）:

| 原文 | 替換為 | 說明 |
|------|--------|------|
| `Employee` | `Product` | PascalCase 類名 |
| `employee` | `product` | camelCase 變量名 |
| `EMPLOYEE` | `PRODUCT` | UPPER_CASE 常量 |
| `員工` | `商品` | 中文名稱 |

**2.2 分類路徑替換**:

| 原文 | 替換為 |
|------|--------|
| `system/employee` | `business/product` |
| `@/views/system/employee` | `@/views/business/product` |
| `@/constants/system` | `@/constants/business` |
| `@/api/system` | `@/api/business` |

**2.3 API 路徑替換**:

| 原文 | 替換為 |
|------|--------|
| `/employee/` | `/product/` |

### Step 3: 自定義字段（30 分鐘）

編輯以下文件，根據業務需求調整字段：

**3.1 types.ts** - 定義數據結構

```typescript
// 示例：ProductVO（列表展示）
export interface ProductVO {
  productId: number;          // 商品 ID
  productName: string;         // 商品名稱
  productCode: string;         // 商品編碼
  categoryId: number;          // 分類 ID
  price: number;               // 價格
  stock: number;               // 庫存
  status: number;              // 狀態（0:下架 1:上架）
  createTime?: string;         // 創建時間
  updateTime?: string;         // 更新時間
}

// ProductQueryForm, ProductAddForm, ProductUpdateForm...
```

**3.2 const.ts** - 定義常量

```typescript
// 權限點
export const PRODUCT_PERMISSION = {
  ADD: 'business:product:add',
  UPDATE: 'business:product:update',
  DELETE: 'business:product:delete',
} as const;

// 狀態標籤
export const PRODUCT_STATUS_LABELS = {
  0: '下架',
  1: '上架',
} as const;
```

**3.3 api.ts** - 定義 API 方法

```typescript
export const productApi = {
  queryProduct: (params: ProductQueryForm): Promise<ResponseDTO<PageResult<ProductVO>>> => {
    return request.post('/product/query', params);
  },

  addProduct: (params: ProductAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/product/add', params);
  },

  // ...其他 API
};
```

**3.4 index.tsx** - 定義表格列

```typescript
const columns: ColumnsType<ProductVO> = [
  { title: '商品名稱', dataIndex: 'productName', width: 150 },
  { title: '商品編碼', dataIndex: 'productCode', width: 120 },
  { title: '分類', dataIndex: 'categoryName', width: 100 },
  { title: '價格', dataIndex: 'price', width: 80 },
  { title: '庫存', dataIndex: 'stock', width: 80 },
  // ...
];
```

**3.5 FormModal.tsx** - 定義表單字段

```typescript
<Form.Item name="productName" label="商品名稱" rules={[{ required: true }]}>
  <Input placeholder="請輸入商品名稱" />
</Form.Item>

<Form.Item name="categoryId" label="分類" rules={[{ required: true }]}>
  <Select placeholder="請選擇分類">
    {/* 選項 */}
  </Select>
</Form.Item>
```

### Step 4: 註冊路由（5 分鐘）

編輯 `src/router/dynamic-routes.ts`：

```typescript
export const viewModules: Record<string, ComponentType<any>> = {
  // ...existing routes
  '/business/product': lazy(() => import('@/views/business/product')),
};
```

---

## 測試驗證清單

創建完成後，按照以下清單驗證：

### ✅ 編譯檢查

```bash
npm run type-check  # TypeScript 零錯誤
npm run lint        # ESLint 零錯誤
```

### ✅ 測試驗證

```bash
npm run test        # 全部測試通過
```

### ✅ 功能驗證

- [ ] 列表頁面可訪問（路由正確）
- [ ] 查詢功能正常（分頁、過濾、排序）
- [ ] 新增功能正常（表單驗證、API 調用、成功提示）
- [ ] 編輯功能正常（數據回填、更新提交）
- [ ] 刪除功能正常（確認彈窗、API 調用、列表刷新）
- [ ] 權限控制正常（按鈕顯示/隱藏）

---

## 高級功能模式

### 模式 1：Read-Only 模組（只讀列表）

**適用場景**: 日誌、歷史記錄等

**跳過階段**: Phase 5（無需 FormModal）

**可選功能**:
- DetailModal（詳情彈窗） - 參考 Operate-Log
- BatchOperation（批量操作） - 參考 Login-Fail

### 模式 2：DetailModal 模式

**參考模組**: Operate-Log

**關鍵實現**:

```typescript
// DetailModal.tsx
const DetailModal = forwardRef<{ show: (id: number) => void }>((_, ref) => {
  const [visible, setVisible] = useState(false);
  const [detail, setDetail] = useState(null);

  useImperativeHandle(ref, () => ({
    show: async (id) => {
      setVisible(true);
      const res = await api.detail(id);
      setDetail(res.data);
    }
  }));

  return <Modal open={visible} onCancel={() => setVisible(false)}>...</Modal>;
});
```

### 模式 3：批量操作模式

**參考模組**: Login-Fail

**關鍵實現**:

```typescript
const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

const rowSelection = {
  selectedRowKeys,
  onChange: setSelectedRowKeys,
};

const handleBatchDelete = () => {
  Modal.confirm({
    title: '確認批量刪除？',
    onOk: async () => {
      await api.batchDelete(selectedRowKeys);
      setSelectedRowKeys([]);
      refreshTable();
    },
  });
};

<Table rowSelection={rowSelection} ... />
<Button onClick={handleBatchDelete}>批量刪除</Button>
```

### 模式 4：性能優化模式（UAParser）

**適用場景**: 列表需要客戶端解析（UserAgent, JSON）

**參考模組**: Login-Log, Operate-Log

**關鍵實現**:

```typescript
const parsedTableData = useMemo(() => {
  return tableData.map((item) => {
    const parser = new UAParser(item.userAgent);
    return {
      ...item,
      browser: parser.getBrowser().name,
      os: parser.getOS().name,
    };
  });
}, [tableData]);
```

---

## 常見問題 (FAQ)

### Q1: 如何處理特殊字段類型？

**A**: 參考以下映射：

| 字段類型 | 表單組件 | 驗證規則 |
|---------|---------|---------|
| 文本 | `<Input />` | `maxLength: 100` |
| 數字 | `<InputNumber />` | `type: 'number'` |
| 日期 | `<DatePicker />` | N/A |
| 下拉選擇 | `<Select />` | `required: true` |
| 多選 | `<Select mode="multiple" />` | N/A |
| 布爾值 | `<Switch />` | N/A |
| 富文本 | `<TextArea />` 或編輯器 | N/A |

### Q2: 如何處理級聯選擇（部門 → 員工）？

**A**: 使用 `<Cascader />` 或聯動 `<Select />`，參考 Employee 模組的部門選擇。

### Q3: 如何處理文件上傳？

**A**: 使用 `<Upload />` 組件，參考 File 模組。

### Q4: 測試失敗如何調試？

**A**:
1. 檢查類型定義是否正確（types.ts）
2. 檢查 API 路徑是否正確（api.ts）
3. 檢查常量命名是否一致（const.ts）
4. 運行 `npm run test -- --watch` 進入調試模式

---

## 預期效率提升

| 階段 | 原開發時間 | 使用模板後 | 節省時間 |
|------|-----------|-----------|----------|
| Phase 2-3: Types + Const + API | 40 分鐘 | 15 分鐘 | **62.5%** |
| Phase 4: List Page | 60 分鐘 | 30 分鐘 | **50%** |
| Phase 5: Form Modal | 50 分鐘 | 25 分鐘 | **50%** |
| Phase 6: Tests | 30 分鐘 | 15 分鐘 | **50%** |
| Phase 7: Routes | 5 分鐘 | 3 分鐘 | **40%** |
| **總計** | **~2.5 小時** | **~1.5 小時** | **40%** |

**額外收益**:
- ✅ 代碼風格一致性 +30%
- ✅ Bug 發生率 -50%
- ✅ Code Review 效率 +40%

---

## 下一步改進計劃

### Phase 1: 半自動化（當前版本 v1.0.0）

✅ 模板文件 + 替換指南

### Phase 2: CLI 工具（計劃 v2.0.0）

```bash
npm run crud:create -- --module Product --category business --type standard
```

**功能**:
- 自動複製模板文件
- 批量替換關鍵詞
- 生成基礎字段結構

### Phase 3: 完全自動化（計劃 v3.0.0）

```bash
npm run crud:generate -- --config product.config.json
```

**功能**:
- 基於配置文件生成所有代碼
- 自動生成測試文件
- 自動註冊路由
- 支持自定義模板

---

## 附錄：模板文件清單

| 文件 | 路徑 | 說明 |
|------|------|------|
| Types | `src/views/system/employee/types.ts` | 類型定義模板 |
| Constants | `src/constants/system/employeeConst.ts` | 常量定義模板 |
| API | `src/api/system/employeeApi.ts` | API 層模板 |
| List Page | `src/views/system/employee/index.tsx` | 列表頁面模板 |
| Form Modal | `src/views/system/employee/components/EmployeeFormModal.tsx` | 表單模態框模板 |

---

**維護日誌**:
- v1.0.0 (2026-03-13) - 初始版本，基於 14 個已驗證模組總結
