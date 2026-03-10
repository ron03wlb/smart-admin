# Redux Toolkit Workshop

**培訓時間**: 2 小時
**培訓對象**: 參與 React 遷移的所有前端工程師
**先修知識**: 基礎 React、TypeScript

---

## 📋 培訓目標

本次 Workshop 完成後，你將能夠：
- ✅ 理解 Redux Toolkit 的核心概念（Store、Slice、Reducer、Action）
- ✅ 使用 `createSlice` 創建 Redux Slice
- ✅ 使用 `createAsyncThunk` 處理異步邏輯
- ✅ 在 React 組件中使用 Redux（useSelector、useDispatch）
- ✅ 將 Pinia Store 遷移為 Redux Toolkit Slice

---

## 第一部分：Redux Toolkit 基礎（30分鐘）

### 1.1 為什麼選擇 Redux Toolkit？

**傳統 Redux 的問題**:
- ❌ 樣板代碼過多（Action Types、Action Creators、Reducer）
- ❌ 配置複雜（Middleware、DevTools）
- ❌ 不可變更新繁瑣（需要手動處理）

**Redux Toolkit 的優勢**:
- ✅ 自動生成 Action Creator
- ✅ 內建 Immer（可變語法寫不可變更新）
- ✅ 集成 Redux DevTools
- ✅ 簡化 Store 配置

### 1.2 核心概念

```
Store（全局狀態樹）
  ├── userSlice（用戶狀態）
  ├── appConfigSlice（應用配置）
  └── dictSlice（數據字典）

Slice = Reducer + Actions
  ├── initialState（初始狀態）
  ├── reducers（同步操作）
  └── extraReducers（異步操作）
```

### 1.3 創建第一個 Slice

```typescript
// store/slices/counterSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface CounterState {
  value: number;
}

const initialState: CounterState = {
  value: 0,
};

export const counterSlice = createSlice({
  name: 'counter',
  initialState,
  reducers: {
    increment: (state) => {
      // ✅ Redux Toolkit 允許「可變」語法（內部使用 Immer）
      state.value += 1;
    },
    decrement: (state) => {
      state.value -= 1;
    },
    incrementByAmount: (state, action: PayloadAction<number>) => {
      state.value += action.payload;
    },
  },
});

// 導出 Action Creators
export const { increment, decrement, incrementByAmount } = counterSlice.actions;

// 導出 Reducer
export default counterSlice.reducer;
```

### 1.4 配置 Store

```typescript
// store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import counterReducer from './slices/counterSlice';

export const store = configureStore({
  reducer: {
    counter: counterReducer,
  },
});

// TypeScript 類型
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

### 1.5 在 React 組件中使用

```typescript
// App.tsx
import { Provider } from 'react-redux';
import { store } from './store';
import Counter from './Counter';

function App() {
  return (
    <Provider store={store}>
      <Counter />
    </Provider>
  );
}
```

```typescript
// Counter.tsx
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from './store';
import { increment, decrement, incrementByAmount } from './store/slices/counterSlice';

export default function Counter() {
  const count = useSelector((state: RootState) => state.counter.value);
  const dispatch = useDispatch();

  return (
    <div>
      <h1>Count: {count}</h1>
      <button onClick={() => dispatch(increment())}>+1</button>
      <button onClick={() => dispatch(decrement())}>-1</button>
      <button onClick={() => dispatch(incrementByAmount(5))}>+5</button>
    </div>
  );
}
```

---

## 第二部分：異步操作（40分鐘）

### 2.1 createAsyncThunk 基礎

```typescript
// store/slices/userSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { loginApi } from '@/api/system/login-api';

interface UserState {
  token: string;
  employeeId: string;
  loading: boolean;
  error: string | null;
}

const initialState: UserState = {
  token: '',
  employeeId: '',
  loading: false,
  error: null,
};

// 創建異步 Thunk
export const login = createAsyncThunk(
  'user/login',
  async (credentials: { username: string; password: string }) => {
    const response = await loginApi.login(credentials);
    return response.data; // 返回 payload
  }
);

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    logout: (state) => {
      state.token = '';
      state.employeeId = '';
      localStorage.removeItem('USER_TOKEN');
    },
  },
  extraReducers: (builder) => {
    builder
      // pending 狀態
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      // fulfilled 狀態
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.token = action.payload.token;
        state.employeeId = action.payload.employeeId;
        localStorage.setItem('USER_TOKEN', action.payload.token);
      })
      // rejected 狀態
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || '登錄失敗';
      });
  },
});

export const { logout } = userSlice.actions;
export default userSlice.reducer;
```

### 2.2 在組件中使用異步 Thunk

```typescript
// views/login/Login.tsx
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { login } from '@/store/slices/userSlice';

export default function Login() {
  const dispatch = useAppDispatch();
  const { loading, error } = useAppSelector((state) => state.user);

  const handleLogin = async () => {
    const result = await dispatch(
      login({ username: 'admin', password: 'admin123' })
    );

    if (login.fulfilled.match(result)) {
      // 登錄成功
      console.log('登錄成功', result.payload);
    } else {
      // 登錄失敗
      console.error('登錄失敗', result.error);
    }
  };

  return (
    <div>
      <button onClick={handleLogin} disabled={loading}>
        {loading ? '登錄中...' : '登錄'}
      </button>
      {error && <div style={{ color: 'red' }}>{error}</div>}
    </div>
  );
}
```

### 2.3 自定義 Hooks（推薦）

```typescript
// store/hooks.ts
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from './index';

// 類型化的 Hooks
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
```

---

## 第三部分：Pinia → Redux Toolkit 遷移（40分鐘）

### 3.1 對比：Pinia vs Redux Toolkit

| 特性 | Pinia（Vue） | Redux Toolkit（React） |
|------|-------------|------------------------|
| **Store 定義** | `defineStore()` | `createSlice()` |
| **狀態** | `state: () => ({})` | `initialState: {}` |
| **計算屬性** | `getters: {}` | `createSelector()` |
| **同步操作** | `actions: {}` | `reducers: {}` |
| **異步操作** | `async actions` | `createAsyncThunk()` |
| **組件使用** | `const store = useXxxStore()` | `useSelector() + useDispatch()` |
| **狀態更新** | 直接賦值（`store.count++`） | Dispatch Action（`dispatch(increment())`） |

### 3.2 實戰遷移：userStore → userSlice

**Pinia 版本（Vue）**:
```typescript
// store/modules/system/user.ts (Pinia)
export const useUserStore = defineStore({
  id: 'userStore',
  state: () => ({
    token: '',
    employeeId: '',
    menuTree: [],
    administratorFlag: false,
  }),
  getters: {
    getToken(state) {
      return state.token || localRead(localKey.USER_TOKEN);
    },
  },
  actions: {
    async login(username: string, password: string) {
      const res = await loginApi.login({ username, password });
      if (res.data) {
        this.token = res.data.token;
        this.employeeId = res.data.employeeId;
        localSave(localKey.USER_TOKEN, res.data.token);
      }
    },
    logout() {
      this.token = '';
      this.employeeId = '';
      localRemove(localKey.USER_TOKEN);
    },
  },
});
```

**Redux Toolkit 版本（React）**:
```typescript
// store/slices/userSlice.ts (Redux Toolkit)
import { createSlice, createAsyncThunk, createSelector } from '@reduxjs/toolkit';
import { loginApi } from '@/api/system/login-api';
import type { RootState } from '../index';

interface UserState {
  token: string;
  employeeId: string;
  menuTree: MenuItem[];
  administratorFlag: boolean;
}

const initialState: UserState = {
  token: '',
  employeeId: '',
  menuTree: [],
  administratorFlag: false,
};

// 異步 Thunk（對應 Pinia actions）
export const login = createAsyncThunk(
  'user/login',
  async ({ username, password }: { username: string; password: string }) => {
    const res = await loginApi.login({ username, password });
    return res.data;
  }
);

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    logout: (state) => {
      state.token = '';
      state.employeeId = '';
      localStorage.removeItem('USER_TOKEN');
    },
    setMenuTree: (state, action) => {
      state.menuTree = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder.addCase(login.fulfilled, (state, action) => {
      state.token = action.payload.token;
      state.employeeId = action.payload.employeeId;
      state.administratorFlag = action.payload.administratorFlag;
      localStorage.setItem('USER_TOKEN', action.payload.token);
    });
  },
});

// Selector（對應 Pinia getters）
export const selectToken = (state: RootState) =>
  state.user.token || localStorage.getItem('USER_TOKEN') || '';

export const selectIsAdmin = (state: RootState) => state.user.administratorFlag;

// 複雜 Selector（使用 createSelector 優化）
export const selectMenuTree = createSelector(
  [(state: RootState) => state.user.menuTree],
  (menuTree) => menuTree.filter((menu) => !menu.hiddenFlag)
);

export const { logout, setMenuTree } = userSlice.actions;
export default userSlice.reducer;
```

**組件使用對比**:

```typescript
// Vue 組件（Pinia）
<script setup>
import { useUserStore } from '@/store/modules/system/user';

const userStore = useUserStore();
const token = computed(() => userStore.getToken);

const handleLogin = async () => {
  await userStore.login('admin', 'admin123');
};

const handleLogout = () => {
  userStore.logout();
};
</script>
```

```typescript
// React 組件（Redux Toolkit）
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectToken, login, logout } from '@/store/slices/userSlice';

export default function Header() {
  const token = useAppSelector(selectToken);
  const dispatch = useAppDispatch();

  const handleLogin = async () => {
    await dispatch(login({ username: 'admin', password: 'admin123' }));
  };

  const handleLogout = () => {
    dispatch(logout());
  };

  return (
    <div>
      <span>Token: {token}</span>
      <button onClick={handleLogin}>登錄</button>
      <button onClick={handleLogout}>登出</button>
    </div>
  );
}
```

---

## 第四部分：實戰練習（10分鐘）

### 練習 1：創建 dictSlice

**需求**：
- 狀態：`dictList: DictItem[]`
- 同步操作：`setDictList`、`clearDictList`
- 異步操作：`fetchDictList`（調用 `dictApi.queryAll()`）

**提示**：
```typescript
// 1. 定義 interface
interface DictState {
  dictList: DictItem[];
}

// 2. createAsyncThunk
export const fetchDictList = createAsyncThunk(
  'dict/fetchDictList',
  async () => {
    const res = await dictApi.queryAll();
    return res.data;
  }
);

// 3. createSlice
export const dictSlice = createSlice({
  // ...
});
```

### 練習 2：使用 dictSlice

**需求**：
- 在組件掛載時調用 `fetchDictList`
- 顯示加載狀態
- 渲染字典列表

---

## 📚 參考資料

- [Redux Toolkit 官方文檔](https://redux-toolkit.js.org/)
- [Redux DevTools Extension](https://github.com/reduxjs/redux-devtools)
- [Pinia to Redux Toolkit 對照表](./pinia-to-rtk-mapping.md)

---

## ✅ 課後檢查清單

完成 Workshop 後，確認你能：
- [ ] 使用 `createSlice` 創建 Slice
- [ ] 使用 `createAsyncThunk` 處理異步操作
- [ ] 在組件中使用 `useSelector` 和 `useDispatch`
- [ ] 使用 `createSelector` 優化 Selector
- [ ] 將 Pinia Store 遷移為 Redux Toolkit Slice
- [ ] 配置 Redux DevTools 調試狀態

**下一步**：開始 Phase 1 實戰遷移！
