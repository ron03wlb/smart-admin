# SmartAdmin Vue to React 遷移實施計畫

**計畫版本**: 1.0.0
**創建日期**: 2026-03-04
**預計週期**: 12週（150人日）
**團隊規模**: 平均2.5人

---

## 📊 執行摘要

### 遷移目標

將SmartAdmin前端從Vue 3.4.27全面遷移至React 18，保持所有功能完整性的同時優化架構設計，為未來擴展奠定基礎。

### 關鍵指標

| 指標 | 當前（Vue） | 目標（React） |
|------|------------|--------------|
| 總文件數 | 195個.vue文件 | 195個.tsx文件 |
| 代碼行數 | ~50,000行 | ~48,000行（優化後） |
| 首屏加載 | 3.2s | < 2s |
| 單元測試覆蓋率 | 未知 | 75%+ |
| 技術債務 | 中等 | 低 |

### 核心決策

- **遷移策略**: 增量遷移（Incremental Migration）
- **狀態管理**: Redux Toolkit（替代Pinia）
- **UI組件庫**: Ant Design 5.x（對應Ant Design Vue 4.x）
- **構建工具**: Vite 5.x（保持不變）
- **發布方式**: 灰度發布（10% → 50% → 100%流量切換）

---

## 🔍 現況分析

### 技術棧現況

```json
{
  "框架": "Vue 3.4.27 (100% Composition API)",
  "構建工具": "Vite 5.2.12",
  "UI庫": "Ant Design Vue 4.2.5",
  "狀態管理": "Pinia 2.1.7",
  "路由": "Vue Router 4.3.2",
  "HTTP客戶端": "Axios 1.6.8",
  "加密": "sm-crypto 0.3.13",
  "國際化": "vue-i18n 9.13.1"
}
```

### 代碼規模統計

| 類別 | 數量 | 說明 |
|------|------|------|
| Vue文件 | 195個 | 126個views + 32個components + 其他 |
| TypeScript文件 | 122個 | API層、工具函數 |
| Pinia Stores | 8個 | user, dict, role, tenant等 |
| 代碼行數 | ~50,000行 | 估算值 |
| 最大組件 | 529行 | goods-list.vue |

### 複雜度矩陣

| 組件名稱 | 行數 | 複雜度等級 | 遷移難度 | 優先級 |
|---------|------|-----------|---------|--------|
| goods-list.vue | 529行 | ⭐⭐⭐⭐⭐ | 極高 | P0 |
| employee-list.vue | 412行 | ⭐⭐⭐⭐⭐ | 極高 | P0 |
| job-list.vue | 379行 | ⭐⭐⭐⭐ | 很高 | P1 |
| header-setting.vue | 375行 | ⭐⭐⭐⭐ | 很高 | P1 |
| side-layout.vue | 368行 | ⭐⭐⭐⭐ | 很高 | P1 |

### 關鍵技術挑戰

| 挑戰項 | 複雜度 | 遷移難度 | 優先級 | 風險等級 |
|--------|--------|----------|--------|----------|
| v-privilege指令→React HOC | 🔴高 | ⭐⭐⭐⭐ | P0 | 高 |
| Pinia→Redux Toolkit | 🔴高 | ⭐⭐⭐⭐⭐ | P0 | 極高 |
| 動態路由生成 | 🟡中 | ⭐⭐⭐ | P0 | 中 |
| defineExpose→useImperativeHandle | 🟢低 | ⭐⭐ | P1 | 低 |
| watch→useEffect | 🟡中 | ⭐⭐⭐ | P1 | 中 |
| Keep-alive緩存 | 🔴高 | ⭐⭐⭐⭐ | P2 | 中 |

---

## 🎯 技術架構設計

### React技術棧選型

#### 核心依賴

```json
{
  "react": "^18.3.0",
  "react-dom": "^18.3.0",
  "typescript": "^5.6.3",
  "@reduxjs/toolkit": "^2.2.0",
  "react-redux": "^9.1.0",
  "redux-persist": "^6.0.0",
  "antd": "^5.22.0",
  "@ant-design/icons": "^5.5.0",
  "@ant-design/pro-components": "^2.8.0",
  "react-router-dom": "^6.28.0",
  "axios": "^1.6.8",
  "swr": "^2.2.0",
  "vite": "^5.2.12",
  "@vitejs/plugin-react-swc": "^3.7.0"
}
```

#### 選型理由

| 技術 | 選型理由 |
|------|----------|
| **React 18** | Concurrent Features、與Vue 3 Composition API概念相似 |
| **Redux Toolkit** | 與Pinia概念對應、企業應用成熟度最高 |
| **Ant Design 5.x** | 與Ant Design Vue 4.x組件85%+直接映射 |
| **React Router 6** | loader/action模式支持動態路由生成 |
| **Vite** | 保留現有構建工具，降低遷移成本 |

### 項目結構設計

```
smart-admin-web-react/
├── public/                          # 靜態資源
├── src/
│   ├── api/                         # API層（保留Vue結構）
│   │   ├── base/
│   │   │   ├── request.ts           # Axios配置
│   │   │   ├── response.model.ts    # ResponseDTO接口
│   │   │   └── page.model.ts        # PageResult接口
│   │   ├── business/                # 業務API
│   │   ├── support/                 # 支撐模塊API
│   │   └── system/                  # 系統模塊API
│   ├── components/                  # 通用組件
│   │   ├── business/                # 業務組件
│   │   ├── framework/               # 框架組件
│   │   │   ├── SmartEnumSelect/     # 枚舉選擇器
│   │   │   ├── TableOperator/       # 表格操作欄
│   │   │   └── PrivilegeButton/     # 權限按鈕
│   │   └── support/                 # 支撐組件
│   ├── hooks/                       # 自定義Hooks
│   │   ├── useTable.ts              # 表格Hook
│   │   ├── useModal.ts              # Modal Hook
│   │   ├── usePrivilege.ts          # 權限Hook
│   │   └── usePagination.ts         # 分頁Hook
│   ├── store/                       # Redux Store
│   │   ├── index.ts                 # Store配置
│   │   └── slices/                  # Redux Slices
│   │       ├── userSlice.ts         # 用戶狀態
│   │       ├── appConfigSlice.ts    # 應用配置
│   │       ├── dictSlice.ts         # 數據字典
│   │       └── tenantSlice.ts       # 多租戶
│   ├── router/                      # 路由配置
│   │   ├── index.tsx                # 路由入口
│   │   ├── routes.tsx               # 靜態路由
│   │   └── dynamic-routes.ts        # 動態路由生成
│   ├── views/                       # 頁面組件
│   │   ├── business/                # 業務模塊
│   │   ├── support/                 # 支撐模塊
│   │   └── system/                  # 系統模塊
│   ├── App.tsx                      # 根組件
│   └── main.tsx                     # 入口文件
├── tests/                           # 測試文件
│   ├── unit/                        # 單元測試
│   ├── integration/                 # 集成測試
│   └── e2e/                         # E2E測試
├── vite.config.ts                   # Vite配置
└── package.json                     # 依賴管理
```

---

## 🔄 關鍵技術映射

### Vue特性 → React等效方案

| Vue特性 | React等效 | 代碼示例 |
|---------|-----------|----------|
| `ref` | `useState` | `const [count, setCount] = useState(0)` |
| `reactive` | `useState` (對象) | `const [form, setForm] = useState({})` |
| `computed` | `useMemo` | `const total = useMemo(() => sum(items), [items])` |
| `watch` | `useEffect` | `useEffect(() => { log(count) }, [count])` |
| `onMounted` | `useEffect(..., [])` | `useEffect(() => { init() }, [])` |
| `v-if` | `&&` | `{visible && <Modal />}` |
| `v-for` | `map()` | `{list.map(item => <Item key={item.id} />)}` |
| `v-model` | `value + onChange` | `<Input value={val} onChange={e => set(e.target.value)} />` |
| `defineExpose` | `useImperativeHandle` | `useImperativeHandle(ref, () => ({ open }))` |

### v-privilege指令 → React權限控制

#### Vue自定義指令（現有）

```vue
<a-button v-privilege="'goods:add'" type="primary">新建</a-button>
```

#### React實現方案

**Hook實現**:

```typescript
// hooks/usePrivilege.ts
import { useAppSelector } from '@/store/hooks';

export function usePrivilege(permission: string): boolean {
  const administratorFlag = useAppSelector(state => state.user.administratorFlag);
  const pointList = useAppSelector(state => state.user.pointsList);

  if (administratorFlag) return true;

  return pointList.some(point => point.webPerms === permission);
}
```

**組件實現**:

```typescript
// components/framework/PrivilegeButton.tsx
import { Button, ButtonProps } from 'antd';
import { usePrivilege } from '@/hooks/usePrivilege';

interface PrivilegeButtonProps extends ButtonProps {
  privilege: string;
}

export default function PrivilegeButton({
  privilege,
  children,
  ...props
}: PrivilegeButtonProps) {
  const hasPrivilege = usePrivilege(privilege);

  if (!hasPrivilege) return null;

  return <Button {...props}>{children}</Button>;
}
```

**使用示例**:

```tsx
<PrivilegeButton privilege="goods:add" type="primary">
  新建
</PrivilegeButton>
```

### Pinia Store → Redux Toolkit遷移

#### Vue Pinia Store（現有）

```typescript
// store/modules/system/user.ts
export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    token: '',
    employeeId: '',
    menuTree: [],
  }),
  getters: {
    getToken(state) {
      return state.token || localRead(localKey.USER_TOKEN);
    },
  },
  actions: {
    logout() {
      this.token = '';
      localRemove(localKey.USER_TOKEN);
    },
  },
});
```

#### React Redux Toolkit Slice

```typescript
// store/slices/userSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserState {
  token: string;
  employeeId: string;
  menuTree: MenuItem[];
}

const initialState: UserState = {
  token: '',
  employeeId: '',
  menuTree: [],
};

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload;
    },
    logout: (state) => {
      state.token = '';
      localStorage.removeItem('USER_TOKEN');
    },
  },
});

// Selectors
export const selectToken = (state: RootState) =>
  state.user.token || localStorage.getItem('USER_TOKEN') || '';

export const { setToken, logout } = userSlice.actions;
export default userSlice.reducer;
```

#### 組件使用對比

**Vue Composition API**:

```vue
<script setup>
import { useUserStore } from '@/store/modules/system/user';

const userStore = useUserStore();
const token = computed(() => userStore.getToken);

const handleLogout = () => {
  userStore.logout();
};
</script>
```

**React Hooks**:

```tsx
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectToken, logout } from '@/store/slices/userSlice';

export default function Header() {
  const token = useAppSelector(selectToken);
  const dispatch = useAppDispatch();

  const handleLogout = () => {
    dispatch(logout());
  };

  return <Button onClick={handleLogout}>登出</Button>;
}
```

---

## 🛠️ CRUD模式實現

### useTable Hook設計

```typescript
// hooks/useTable.ts
import { useState, useCallback } from 'react';
import { message } from 'antd';
import { PageResult } from '@/api/base/page.model';

interface UseTableOptions<T> {
  queryApi: (params: any) => Promise<PageResult<T>>;
  deleteApi?: (id: number) => Promise<void>;
  exportApi?: (params: any) => Promise<void>;
}

export function useTable<T = any>(options: UseTableOptions<T>) {
  const { queryApi, deleteApi, exportApi } = options;

  const [dataSource, setDataSource] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });

  const query = useCallback(async (params = {}) => {
    setLoading(true);
    try {
      const res = await queryApi({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        ...params,
      });
      setDataSource(res.data.list);
      setTotal(res.data.total);
    } catch (error) {
      message.error('查詢失敗');
    } finally {
      setLoading(false);
    }
  }, [queryApi, pagination]);

  const deleteRow = useCallback(async (id: number) => {
    if (!deleteApi) return;

    try {
      await deleteApi(id);
      message.success('刪除成功');
      query();
    } catch (error) {
      message.error('刪除失敗');
    }
  }, [deleteApi, query]);

  const batchDelete = useCallback(async (ids: number[]) => {
    if (!deleteApi) return;

    await Promise.all(ids.map(id => deleteApi(id)));
    message.success(`成功刪除${ids.length}條記錄`);
    query();
  }, [deleteApi, query]);

  return {
    dataSource,
    loading,
    total,
    pagination,
    query,
    deleteRow,
    batchDelete,
    setPagination,
  };
}
```

### Form Modal模式設計

```typescript
// hooks/useModal.ts
import { useState, useCallback } from 'react';

export function useModal<T = any>() {
  const [visible, setVisible] = useState(false);
  const [editData, setEditData] = useState<T | null>(null);

  const open = useCallback((data?: T) => {
    setEditData(data || null);
    setVisible(true);
  }, []);

  const close = useCallback(() => {
    setVisible(false);
    setEditData(null);
  }, []);

  return {
    visible,
    editData,
    open,
    close,
  };
}
```

---

## 📅 分階段實施計畫

### Phase 1: Foundation & POC（Week 1-2）

#### 目標
完成React項目初始化，驗證技術棧可行性

#### 關鍵任務

**Week 1: 項目搭建與技術驗證**
- Day 1: Vite + React + TypeScript腳手架搭建
- Day 1-2: 核心依賴安裝（Redux Toolkit、Ant Design、React Router）
- Day 2-3: API層封裝（axios配置、ResponseDTO類型定義）
- Day 4-5: 權限系統POC（usePrivilege Hook、PrivilegeButton組件）

**Week 2: 核心功能遷移**
- Day 1-2: 登錄頁遷移（LoginForm、Sa-Token集成）
- Day 3: 首頁遷移（快捷入口、通知顯示）
- Day 4-5: 菜單系統（側邊菜單、動態路由生成、TagNav）

#### 交付物
- ✅ 可運行的React項目（http://localhost:8082）
- ✅ 權限系統POC Demo
- ✅ 完整的登錄流程（登錄→首頁→菜單）

#### 驗收標準
- ✅ 用戶可成功登錄並跳轉首頁
- ✅ 側邊菜單正確渲染（支持3層嵌套）
- ✅ 權限按鈕正確顯示/隱藏

---

### Phase 2: Core Infrastructure（Week 3-4）

#### 目標
完成SmartAdmin常用組件和狀態管理遷移

#### 關鍵任務

**Week 3: 通用組件庫建設**
- Day 1-2: 表單組件（SmartEnumSelect、DictSelect、CategoryTreeSelect）
- Day 3: 表格組件（TableOperator、useTable Hook）
- Day 4-5: 業務組件（FileUpload、SmartLoading、EmployeeSelect）

**Week 4: 狀態管理與國際化**
- Day 1-3: Redux Slices遷移（appConfig、dict、role、tenant、spin）
- Day 4: 國際化（react-i18next配置、語言包遷移）
- Day 5: Keep-alive機制（KeepAliveOutlet組件）

#### 交付物
- ✅ 10+個通用組件（Storybook文檔）
- ✅ 8個Redux Slices（對應Pinia stores）
- ✅ 國際化切換功能

#### 驗收標準
- ✅ 所有狀態管理邏輯與Vue版本一致
- ✅ 語言切換無刷新生效
- ✅ 頁面緩存正確工作

---

### Phase 3: Component Migration（Week 5-8）

#### 目標
完成所有195個Vue頁面遷移

#### Week 5: System模塊遷移（70%頁面）

**優先級排序**:
1. **P0**: employee-list (412行)、menu-list (278行)
2. **P1**: role-list、department-list
3. **P2**: login-log、operate-log

**關鍵任務**:
- Day 1-2: 員工管理（EmployeeList、EmployeeFormModal）
- Day 3-4: 菜單管理（MenuList、MenuOperateModal）
- Day 5: 角色管理（RoleList、權限樹選擇器）

#### Week 6: Business模塊遷移

**優先級排序**:
1. **P0**: goods-list (529行)、enterprise-list (288行)
2. **P1**: notice-list (358行)

**關鍵任務**:
- Day 1-3: 商品管理（GoodsList、分類樹、導入/導出）
- Day 4-5: 企業管理（EnterpriseList、EnterpriseFormDrawer）

#### Week 7: Support模塊遷移

**優先級排序**:
1. **P0**: job-list (379行)、file-list (296行)
2. **P1**: change-log-list (326行)、help-doc (328行)

**關鍵任務**:
- Day 1-2: 任務調度（JobList、Cron表達式組件）
- Day 3-4: 文件管理（FileList、文件預覽）
- Day 5: 變更日誌（ChangeLogList、Markdown編輯器）

#### Week 8: 剩餘頁面遷移（~30個）

**任務分配**:
- Developer A: OA模塊（notice、invoice、bank）
- Developer B: 系統模塊（login-log、operate-log、config）
- Developer C: 支撐模塊（feedback、dict、data-tracer）

#### 交付物
- ✅ 100%頁面遷移完成
- ✅ 全量E2E測試套件
- ✅ 遷移完成報告

---

### Phase 4: Integration & Testing（Week 9-10）

#### Week 9: 集成測試與Bug修復

**關鍵任務**:
- Day 1-2: 功能測試（權限系統、CRUD流程、導入/導出）
- Day 3: 兼容性測試（Chrome、Edge、Firefox、Safari）
- Day 4-5: 性能測試（首屏加載、表格渲染、內存洩漏）

#### Week 10: UAT與文檔完善

**關鍵任務**:
- Day 1-3: UAT測試（產品經理、測試團隊驗收）
- Day 4-5: 文檔編寫（開發者文檔、遷移指南、部署文檔）

#### 交付物
- ✅ 測試報告（功能、兼容性、性能）
- ✅ UAT通過報告
- ✅ 完整技術文檔

---

### Phase 5: Optimization & Deployment（Week 11-12）

#### Week 11: 性能優化

**優化清單**:
- Day 1-2: 代碼拆分（路由懶加載、第三方庫拆分）
- Day 3: 資源優化（圖片壓縮、字體子集化、CDN）
- Day 4-5: 運行時優化（useMemo、虛擬列表、防抖/節流）

**性能目標**:
| 指標 | Vue版本 | React目標 |
|------|---------|-----------|
| 首屏加載 | 3.2s | < 2s |
| TTI | 4.5s | < 3s |
| 表格渲染（1000行） | 1.8s | < 1s |

#### Week 12: 灰度發布與監控

**關鍵任務**:
- Day 1-2: 灰度發布（10%流量→React）
- Day 3-4: 監控與告警（Sentry、Web Vitals）
- Day 5: 全量發布（100%流量切換）

#### 交付物
- ✅ 生產環境React前端
- ✅ 監控大盤（Grafana）
- ✅ 發布報告

---

## ⏱️ 工作量估算

### 時間估算（12週 = 60工作日）

| Phase | 週數 | 工作日 | FTE | 總人日 |
|-------|------|--------|-----|--------|
| Phase 1（Foundation） | 2週 | 10天 | 2人 | 20人日 |
| Phase 2（Infrastructure） | 2週 | 10天 | 2人 | 20人日 |
| Phase 3（Migration） | 4週 | 20天 | 3人 | 60人日 |
| Phase 4（Testing） | 2週 | 10天 | 3人 | 30人日 |
| Phase 5（Optimization） | 2週 | 10天 | 2人 | 20人日 |
| **總計** | **12週** | **60天** | **2.5人** | **150人日** |

### 團隊組成

| 角色 | 人數 | 技能要求 | 職責 |
|------|------|----------|------|
| React高級工程師 | 1人 | React 18、Redux Toolkit、TypeScript | 架構設計、核心組件開發 |
| React工程師 | 2人 | React基礎、Ant Design | 頁面遷移、業務邏輯開發 |
| 測試工程師 | 1人 | Jest、Playwright、性能測試 | 測試用例編寫、Bug跟蹤 |
| DevOps工程師 | 0.5人 | Nginx、Docker、CI/CD | 部署配置、監控設置 |

### 關鍵里程碑

| 里程碑 | 時間節點 | 交付標準 | 風險等級 |
|--------|----------|----------|----------|
| M1: POC完成 | Week 2 | 登錄頁+首頁+權限系統可用 | 🟢低 |
| M2: 核心組件完成 | Week 4 | 10+個通用組件+8個Redux Slices | 🟡中 |
| M3: 50%頁面遷移 | Week 6 | system+business模塊核心頁面 | 🟡中 |
| M4: 100%頁面遷移 | Week 8 | 所有195個Vue頁面遷移完成 | 🔴高 |
| M5: UAT通過 | Week 10 | 功能、性能測試通過 | 🟡中 |
| M6: 生產發布 | Week 12 | React前端100%流量 | 🔴高 |

---

## ⚠️ 風險與應對措施

### 技術風險

| 風險項 | 概率 | 影響 | 風險等級 | 應對措施 |
|--------|------|------|----------|----------|
| Redux Toolkit學習曲線導致進度延誤 | 中 | 高 | 🔴高 | 提前2週團隊培訓、提供Pinia→RTK對照表 |
| Ant Design 5.x破壞性變更 | 低 | 中 | 🟡中 | 提前閱讀Migration Guide、POC驗證 |
| 動態路由生成邏輯復雜 | 中 | 高 | 🔴高 | Week 2完成POC、編寫集成測試 |
| Keep-alive機制實現復雜 | 高 | 中 | 🟡中 | 使用react-activation庫、降級方案 |
| 性能不達標（首屏>3s） | 中 | 高 | 🔴高 | Week 11專項優化、Lighthouse CI |

### 業務風險

| 風險項 | 概率 | 影響 | 風險等級 | 應對措施 |
|--------|------|------|----------|----------|
| 功能遺漏導致業務中斷 | 中 | 極高 | 🔴極高 | 完整功能清單、UAT測試 |
| 灰度發布期間用戶體驗不一致 | 高 | 中 | 🟡中 | Nginx基於Cookie劃分 |
| 數據不一致（localStorage格式變更） | 中 | 高 | 🔴高 | 提供遷移腳本、兼容舊數據 |

### 應急預案

#### 預案A: 進度延誤（延遲>2週）
**觸發條件**: Week 6時頁面遷移進度<40%

**應對措施**:
1. 增加1名React工程師
2. 降低非核心頁面優先級（P2頁面延後）
3. 延長Phase 3至6週

#### 預案B: 性能不達標
**觸發條件**: Week 11性能測試首屏>3s

**應對措施**:
1. 引入Web Worker處理計算密集型任務
2. 使用Server-Side Rendering（Next.js）
3. 降級方案：僅優化P0頁面

#### 預案C: 灰度發布失敗
**觸發條件**: 錯誤率>1%或用戶投訴>10件/天

**應對措施**:
1. Nginx立即切回Vue前端（100%流量）
2. 分析Sentry錯誤日誌
3. 修復後重新灰度（5%流量）

---

## 🧪 測試策略

### 測試覆蓋率目標

| 層級 | 目標覆蓋率 | 說明 |
|------|-----------|------|
| Hooks | 90% | 核心業務邏輯，高覆蓋率 |
| Components | 80% | UI組件，重點測試交互 |
| Utils | 95% | 工具函數，純函數易測試 |
| Pages | 60% | E2E為主，單元測試為輔 |
| **Overall** | **75%+** | 整體項目覆蓋率 |

### 單元測試（Jest + React Testing Library）

**配置**:

```json
{
  "scripts": {
    "test": "jest --coverage",
    "test:watch": "jest --watch"
  }
}
```

**測試示例**:

```typescript
// hooks/usePrivilege.test.ts
describe('usePrivilege', () => {
  it('should return true for administrator', () => {
    const { result } = renderHook(() => usePrivilege('goods:add'));
    expect(result.current).toBe(true);
  });
});
```

### E2E測試（Playwright）

**測試示例**:

```typescript
// tests/e2e/goods-crud.spec.ts
test('should create new goods', async ({ page }) => {
  await page.goto('/business/goods/goods-list');
  await page.click('button:has-text("新建")');
  await page.fill('input[name="goodsName"]', '測試商品');
  await page.click('button:has-text("提交")');
  await expect(page.locator('.ant-message-success')).toBeVisible();
});
```

---

## 📚 驗證計畫

### 端到端驗證流程

1. **登錄驗證**
   - 用戶名/密碼登錄
   - Token存儲和攜帶
   - 登錄失效跳轉

2. **權限驗證**
   - v-privilege指令替換（PrivilegeButton組件）
   - 菜單權限控制
   - 按鈕權限控制

3. **CRUD驗證**（以商品管理為例）
   - 列表查詢（分頁、排序、篩選）
   - 新增商品（表單驗證、API調用）
   - 編輯商品（數據回填、更新提交）
   - 刪除商品（確認彈窗、批量刪除）
   - 導入/導出（Excel文件處理）

4. **狀態管理驗證**
   - Redux Store持久化（redux-persist）
   - 跨組件狀態共享
   - 狀態更新響應式

5. **路由驗證**
   - 動態路由生成（基於後端菜單數據）
   - 路由守衛（登錄驗證）
   - Keep-alive緩存（頁面緩存恢復）

6. **國際化驗證**
   - 語言切換（中文/英文）
   - Ant Design組件國際化
   - 日期格式本地化

7. **性能驗證**
   - 首屏加載時間 < 2s
   - 表格渲染1000行 < 1s
   - 內存佔用 < 100MB

---

## 📖 關鍵文件清單

基於此遷移計畫，以下是最關鍵的5個文件：

1. **smart-admin-web/src/store/modules/system/user.ts** (365行)
   - 最複雜的Pinia Store
   - 需優先遷移為Redux Toolkit Slice
   - 包含登錄邏輯、菜單樹構建、權限管理

2. **smart-admin-web/src/directives/privilege.ts**
   - v-privilege自定義指令（95+處使用）
   - 需轉換為React usePrivilege Hook和PrivilegeButton組件

3. **smart-admin-web/src/router/index.ts**
   - 動態路由生成邏輯
   - 需改寫為React Router 6 loader模式

4. **smart-admin-web/src/lib/axios.ts**
   - Axios配置和攔截器
   - 需保持與React版本一致（Token、多租戶headers、ResponseDTO處理）

5. **smart-admin-web/src/views/business/erp/goods/goods-list.vue** (529行)
   - 最大的業務組件
   - 作為CRUD模式遷移的典型示例
   - 包含表格、表單、權限、導入/導出

---

## ✅ 成功關鍵因素

1. **團隊準備**: React培訓、Pinia→RTK對照表
2. **分階段驗證**: 每個Phase都有明確的交付物和驗收標準
3. **完整測試**: 75%+代碼覆蓋率、E2E測試覆蓋核心流程
4. **灰度發布**: 10%→50%→100%流量切換
5. **監控告警**: Sentry錯誤監控、Web Vitals性能監控

---

## 📝 下一步行動

### 立即行動（本週內）

1. ✅ 創建feature/vue-to-react-migration分支
2. ✅ 項目初始化（Vite腳手架）
3. ✅ 團隊培訓（Redux Toolkit Workshop）

### Week 1 關鍵任務

- **Day 1**: Vite + React + TypeScript腳手架搭建
- **Day 2**: Redux Store配置、API層封裝
- **Day 3**: usePrivilege Hook POC
- **Day 4**: PrivilegeButton組件實現
- **Day 5**: 權限系統集成測試

### Week 2 里程碑

- **M1: POC完成** - 登錄頁+首頁+權限系統可用

---

**計畫批准**: ⬜ 待批准
**計畫執行**: ⬜ 待開始
**計畫版本**: 1.0.0
