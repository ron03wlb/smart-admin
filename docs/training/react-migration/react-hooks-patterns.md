# React Hooks 實戰模式

**培訓時間**: 1.5 小時
**培訓對象**: 參與 React 遷移的前端工程師
**先修知識**: 基礎 React、TypeScript

---

## 📋 培訓目標

- ✅ 掌握 React Hooks 基本使用（useState、useEffect、useRef）
- ✅ 理解 Hooks 依賴項管理
- ✅ 學會性能優化 Hooks（useMemo、useCallback）
- ✅ 編寫自定義 Hooks（useTable、useModal、usePrivilege）
- ✅ 避免常見 Hooks 陷阱

---

## 第一部分：基礎 Hooks（30分鐘）

### 1.1 useState vs Vue ref/reactive

**Vue Composition API**:
```vue
<script setup>
import { ref, reactive } from 'vue';

const count = ref(0);
const user = reactive({ name: '', age: 0 });

const increment = () => {
  count.value++;
};

const updateUser = () => {
  user.name = 'Alice';
  user.age = 25;
};
</script>
```

**React Hooks**:
```typescript
import { useState } from 'react';

export default function Counter() {
  const [count, setCount] = useState(0);
  const [user, setUser] = useState({ name: '', age: 0 });

  const increment = () => {
    setCount(count + 1);
    // 或使用函數式更新
    setCount((prev) => prev + 1);
  };

  const updateUser = () => {
    // ❌ 不能直接修改（Redux 禁止 mutation）
    // user.name = 'Alice';

    // ✅ 正確方式：創建新對象
    setUser({ ...user, name: 'Alice', age: 25 });

    // ✅ 或使用 Immer（React 沒有內建，需要安裝）
    // import { produce } from 'immer';
    // setUser(produce(draft => {
    //   draft.name = 'Alice';
    //   draft.age = 25;
    // }));
  };

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={increment}>+1</button>
    </div>
  );
}
```

**關鍵差異**:
| 特性 | Vue Composition API | React Hooks |
|------|---------------------|-------------|
| **基本狀態** | `ref(0)` | `useState(0)` |
| **對象狀態** | `reactive({})` | `useState({})` |
| **訪問值** | `count.value` | `count` |
| **更新值** | `count.value++` | `setCount(count + 1)` |
| **不可變** | 否（直接修改） | 是（必須創建新對象） |

---

### 1.2 useEffect vs Vue watch/onMounted

**Vue Composition API**:
```vue
<script setup>
import { ref, watch, onMounted } from 'vue';

const count = ref(0);

// 組件掛載時執行
onMounted(() => {
  console.log('Component mounted');
});

// 監聽 count 變化
watch(count, (newVal, oldVal) => {
  console.log(`Count changed from ${oldVal} to ${newVal}`);
});
</script>
```

**React Hooks**:
```typescript
import { useState, useEffect } from 'react';

export default function Counter() {
  const [count, setCount] = useState(0);

  // 組件掛載時執行（onMounted）
  useEffect(() => {
    console.log('Component mounted');
  }, []); // 空依賴項 = 只執行一次

  // 監聽 count 變化（watch）
  useEffect(() => {
    console.log(`Count changed to ${count}`);
  }, [count]); // 依賴項 = count

  // 組件卸載時執行（onUnmounted）
  useEffect(() => {
    return () => {
      console.log('Component unmounted');
    };
  }, []);

  return <div>Count: {count}</div>;
}
```

**關鍵差異**:
| 特性 | Vue | React |
|------|-----|-------|
| **掛載** | `onMounted(() => {})` | `useEffect(() => {}, [])` |
| **卸載** | `onUnmounted(() => {})` | `useEffect(() => () => {}, [])` |
| **監聽** | `watch(value, callback)` | `useEffect(() => {}, [value])` |
| **清理函數** | `watch` 返回 stop | `useEffect` 返回 cleanup |

---

### 1.3 useRef vs Vue ref（DOM 引用）

**Vue Composition API**:
```vue
<script setup>
import { ref } from 'vue';

const inputRef = ref<HTMLInputElement | null>(null);

const focusInput = () => {
  inputRef.value?.focus();
};
</script>

<template>
  <input ref="inputRef" />
  <button @click="focusInput">Focus</button>
</template>
```

**React Hooks**:
```typescript
import { useRef } from 'react';

export default function Form() {
  const inputRef = useRef<HTMLInputElement>(null);

  const focusInput = () => {
    inputRef.current?.focus();
  };

  return (
    <div>
      <input ref={inputRef} />
      <button onClick={focusInput}>Focus</button>
    </div>
  );
}
```

**關鍵差異**:
| 特性 | Vue | React |
|------|-----|-------|
| **定義** | `ref<Element \| null>(null)` | `useRef<Element>(null)` |
| **訪問** | `ref.value` | `ref.current` |
| **模板綁定** | `ref="refName"` | `ref={refObj}` |

---

## 第二部分：性能優化 Hooks（30分鐘）

### 2.1 useMemo vs Vue computed

**Vue Composition API**:
```vue
<script setup>
import { ref, computed } from 'vue';

const items = ref([1, 2, 3, 4, 5]);

const total = computed(() => {
  console.log('Computing total...');
  return items.value.reduce((sum, item) => sum + item, 0);
});
</script>
```

**React Hooks**:
```typescript
import { useState, useMemo } from 'react';

export default function List() {
  const [items, setItems] = useState([1, 2, 3, 4, 5]);

  const total = useMemo(() => {
    console.log('Computing total...');
    return items.reduce((sum, item) => sum + item, 0);
  }, [items]); // 依賴項：items

  return <div>Total: {total}</div>;
}
```

**何時使用 useMemo**:
- ✅ 昂貴的計算（循環、過濾、排序大數組）
- ✅ 避免子組件重渲染（傳遞對象/數組 props）
- ❌ 簡單計算（加減乘除、字符串拼接）

---

### 2.2 useCallback vs Vue 手動優化

**Vue Composition API**:
```vue
<script setup>
import { ref } from 'vue';

const count = ref(0);

const handleClick = () => {
  count.value++;
};
</script>

<template>
  <ChildComponent @click="handleClick" />
</template>
```

**React Hooks**:
```typescript
import { useState, useCallback } from 'react';

export default function Parent() {
  const [count, setCount] = useState(0);

  // ❌ 每次渲染都創建新函數（子組件重渲染）
  const handleClick = () => {
    setCount(count + 1);
  };

  // ✅ 使用 useCallback 緩存函數
  const handleClickOptimized = useCallback(() => {
    setCount((prev) => prev + 1);
  }, []); // 空依賴項 = 函數永不變

  return <ChildComponent onClick={handleClickOptimized} />;
}
```

**何時使用 useCallback**:
- ✅ 傳遞給子組件的回調函數
- ✅ 作為 useEffect 依賴項的函數
- ❌ 組件內部的事件處理函數（不傳遞給子組件）

---

### 2.3 React.memo vs Vue 無需優化

**Vue**:
```vue
<!-- Vue 默認優化（組件級緩存） -->
<template>
  <ChildComponent :data="data" />
</template>
```

**React**:
```typescript
// 子組件（未優化）
function ChildComponent({ data }: { data: string }) {
  console.log('Child rendered');
  return <div>{data}</div>;
}

// 子組件（使用 React.memo 優化）
const ChildComponentOptimized = React.memo(function ChildComponent({ data }) {
  console.log('Child rendered');
  return <div>{data}</div>;
});

// 父組件
export default function Parent() {
  const [count, setCount] = useState(0);
  const data = 'Hello';

  return (
    <div>
      <button onClick={() => setCount(count + 1)}>Count: {count}</button>
      <ChildComponentOptimized data={data} />
    </div>
  );
}
```

---

## 第三部分：自定義 Hooks（40分鐘）

### 3.1 useTable Hook（SmartAdmin CRUD 模式）

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

  // 查詢列表
  const query = useCallback(
    async (params = {}) => {
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
    },
    [queryApi, pagination]
  );

  // 刪除單條
  const deleteRow = useCallback(
    async (id: number) => {
      if (!deleteApi) return;

      try {
        await deleteApi(id);
        message.success('刪除成功');
        query(); // 重新查詢
      } catch (error) {
        message.error('刪除失敗');
      }
    },
    [deleteApi, query]
  );

  // 批量刪除
  const batchDelete = useCallback(
    async (ids: number[]) => {
      if (!deleteApi) return;

      await Promise.all(ids.map((id) => deleteApi(id)));
      message.success(`成功刪除${ids.length}條記錄`);
      query();
    },
    [deleteApi, query]
  );

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

**使用示例**:
```typescript
// views/goods/GoodsList.tsx
import { useTable } from '@/hooks/useTable';
import { goodsApi } from '@/api/business/goods-api';

export default function GoodsList() {
  const {
    dataSource,
    loading,
    total,
    pagination,
    query,
    deleteRow,
    batchDelete,
  } = useTable({
    queryApi: goodsApi.queryPage,
    deleteApi: goodsApi.delete,
  });

  useEffect(() => {
    query(); // 初始化查詢
  }, [query]);

  return (
    <Table
      dataSource={dataSource}
      loading={loading}
      pagination={{ total, ...pagination }}
      columns={columns}
    />
  );
}
```

---

### 3.2 useModal Hook

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
    isEdit: !!editData, // 是否編輯模式
    open,
    close,
  };
}
```

**使用示例**:
```typescript
// views/goods/GoodsList.tsx
import { useModal } from '@/hooks/useModal';
import GoodsFormModal from './GoodsFormModal';

export default function GoodsList() {
  const modal = useModal<GoodsItem>();

  return (
    <div>
      <button onClick={() => modal.open()}>新增</button>
      <button onClick={() => modal.open(selectedRow)}>編輯</button>

      <GoodsFormModal
        visible={modal.visible}
        editData={modal.editData}
        isEdit={modal.isEdit}
        onClose={modal.close}
      />
    </div>
  );
}
```

---

### 3.3 usePrivilege Hook（SmartAdmin 權限控制）

```typescript
// hooks/usePrivilege.ts
import { useAppSelector } from '@/store/hooks';

export function usePrivilege(permission: string): boolean {
  const administratorFlag = useAppSelector((state) => state.user.administratorFlag);
  const pointsList = useAppSelector((state) => state.user.pointsList);

  if (administratorFlag) return true;

  return pointsList.some((point) => point.webPerms === permission);
}

// 多權限檢查（任一滿足）
export function usePrivilegeAny(permissions: string[]): boolean {
  const administratorFlag = useAppSelector((state) => state.user.administratorFlag);
  const pointsList = useAppSelector((state) => state.user.pointsList);

  if (administratorFlag) return true;

  return permissions.some((perm) =>
    pointsList.some((point) => point.webPerms === perm)
  );
}

// 多權限檢查（全部滿足）
export function usePrivilegeAll(permissions: string[]): boolean {
  const administratorFlag = useAppSelector((state) => state.user.administratorFlag);
  const pointsList = useAppSelector((state) => state.user.pointsList);

  if (administratorFlag) return true;

  return permissions.every((perm) =>
    pointsList.some((point) => point.webPerms === perm)
  );
}
```

**使用示例**:
```typescript
// views/goods/GoodsList.tsx
import { usePrivilege } from '@/hooks/usePrivilege';

export default function GoodsList() {
  const canAdd = usePrivilege('goods:add');
  const canEdit = usePrivilege('goods:edit');
  const canDelete = usePrivilege('goods:delete');

  return (
    <div>
      {canAdd && <button>新增</button>}
      {canEdit && <button>編輯</button>}
      {canDelete && <button>刪除</button>}
    </div>
  );
}
```

---

## 第四部分：常見陷阱與解決方案（30分鐘）

### 4.1 依賴項遺漏

**❌ 錯誤示例**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    console.log(count); // 使用了 count
  }, []); // ⚠️ 缺少依賴項 count

  return <div>{count}</div>;
}
```

**✅ 正確示例**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    console.log(count);
  }, [count]); // ✅ 添加依賴項

  return <div>{count}</div>;
}
```

**工具檢查**:
- 使用 `eslint-plugin-react-hooks` 自動檢查

---

### 4.2 過度使用 useEffect

**❌ 錯誤示例**:
```typescript
function Component() {
  const [count, setCount] = useState(0);
  const [doubledCount, setDoubledCount] = useState(0);

  // ❌ 不必要的 useEffect
  useEffect(() => {
    setDoubledCount(count * 2);
  }, [count]);

  return <div>{doubledCount}</div>;
}
```

**✅ 正確示例**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  // ✅ 直接計算（或使用 useMemo）
  const doubledCount = count * 2;

  return <div>{doubledCount}</div>;
}
```

---

### 4.3 useCallback 過度優化

**❌ 過度優化**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  // ❌ 不必要的 useCallback（函數未傳遞給子組件）
  const handleClick = useCallback(() => {
    setCount(count + 1);
  }, [count]);

  return <button onClick={handleClick}>Click</button>;
}
```

**✅ 適度優化**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  // ✅ 組件內部事件處理，不需要 useCallback
  const handleClick = () => {
    setCount(count + 1);
  };

  return <button onClick={handleClick}>Click</button>;
}
```

---

### 4.4 閉包陷阱

**❌ 錯誤示例**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      // ⚠️ 閉包捕獲初始值 count = 0
      setCount(count + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, []); // 空依賴項

  return <div>{count}</div>; // count 永遠是 1
}
```

**✅ 正確示例**:
```typescript
function Component() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      // ✅ 使用函數式更新
      setCount((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  return <div>{count}</div>; // count 正常遞增
}
```

---

## 📚 參考資料

- [React Hooks 官方文檔](https://react.dev/reference/react)
- [React Hooks FAQ](https://react.dev/learn/you-might-not-need-an-effect)
- [Redux Toolkit Workshop](./redux-toolkit-workshop.md)
- [Pinia to RTK Mapping](./pinia-to-rtk-mapping.md)

---

## ✅ 課後檢查清單

完成 Workshop 後，確認你能：
- [ ] 正確使用 useState、useEffect、useRef
- [ ] 理解 useEffect 依賴項管理
- [ ] 使用 useMemo、useCallback 優化性能
- [ ] 編寫自定義 Hooks（useTable、useModal、usePrivilege）
- [ ] 避免常見 Hooks 陷阱（依賴項遺漏、閉包陷阱）
- [ ] 在 SmartAdmin 項目中應用這些模式

**下一步**：開始 Phase 1 實戰遷移！
