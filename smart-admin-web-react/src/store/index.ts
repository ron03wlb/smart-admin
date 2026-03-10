/**
 * Redux Store 配置
 * 使用 Redux Toolkit + Redux Persist
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
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
import storage from 'redux-persist/lib/storage'; // localStorage

import userReducer from './slices/userSlice';
import dictReducer from './slices/dictSlice';
import spinReducer from './slices/spinSlice';
import appConfigReducer from './slices/appConfigSlice';
import roleReducer from './slices/roleSlice';
import tenantReducer from './slices/tenantSlice';

// ==================== Root Reducer ====================

const rootReducer = combineReducers({
  user: userReducer,
  dict: dictReducer,
  spin: spinReducer,
  appConfig: appConfigReducer,
  role: roleReducer,
  tenant: tenantReducer,
  // 未來可添加更多 slices: tagNav, menu, etc.
});

// ==================== Redux Persist 配置 ====================

const persistConfig = {
  key: 'root',
  version: 1,
  storage,
  whitelist: ['user', 'dict', 'appConfig', 'tenant'], // 持久化 user、dict、appConfig 和 tenant slices
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

// ==================== Store 配置 ====================

export const store = configureStore({
  reducer: persistedReducer,
  middleware: getDefaultMiddleware =>
    getDefaultMiddleware({
      serializableCheck: {
        // 忽略 Redux Persist 的 actions
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
  devTools: process.env.NODE_ENV !== 'production', // 開發環境啟用 Redux DevTools
});

// ==================== Persistor ====================

export const persistor = persistStore(store);

// ==================== TypeScript 類型定義 ====================

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
