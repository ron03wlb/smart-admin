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

// ================================= Redux Persist 配置 =================================

const persistConfig = {
  key: 'smart_admin_root', // LocalStorage 鍵名
  storage,
  whitelist: ['user', 'menu', 'tagNav'], // 持久化 user, menu, tagNav
};

// ================================= Root Reducer =================================

const rootReducer = combineReducers({
  user: userReducer,
  menu: menuReducer,
  tagNav: tagNavReducer,
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
  devTools: process.env.NODE_ENV !== 'production', // 開發環境啟用 Redux DevTools
});

export const persistor = persistStore(store);

// ================================= TypeScript 類型 =================================

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
