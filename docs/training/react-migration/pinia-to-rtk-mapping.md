# Pinia to Redux Toolkit 對照表

**版本**: 1.0.0
**更新日期**: 2026-03-09

本文檔提供 SmartAdmin 從 Pinia（Vue）遷移至 Redux Toolkit（React）的完整對照表。

---

## 📋 快速對照表

| 特性 | Pinia（Vue） | Redux Toolkit（React） | 說明 |
|------|-------------|------------------------|------|
| **Store 定義** | `defineStore()` | `createSlice()` | Slice = Reducer + Actions |
| **Store ID** | `id: 'userStore'` | `name: 'user'` | 用於 Redux DevTools 顯示 |
| **初始狀態** | `state: () => ({})` | `initialState: {}` | 必須是對象，不是函數 |
| **計算屬性** | `getters: {}` | `createSelector()` | Selector 函數 |
| **同步操作** | `actions: {}` | `reducers: {}` | Action Creator 自動生成 |
| **異步操作** | `async actions` | `createAsyncThunk()` | 處理 pending/fulfilled/rejected |
| **組件訪問** | `const store = useXxxStore()` | `useSelector() + useDispatch()` | Hook 訪問 |
| **狀態更新** | `store.count++` | `dispatch(increment())` | 必須 dispatch Action |
| **持久化** | pinia-plugin-persistedstate | redux-persist | 配置方式不同 |

---

## 1. Store 定義

### Pinia（Vue）

```typescript
// store/modules/system/user.ts
export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    token: '',
    employeeId: '',
  }),
  getters: {
    getToken(state) {
      return state.token;
    },
  },
  actions: {
    setToken(token: string) {
      this.token = token;
    },
  },
});
```

### Redux Toolkit（React）

```typescript
// store/slices/userSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserState {
  token: string;
  employeeId: string;
}

const initialState: UserState = {
  token: '',
  employeeId: '',
};

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload;
    },
  },
});

// Selector
export const selectToken = (state: RootState) => state.user.token;

export const { setToken } = userSlice.actions;
export default userSlice.reducer;
```

---

## 2. Getters vs Selectors

### Pinia Getters

```typescript
// store/modules/system/user.ts
export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    token: '',
    administratorFlag: false,
    pointsList: [],
  }),
  getters: {
    // 簡單 Getter
    getToken(state) {
      return state.token || localRead(localKey.USER_TOKEN);
    },

    // 依賴其他 Getter
    isAdmin(state) {
      return state.administratorFlag;
    },

    // 帶參數的 Getter
    hasPrivilege() {
      return (permission: string) => {
        if (this.administratorFlag) return true;
        return this.pointsList.some((p) => p.webPerms === permission);
      };
    },
  },
});
```

### Redux Toolkit Selectors

```typescript
// store/slices/userSlice.ts
import { createSelector } from '@reduxjs/toolkit';
import type { RootState } from '../index';

// 簡單 Selector
export const selectToken = (state: RootState) =>
  state.user.token || localStorage.getItem('USER_TOKEN') || '';

export const selectIsAdmin = (state: RootState) => state.user.administratorFlag;

export const selectPointsList = (state: RootState) => state.user.pointsList;

// 依賴其他 Selector（使用 createSelector 優化）
export const selectUserPermissions = createSelector(
  [selectPointsList],
  (pointsList) => pointsList.map((p) => p.webPerms)
);

// 帶參數的 Selector（工廠函數模式）
export const makeSelectHasPrivilege = () =>
  createSelector(
    [selectIsAdmin, selectPointsList, (_, permission: string) => permission],
    (isAdmin, pointsList, permission) => {
      if (isAdmin) return true;
      return pointsList.some((p) => p.webPerms === permission);
    }
  );
```

---

## 3. Actions vs Reducers

### Pinia Actions（同步）

```typescript
// store/modules/system/user.ts
export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    token: '',
    menuTree: [],
  }),
  actions: {
    setToken(token: string) {
      this.token = token;
      localSave(localKey.USER_TOKEN, token);
    },
    setMenuTree(menuTree: MenuItem[]) {
      this.menuTree = menuTree;
    },
    logout() {
      this.token = '';
      this.menuTree = [];
      localRemove(localKey.USER_TOKEN);
    },
  },
});
```

### Redux Toolkit Reducers（同步）

```typescript
// store/slices/userSlice.ts
export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setToken: (state, action: PayloadAction<string>) => {
      state.token = action.payload;
      localStorage.setItem('USER_TOKEN', action.payload);
    },
    setMenuTree: (state, action: PayloadAction<MenuItem[]>) => {
      state.menuTree = action.payload;
    },
    logout: (state) => {
      state.token = '';
      state.menuTree = [];
      localStorage.removeItem('USER_TOKEN');
    },
  },
});
```

---

## 4. 異步操作

### Pinia 異步 Actions

```typescript
// store/modules/system/user.ts
export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    token: '',
    loading: false,
    error: null,
  }),
  actions: {
    async login(username: string, password: string) {
      this.loading = true;
      this.error = null;

      try {
        const res = await loginApi.login({ username, password });
        if (res.data) {
          this.token = res.data.token;
          localSave(localKey.USER_TOKEN, res.data.token);
        }
      } catch (error) {
        this.error = error.message;
      } finally {
        this.loading = false;
      }
    },
  },
});
```

### Redux Toolkit createAsyncThunk

```typescript
// store/slices/userSlice.ts
export const login = createAsyncThunk(
  'user/login',
  async ({ username, password }: { username: string; password: string }) => {
    const res = await loginApi.login({ username, password });
    return res.data;
  }
);

export const userSlice = createSlice({
  name: 'user',
  initialState: {
    token: '',
    loading: false,
    error: null,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.token = action.payload.token;
        localStorage.setItem('USER_TOKEN', action.payload.token);
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || '登錄失敗';
      });
  },
});
```

---

## 5. 組件使用

### Pinia 組件使用（Vue）

```vue
<script setup>
import { useUserStore } from '@/store/modules/system/user';
import { computed } from 'vue';

const userStore = useUserStore();

// 讀取狀態
const token = computed(() => userStore.getToken);
const isAdmin = computed(() => userStore.isAdmin);

// 調用同步 Action
const handleSetToken = (newToken: string) => {
  userStore.setToken(newToken);
};

// 調用異步 Action
const handleLogin = async () => {
  await userStore.login('admin', 'admin123');
};

// 直接修改狀態（Pinia 允許）
const directUpdate = () => {
  userStore.token = 'new-token';
};
</script>
```

### Redux Toolkit 組件使用（React）

```typescript
// Header.tsx
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectToken, selectIsAdmin, setToken, login } from '@/store/slices/userSlice';

export default function Header() {
  const dispatch = useAppDispatch();

  // 讀取狀態
  const token = useAppSelector(selectToken);
  const isAdmin = useAppSelector(selectIsAdmin);

  // 調用同步 Reducer
  const handleSetToken = (newToken: string) => {
    dispatch(setToken(newToken));
  };

  // 調用異步 Thunk
  const handleLogin = async () => {
    const result = await dispatch(login({ username: 'admin', password: 'admin123' }));

    if (login.fulfilled.match(result)) {
      console.log('登錄成功', result.payload);
    }
  };

  // ❌ 不能直接修改狀態（Redux 禁止）
  // token = 'new-token';  // TypeError: Cannot assign to read only property

  return (
    <div>
      <span>Token: {token}</span>
      <span>Admin: {isAdmin ? '是' : '否'}</span>
      <button onClick={handleLogin}>登錄</button>
    </div>
  );
}
```

---

## 6. Store 配置

### Pinia 配置（Vue）

```typescript
// main.ts
import { createPinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

const app = createApp(App);
app.use(pinia);
```

### Redux Toolkit 配置（React）

```typescript
// store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import {
  persistStore,
  persistReducer,
  FLUSH,
  REHYDRATE,
  PAUSE,
  PERSIST,
  PURGE,
  REGISTER,
} from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import userReducer from './slices/userSlice';
import dictReducer from './slices/dictSlice';

// redux-persist 配置
const persistConfig = {
  key: 'root',
  version: 1,
  storage,
  whitelist: ['user', 'dict'], // 持久化的 Slice
};

const persistedReducer = persistReducer(persistConfig, combineReducers({
  user: userReducer,
  dict: dictReducer,
}));

export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
});

export const persistor = persistStore(store);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

```typescript
// App.tsx
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';
import { store, persistor } from './store';

function App() {
  return (
    <Provider store={store}>
      <PersistGate loading={null} persistor={persistor}>
        {/* 應用內容 */}
      </PersistGate>
    </Provider>
  );
}
```

---

## 7. 完整遷移對照：SmartAdmin Stores

### 7.1 userStore → userSlice

| Pinia | Redux Toolkit | 說明 |
|-------|---------------|------|
| `useUserStore()` | `useAppSelector(selectUser)` | 訪問方式 |
| `userStore.getToken` | `useAppSelector(selectToken)` | 獲取狀態 |
| `userStore.setToken(token)` | `dispatch(setToken(token))` | 更新狀態 |
| `await userStore.login()` | `await dispatch(login())` | 異步操作 |
| `userStore.$reset()` | `dispatch(resetUser())` | 重置狀態 |

### 7.2 dictStore → dictSlice

| Pinia | Redux Toolkit | 說明 |
|-------|---------------|------|
| `useDictStore()` | `useAppSelector(selectDict)` | 訪問方式 |
| `dictStore.dictList` | `useAppSelector(selectDictList)` | 獲取狀態 |
| `dictStore.setDictList(list)` | `dispatch(setDictList(list))` | 更新狀態 |
| `await dictStore.fetchDictList()` | `await dispatch(fetchDictList())` | 異步操作 |

### 7.3 appConfigStore → appConfigSlice

| Pinia | Redux Toolkit | 說明 |
|-------|---------------|------|
| `useAppConfigStore()` | `useAppSelector(selectAppConfig)` | 訪問方式 |
| `appConfigStore.locale` | `useAppSelector(selectLocale)` | 獲取語言 |
| `appConfigStore.setLocale('en')` | `dispatch(setLocale('en'))` | 切換語言 |
| `appConfigStore.sidebarCollapse` | `useAppSelector(selectSidebarCollapse)` | 側邊欄狀態 |
| `appConfigStore.toggleSidebar()` | `dispatch(toggleSidebar())` | 切換側邊欄 |

---

## 8. 注意事項

### ❌ Pinia 允許但 Redux 禁止的操作

```typescript
// ❌ 直接修改狀態（Redux 禁止）
const userStore = useUserStore();
userStore.token = 'new-token';  // Pinia OK, Redux NO

// ✅ Redux 正確方式
dispatch(setToken('new-token'));
```

```typescript
// ❌ 直接解構狀態（會失去響應性）
const { token, employeeId } = useUserStore();  // Pinia OK（需要 storeToRefs）

// ✅ Redux 正確方式
const token = useAppSelector(selectToken);
const employeeId = useAppSelector(selectEmployeeId);
```

### ⚠️ 性能優化差異

**Pinia**：
- Getter 自動緩存
- 組件解構需要 `storeToRefs`

**Redux Toolkit**:
- 使用 `createSelector` 緩存 Selector
- `useSelector` 默認淺比較（引用相等）
- 復雜對象需要自定義比較函數

```typescript
// 復雜對象 Selector 優化
const menuTree = useAppSelector(selectMenuTree, shallowEqual);
```

---

## 9. 遷移檢查清單

### 對於每個 Pinia Store，確認：

- [ ] `defineStore` → `createSlice`
- [ ] `state: () => ({})` → `initialState: {}`
- [ ] `getters: {}` → `createSelector()` 或簡單函數
- [ ] `actions: {}` → `reducers: {}`（同步）或 `createAsyncThunk`（異步）
- [ ] 組件使用 `useXxxStore()` → `useSelector() + useDispatch()`
- [ ] 直接修改狀態 → `dispatch(action())`
- [ ] 狀態持久化配置（pinia-plugin-persistedstate → redux-persist）
- [ ] Redux DevTools 可正常使用

---

## 📚 相關文檔

- [Redux Toolkit Workshop](./redux-toolkit-workshop.md)
- [React Hooks Patterns](./react-hooks-patterns.md)
- [Vue to React 遷移計劃](../../migration/vue-to-react-migration-plan.md)
