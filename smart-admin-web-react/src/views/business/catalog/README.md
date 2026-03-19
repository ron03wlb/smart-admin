# Catalog Module (目錄模塊)

## 概述

Catalog 模塊提供不同類型的分類管理功能，是 Vue 版本 `smart-admin-web/src/views/business/erp/catalog/` 的 React 遷移版本。

## 架構設計

### 模塊結構

```
catalog/
├── goods-catalog/         # 商品分類管理
│   └── index.tsx
├── custom-catalog/        # 自定義分類管理（演示用途）
│   └── index.tsx
└── README.md
```

### 核心組件複用

Catalog 模塊使用包裝器模式（Wrapper Pattern），復用 `business/category` 模塊的 `CategoryTreeTable` 組件：

```typescript
// goods-catalog/index.tsx
export default function GoodsCatalog() {
  return <CategoryTreeTable categoryType={CategoryTypeEnum.GOODS} />;
}

// custom-catalog/index.tsx
export default function CustomCatalog() {
  return <CategoryTreeTable categoryType={CategoryTypeEnum.DEMO} privilegePrefix="custom:" />;
}
```

## 子模塊說明

### 1. GoodsCatalog（商品分類）

**路徑**: `catalog/goods-catalog/index.tsx`

**描述**: 商品分類管理頁面，用於管理商品的分類層級結構。

**配置**:
- `categoryType`: `CategoryTypeEnum.GOODS` (值為 1)
- `privilegePrefix`: 默認空字符串（使用標準權限 `category:add`, `category:update` 等）

**Vue 源碼參考**: `smart-admin-web/src/views/business/erp/catalog/goods-catalog.vue`

### 2. CustomCatalog（自定義分類）

**路徑**: `catalog/custom-catalog/index.tsx`

**描述**: 自定義分類管理頁面，用於演示目的，展示如何使用自定義權限前綴。

**配置**:
- `categoryType`: `CategoryTypeEnum.DEMO` (值為 2)
- `privilegePrefix`: `"custom:"` （權限變為 `custom:category:add`, `custom:category:update` 等）

**Vue 源碼參考**: `smart-admin-web/src/views/business/erp/catalog/custom-catalog.vue`

## CategoryTreeTable 組件

Catalog 模塊依賴 `business/category/index.tsx` 中的 `CategoryTreeTable` 組件。

### Props

| Prop | 類型 | 必填 | 默認值 | 說明 |
|------|------|------|--------|------|
| `categoryType` | `CategoryTypeEnum` | 是 | - | 分類類型（1:商品, 2:演示） |
| `privilegePrefix` | `string` | 否 | `""` | 權限前綴，用於自定義權限控制 |

### 功能特性

1. **樹形表格展示**: 使用 Ant Design Table 的樹形結構展示分類層級
2. **CRUD 操作**:
   - 新建分類
   - 增加子分類
   - 編輯分類
   - 刪除分類
3. **權限控制**: 使用 `usePrivilege` Hook 進行細粒度權限檢查
4. **響應式佈局**: 支持表格橫向滾動（scroll={{ x: 1000 }}）

## 使用示例

### 在路由中使用

```typescript
// router configuration
{
  path: '/business/catalog/goods',
  component: () => import('@/views/business/catalog/goods-catalog'),
  meta: { title: '商品分類' }
},
{
  path: '/business/catalog/custom',
  component: () => import('@/views/business/catalog/custom-catalog'),
  meta: { title: '自定義分類' }
}
```

### 直接使用 CategoryTreeTable

如果需要自定義其他類型的分類，可以直接使用 `CategoryTreeTable`:

```typescript
import { CategoryTreeTable } from '@/views/business/category';
import { CategoryTypeEnum } from '@/views/business/category/types';

// 示例：創建一個服務分類
export default function ServiceCatalog() {
  return (
    <CategoryTreeTable
      categoryType={3}  // 假設 3 代表服務分類
      privilegePrefix="service:"
    />
  );
}
```

## API 依賴

Catalog 模塊使用以下 API（來自 `@/api/business/categoryApi`）:

- `queryCategoryTree(params)`: 查詢分類樹
- `addCategory(form)`: 新增分類
- `updateCategory(form)`: 更新分類
- `deleteCategory(categoryId)`: 刪除分類

## 權限配置

### 商品分類權限（GoodsCatalog）

- `category:add` - 新建分類
- `category:addChild` - 增加子分類
- `category:update` - 編輯分類
- `category:delete` - 刪除分類

### 自定義分類權限（CustomCatalog）

- `custom:category:add` - 新建分類
- `custom:category:addChild` - 增加子分類
- `custom:category:update` - 編輯分類
- `custom:category:delete` - 刪除分類

## 數據結構

參考 `business/category/types.ts`:

```typescript
export interface CategoryVO {
  categoryId: number;
  categoryName: string;
  categoryType: number;
  parentId?: number;
  sort?: number;
  remark?: string;
  disabledFlag: boolean;
  createTime?: string;
  updateTime?: string;
  children?: CategoryVO[];  // 樹形結構子節點
}

export enum CategoryTypeEnum {
  GOODS = 1,  // 商品分類
  DEMO = 2,   // 演示分類
}
```

## 與 Vue 版本的差異

1. **組件複用**: React 版本使用包裝器模式復用核心組件，而 Vue 版本每個頁面都渲染獨立的 `CategoryTreeTable` 組件
2. **類型安全**: React 版本使用 TypeScript 提供完整的類型定義和類型檢查
3. **權限控制**: React 使用 `usePrivilege` Hook，Vue 使用 `v-privilege` 指令
4. **狀態管理**: React 使用 `useState` + `useEffect`，Vue 使用 Composition API `ref` + `reactive`

## 測試

測試文件位於:
- `catalog/goods-catalog/index.test.tsx` (待實現)
- `catalog/custom-catalog/index.test.tsx` (待實現)

核心邏輯的測試已在 `business/category` 模塊中涵蓋。

## 遷移狀態

| Vue 文件 | React 文件 | 狀態 |
|----------|-----------|------|
| `catalog/goods-catalog.vue` | `catalog/goods-catalog/index.tsx` | ✅ 完成 |
| `catalog/custom-catalog.vue` | `catalog/custom-catalog/index.tsx` | ✅ 完成 |
| `catalog/components/category-tree-table.vue` | `category/index.tsx` (CategoryTreeTable) | ✅ 完成 |
| `catalog/components/category-form-modal.vue` | `category/components/CategoryFormModal.tsx` | ✅ 完成 |

## 後續優化建議

1. **測試覆蓋**: 添加 GoodsCatalog 和 CustomCatalog 的單元測試
2. **E2E 測試**: 添加分類 CRUD 完整流程的端到端測試
3. **性能優化**: 對於大量分類數據，可考慮添加虛擬滾動或分頁加載
4. **拖拽排序**: 可選功能 - 支持拖拽調整分類順序

## 參考文檔

- [Category 模塊文檔](../category/README.md) (待創建)
- [SmartAdmin 分類管理 API](../../api/business/categoryApi.ts)
- [Ant Design Tree Table](https://ant.design/components/table-cn/#components-table-demo-tree-data)
