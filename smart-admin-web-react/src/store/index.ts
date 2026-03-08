/**
 * Redux Store 配置（with Redux Persist）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { configureStore, combineReducers } from '@reduxjs/toolkit';
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
import storage from 'redux-persist/lib/storage'; // LocalStorage

import userReducer from './slices/userSlice';
import menuReducer from './slices/menuSlice';
import tagNavReducer from './slices/tagNavSlice';
import appConfigReducer from './slices/appConfigSlice';
import dictReducer from './slices/dictSlice';
import spinReducer from './slices/spinSlice';
import tenantReducer from './slices/tenantSlice';

// ================================= Redux Persist 配置 =================================

const persistConfig = {
  key: 'smart_admin_root', // LocalStorage 鍵名
  storage,
  whitelist: ['user', 'menu', 'tagNav', 'appConfig', 'tenant'], // 持久化
};

// ================================= Root Reducer =================================

const rootReducer = combineReducers({
  user: userReducer,
  menu: menuReducer,
  tagNav: tagNavReducer,
  appConfig: appConfigReducer,
  dict: dictReducer,
  spin: spinReducer,
  tenant: tenantReducer,
});

const persistedReducer = persistReducer(persistConfig, rootReducer);

// ================================= Store 配置 =================================

export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // 忽略 redux-persist 的 action 類型
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
  devTools: import.meta.env.DEV, // 開發環境啟用 Redux DevTools
});

export const persistor = persistStore(store);

// ================================= TypeScript 類型 =================================

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
