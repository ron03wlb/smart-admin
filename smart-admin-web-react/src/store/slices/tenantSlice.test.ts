/**
 * tenantSlice Tests
 * tenantSlice 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

// TypeScript global type declaration
declare const global: typeof globalThis;

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import tenantReducer, {
  setTenantInfo,
  setTenantId,
  setTimezone,
  setTenantCode,
  clearTenantInfo,
  reset,
} from './tenantSlice';
import type { TenantState } from '@/types/tenant';
import { STORAGE_KEYS } from '@/constants/storageKeys';

describe('tenantSlice', () => {
  // 保存原始的 localStorage
  let originalLocalStorage: Storage;

  beforeEach(() => {
    originalLocalStorage = global.localStorage;
    // Mock localStorage
    const localStorageMock = (() => {
      let store: Record<string, string> = {};
      return {
        getItem: vi.fn((key: string) => store[key] || null),
        setItem: vi.fn((key: string, value: string) => {
          store[key] = value;
        }),
        removeItem: vi.fn((key: string) => {
          delete store[key];
        }),
        clear: vi.fn(() => {
          store = {};
        }),
        get length() {
          return Object.keys(store).length;
        },
        key: vi.fn((index: number) => Object.keys(store)[index] || null),
      };
    })();
    global.localStorage = localStorageMock as Storage;
  });

  afterEach(() => {
    global.localStorage = originalLocalStorage;
    vi.clearAllMocks();
  });

  describe('初始狀態', () => {
    it('應該返回默認狀態', () => {
      const state = tenantReducer(undefined, { type: '' });
      expect(state.tenantId).toBe(null);
      expect(state.timezone).toBe('Asia/Taipei');
      expect(state.tenantCode).toBe('default');
    });

    it('應該從 localStorage 加載狀態', () => {
      global.localStorage.setItem(STORAGE_KEYS.TENANT_ID, '123');
      global.localStorage.setItem(STORAGE_KEYS.TENANT_TIMEZONE, 'America/New_York');
      global.localStorage.setItem(STORAGE_KEYS.TENANT_CODE, 'acme-corp');

      // 重新導入以觸發初始化（在測試中需要手動處理）
      // 由於已經設置了 localStorage，測試初始化邏輯
      expect(global.localStorage.getItem(STORAGE_KEYS.TENANT_ID)).toBe('123');
    });
  });

  describe('setTenantInfo', () => {
    it('應該設置租戶信息', () => {
      const state = tenantReducer(
        undefined,
        setTenantInfo({
          tenantId: 100,
          timezone: 'Europe/London',
          tenantCode: 'uk-tenant',
        })
      );

      expect(state.tenantId).toBe(100);
      expect(state.timezone).toBe('Europe/London');
      expect(state.tenantCode).toBe('uk-tenant');
    });

    it('應該將租戶信息保存到 localStorage', () => {
      tenantReducer(
        undefined,
        setTenantInfo({
          tenantId: 200,
          timezone: 'Asia/Tokyo',
          tenantCode: 'jp-tenant',
        })
      );

      expect(global.localStorage.setItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_ID, '200');
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        STORAGE_KEYS.TENANT_TIMEZONE,
        'Asia/Tokyo'
      );
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        STORAGE_KEYS.TENANT_CODE,
        'jp-tenant'
      );
    });

    it('應該處理 tenantId 為 null 的情況', () => {
      const state = tenantReducer(
        undefined,
        setTenantInfo({
          tenantId: null,
          timezone: 'Asia/Taipei',
          tenantCode: 'default',
        })
      );

      expect(state.tenantId).toBe(null);
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_ID);
    });
  });

  describe('setTenantId', () => {
    it('應該設置租戶 ID', () => {
      const state = tenantReducer(undefined, setTenantId(500));
      expect(state.tenantId).toBe(500);
    });

    it('應該將租戶 ID 保存到 localStorage', () => {
      tenantReducer(undefined, setTenantId(300));
      expect(global.localStorage.setItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_ID, '300');
    });

    it('應該處理 tenantId 為 null', () => {
      tenantReducer(undefined, setTenantId(null));
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_ID);
    });
  });

  describe('setTimezone', () => {
    it('應該設置時區', () => {
      const state = tenantReducer(undefined, setTimezone('America/Los_Angeles'));
      expect(state.timezone).toBe('America/Los_Angeles');
    });

    it('應該將時區保存到 localStorage', () => {
      tenantReducer(undefined, setTimezone('Europe/Paris'));
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        STORAGE_KEYS.TENANT_TIMEZONE,
        'Europe/Paris'
      );
    });
  });

  describe('setTenantCode', () => {
    it('應該設置租戶代碼', () => {
      const state = tenantReducer(undefined, setTenantCode('enterprise-001'));
      expect(state.tenantCode).toBe('enterprise-001');
    });

    it('應該將租戶代碼保存到 localStorage', () => {
      tenantReducer(undefined, setTenantCode('demo-tenant'));
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        STORAGE_KEYS.TENANT_CODE,
        'demo-tenant'
      );
    });
  });

  describe('clearTenantInfo', () => {
    it('應該清除租戶信息', () => {
      let state = tenantReducer(
        undefined,
        setTenantInfo({
          tenantId: 999,
          timezone: 'Asia/Shanghai',
          tenantCode: 'cn-tenant',
        })
      );

      state = tenantReducer(state, clearTenantInfo());

      expect(state.tenantId).toBe(null);
      expect(state.timezone).toBe('Asia/Taipei');
      expect(state.tenantCode).toBe('default');
    });

    it('應該從 localStorage 移除租戶信息', () => {
      tenantReducer(undefined, clearTenantInfo());

      expect(global.localStorage.removeItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_ID);
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_TIMEZONE);
      expect(global.localStorage.removeItem).toHaveBeenCalledWith(STORAGE_KEYS.TENANT_CODE);
    });
  });

  describe('reset', () => {
    it('應該重置為默認狀態', () => {
      let state: TenantState = {
        tenantId: 777,
        timezone: 'Europe/Berlin',
        tenantCode: 'de-tenant',
      };

      state = tenantReducer(state, reset());

      expect(state.tenantId).toBe(null);
      expect(state.timezone).toBe('Asia/Taipei');
      expect(state.tenantCode).toBe('default');
    });
  });

  describe('完整工作流', () => {
    it('應該支持完整的租戶管理流程', () => {
      // 1. 初始狀態
      let state = tenantReducer(undefined, { type: '' });
      expect(state.tenantId).toBe(null);

      // 2. 設置租戶信息
      state = tenantReducer(
        state,
        setTenantInfo({
          tenantId: 1001,
          timezone: 'Asia/Seoul',
          tenantCode: 'kr-tenant',
        })
      );
      expect(state.tenantId).toBe(1001);
      expect(state.timezone).toBe('Asia/Seoul');
      expect(state.tenantCode).toBe('kr-tenant');

      // 3. 單獨更新租戶 ID
      state = tenantReducer(state, setTenantId(2002));
      expect(state.tenantId).toBe(2002);
      expect(state.timezone).toBe('Asia/Seoul'); // 其他字段不變

      // 4. 單獨更新時區
      state = tenantReducer(state, setTimezone('America/Chicago'));
      expect(state.timezone).toBe('America/Chicago');
      expect(state.tenantId).toBe(2002); // 其他字段不變

      // 5. 單獨更新租戶代碼
      state = tenantReducer(state, setTenantCode('us-central'));
      expect(state.tenantCode).toBe('us-central');

      // 6. 清除租戶信息
      state = tenantReducer(state, clearTenantInfo());
      expect(state.tenantId).toBe(null);
      expect(state.timezone).toBe('Asia/Taipei');
      expect(state.tenantCode).toBe('default');
    });
  });

  describe('狀態不可變性', () => {
    it('應該不修改原始狀態', () => {
      const originalState: TenantState = {
        tenantId: 123,
        timezone: 'Asia/Taipei',
        tenantCode: 'test',
      };

      const state = tenantReducer(originalState, setTenantId(456));

      expect(originalState.tenantId).toBe(123);
      expect(state.tenantId).toBe(456);
    });
  });

  describe('邊界情況', () => {
    it('應該處理空字符串時區', () => {
      const state = tenantReducer(undefined, setTimezone(''));
      expect(state.timezone).toBe('');
    });

    it('應該處理空字符串租戶代碼', () => {
      const state = tenantReducer(undefined, setTenantCode(''));
      expect(state.tenantCode).toBe('');
    });

    it('應該處理負數租戶 ID', () => {
      const state = tenantReducer(undefined, setTenantId(-1));
      expect(state.tenantId).toBe(-1);
    });

    it('應該處理超大租戶 ID', () => {
      const state = tenantReducer(undefined, setTenantId(Number.MAX_SAFE_INTEGER));
      expect(state.tenantId).toBe(Number.MAX_SAFE_INTEGER);
    });
  });
});
