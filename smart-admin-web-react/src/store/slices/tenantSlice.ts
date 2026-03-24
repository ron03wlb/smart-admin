/**
 * Tenant Slice
 * 租戶上下文狀態管理
 *
 * 多租戶系統的租戶信息管理，包括：
 * - 租戶 ID
 * - 時區配置
 * - 租戶代碼
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { TenantState, TenantInfo } from '@/types/tenant';
import { STORAGE_KEYS } from '@/constants/storageKeys';

/**
 * 從 localStorage 讀取租戶信息（fallback）
 */
function loadTenantFromLocalStorage(): TenantState {
  try {
    const tenantIdStr = localStorage.getItem(STORAGE_KEYS.TENANT_ID);
    const timezone = localStorage.getItem(STORAGE_KEYS.TENANT_TIMEZONE) || 'Asia/Taipei';
    const tenantCode = localStorage.getItem(STORAGE_KEYS.TENANT_CODE) || 'default';

    return {
      tenantId: tenantIdStr ? Number(tenantIdStr) : null,
      timezone,
      tenantCode,
    };
  } catch (error) {
    console.error('Failed to load tenant info from localStorage:', error);
    return {
      tenantId: null,
      timezone: 'Asia/Taipei',
      tenantCode: 'default',
    };
  }
}

const initialState: TenantState = loadTenantFromLocalStorage();

const tenantSlice = createSlice({
  name: 'tenant',
  initialState,
  reducers: {
    /**
     * 設置租戶信息
     * @param info - 租戶信息（tenantId, timezone, tenantCode）
     */
    setTenantInfo: (state, action: PayloadAction<TenantInfo>) => {
      state.tenantId = action.payload.tenantId;
      state.timezone = action.payload.timezone;
      state.tenantCode = action.payload.tenantCode;

      // 同步到 localStorage（向後兼容）
      try {
        if (action.payload.tenantId !== null) {
          localStorage.setItem(STORAGE_KEYS.TENANT_ID, String(action.payload.tenantId));
        } else {
          localStorage.removeItem(STORAGE_KEYS.TENANT_ID);
        }
        localStorage.setItem(STORAGE_KEYS.TENANT_TIMEZONE, action.payload.timezone);
        localStorage.setItem(STORAGE_KEYS.TENANT_CODE, action.payload.tenantCode);
      } catch (error) {
        console.error('Failed to save tenant info to localStorage:', error);
      }
    },

    /**
     * 設置租戶 ID
     */
    setTenantId: (state, action: PayloadAction<number | null>) => {
      state.tenantId = action.payload;
      try {
        if (action.payload !== null) {
          localStorage.setItem(STORAGE_KEYS.TENANT_ID, String(action.payload));
        } else {
          localStorage.removeItem(STORAGE_KEYS.TENANT_ID);
        }
      } catch (error) {
        console.error('Failed to save tenant ID to localStorage:', error);
      }
    },

    /**
     * 設置時區
     */
    setTimezone: (state, action: PayloadAction<string>) => {
      state.timezone = action.payload;
      try {
        localStorage.setItem(STORAGE_KEYS.TENANT_TIMEZONE, action.payload);
      } catch (error) {
        console.error('Failed to save timezone to localStorage:', error);
      }
    },

    /**
     * 設置租戶代碼
     */
    setTenantCode: (state, action: PayloadAction<string>) => {
      state.tenantCode = action.payload;
      try {
        localStorage.setItem(STORAGE_KEYS.TENANT_CODE, action.payload);
      } catch (error) {
        console.error('Failed to save tenant code to localStorage:', error);
      }
    },

    /**
     * 清除租戶信息
     */
    clearTenantInfo: state => {
      state.tenantId = null;
      state.timezone = 'Asia/Taipei';
      state.tenantCode = 'default';

      // 同步到 localStorage
      try {
        localStorage.removeItem(STORAGE_KEYS.TENANT_ID);
        localStorage.removeItem(STORAGE_KEYS.TENANT_TIMEZONE);
        localStorage.removeItem(STORAGE_KEYS.TENANT_CODE);
      } catch (error) {
        console.error('Failed to clear tenant info from localStorage:', error);
      }
    },

    /**
     * 重置為默認狀態
     */
    reset: () => ({
      tenantId: null,
      timezone: 'Asia/Taipei',
      tenantCode: 'default',
    }),
  },
});

export const { setTenantInfo, setTenantId, setTimezone, setTenantCode, clearTenantInfo, reset } =
  tenantSlice.actions;

export default tenantSlice.reducer;
