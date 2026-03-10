/**
 * SmartLoading 工具類測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SmartLoading } from './SmartLoading';
import { store } from '@/store';
import { selectLoading } from '@/store/slices/spinSlice';

describe('SmartLoading', () => {
  beforeEach(() => {
    // 清理 loading 狀態
    if (selectLoading(store.getState())) {
      SmartLoading.hide();
    }
  });

  afterEach(() => {
    // 清理 loading 狀態
    if (selectLoading(store.getState())) {
      SmartLoading.hide();
    }
  });

  describe('show 方法', () => {
    it('應該顯示全局 loading', () => {
      SmartLoading.show();

      const loading = selectLoading(store.getState());
      expect(loading).toBe(true);
    });

    it('應該能夠多次調用 show', () => {
      SmartLoading.show();
      SmartLoading.show();
      SmartLoading.show();

      const loading = selectLoading(store.getState());
      expect(loading).toBe(true);
    });
  });

  describe('hide 方法', () => {
    it('應該隱藏全局 loading', () => {
      // 先顯示
      SmartLoading.show();
      expect(selectLoading(store.getState())).toBe(true);

      // 再隱藏
      SmartLoading.hide();
      expect(selectLoading(store.getState())).toBe(false);
    });

    it('應該能夠在未顯示時調用 hide', () => {
      // 直接調用 hide（沒有先 show）
      SmartLoading.hide();

      const loading = selectLoading(store.getState());
      expect(loading).toBe(false);
    });

    it('應該能夠多次調用 hide', () => {
      SmartLoading.show();
      SmartLoading.hide();
      SmartLoading.hide();
      SmartLoading.hide();

      const loading = selectLoading(store.getState());
      expect(loading).toBe(false);
    });
  });

  describe('show 和 hide 組合使用', () => {
    it('應該能夠切換 loading 狀態', () => {
      // 初始狀態
      expect(selectLoading(store.getState())).toBe(false);

      // 顯示
      SmartLoading.show();
      expect(selectLoading(store.getState())).toBe(true);

      // 隱藏
      SmartLoading.hide();
      expect(selectLoading(store.getState())).toBe(false);

      // 再次顯示
      SmartLoading.show();
      expect(selectLoading(store.getState())).toBe(true);

      // 再次隱藏
      SmartLoading.hide();
      expect(selectLoading(store.getState())).toBe(false);
    });

    it('應該模擬異步操作的典型使用場景', async () => {
      // 模擬異步操作
      const mockAsyncOperation = async () => {
        SmartLoading.show();
        try {
          // 模擬 API 請求
          await new Promise(resolve => setTimeout(resolve, 50));
        } finally {
          SmartLoading.hide();
        }
      };

      // 執行前
      expect(selectLoading(store.getState())).toBe(false);

      // 執行異步操作
      const promise = mockAsyncOperation();

      // 執行中（應該顯示 loading）
      expect(selectLoading(store.getState())).toBe(true);

      // 等待完成
      await promise;

      // 執行後（應該隱藏 loading）
      expect(selectLoading(store.getState())).toBe(false);
    });
  });

  describe('DOM 操作', () => {
    it('應該不會因為找不到 DOM 元素而拋出錯誤', () => {
      // querySelector 可能返回 null，但不應該導致錯誤
      expect(() => {
        SmartLoading.show();
        SmartLoading.hide();
      }).not.toThrow();
    });

    it('應該安全處理 DOM 操作異常', () => {
      // Mock querySelector 拋出異常
      const originalQuerySelector = document.querySelector;
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      document.querySelector = vi.fn(() => {
        throw new Error('DOM error');
      });

      // 不應該拋出錯誤
      expect(() => {
        SmartLoading.show();
        SmartLoading.hide();
      }).not.toThrow();

      // 應該記錄錯誤
      expect(consoleErrorSpy).toHaveBeenCalled();

      // 恢復原始函數
      document.querySelector = originalQuerySelector;
      consoleErrorSpy.mockRestore();
    });
  });

  describe('狀態一致性', () => {
    it('應該保持 Redux store 狀態一致', () => {
      // 多次操作
      SmartLoading.show();
      SmartLoading.show();
      expect(selectLoading(store.getState())).toBe(true);

      SmartLoading.hide();
      expect(selectLoading(store.getState())).toBe(false);

      SmartLoading.hide();
      expect(selectLoading(store.getState())).toBe(false);
    });
  });
});
