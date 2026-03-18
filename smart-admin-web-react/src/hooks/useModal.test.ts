/**
 * useModal Hook Tests
 * useModal Hook 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useModal } from './useModal';

interface TestFormData {
  id?: number;
  name: string;
  age?: number;
}

describe('useModal', () => {
  const defaultFormData: TestFormData = {
    name: '',
    age: undefined,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('基本功能', () => {
    it('應該初始化默認狀態', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      expect(result.current.visible).toBe(false);
      expect(result.current.formData).toEqual(defaultFormData);
      expect(result.current.isEdit).toBe(false);
      expect(result.current.loading).toBe(false);
    });

    it('應該接受自定義默認表單數據', () => {
      const customDefaultData: TestFormData = {
        name: '測試',
        age: 18,
      };

      const { result } = renderHook(() => useModal({ defaultFormData: customDefaultData }));

      expect(result.current.formData).toEqual(customDefaultData);
    });
  });

  describe('open 方法', () => {
    it('應該打開 Modal', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      act(() => {
        result.current.open();
      });

      expect(result.current.visible).toBe(true);
    });

    it('應該打開 Modal 並填充數據（編輯模式）', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      const editData: TestFormData = {
        id: 1,
        name: '張三',
        age: 25,
      };

      act(() => {
        result.current.open(editData);
      });

      expect(result.current.visible).toBe(true);
      expect(result.current.formData).toEqual(editData);
      expect(result.current.isEdit).toBe(true);
    });

    it('應該執行 onOpen 回調', () => {
      const onOpen = vi.fn();
      const { result } = renderHook(() => useModal({ defaultFormData, onOpen }));

      const editData: TestFormData = {
        id: 1,
        name: '張三',
        age: 25,
      };

      act(() => {
        result.current.open(editData);
      });

      expect(onOpen).toHaveBeenCalledWith(editData);
      expect(onOpen).toHaveBeenCalledTimes(1);
    });

    it('應該在新增模式下打開（無數據）', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      act(() => {
        result.current.open();
      });

      expect(result.current.visible).toBe(true);
      expect(result.current.formData).toEqual(defaultFormData);
      expect(result.current.isEdit).toBe(false);
    });
  });

  describe('close 方法', () => {
    it('應該關閉 Modal', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      // 先打開
      act(() => {
        result.current.open();
      });

      expect(result.current.visible).toBe(true);

      // 再關閉
      act(() => {
        result.current.close();
      });

      expect(result.current.visible).toBe(false);
    });

    it('應該關閉 Modal 並重置表單', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      const editData: TestFormData = {
        id: 1,
        name: '張三',
        age: 25,
      };

      // 打開並填充數據
      act(() => {
        result.current.open(editData);
      });

      expect(result.current.formData).toEqual(editData);

      // 關閉
      act(() => {
        result.current.close();
      });

      expect(result.current.visible).toBe(false);
      expect(result.current.formData).toEqual(defaultFormData);
    });

    it('應該執行 onClose 回調', () => {
      const onClose = vi.fn();
      const { result } = renderHook(() => useModal({ defaultFormData, onClose }));

      // 打開
      act(() => {
        result.current.open();
      });

      // 關閉
      act(() => {
        result.current.close();
      });

      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('reset 方法', () => {
    it('應該重置表單到默認值', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      // 修改表單數據
      act(() => {
        result.current.setFormData({
          id: 1,
          name: '張三',
          age: 25,
        });
      });

      expect(result.current.formData.name).toBe('張三');

      // 重置
      act(() => {
        result.current.reset();
      });

      expect(result.current.formData).toEqual(defaultFormData);
    });
  });

  describe('isEdit 判斷', () => {
    it('應該在有 id 字段時判斷為編輯模式', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      const editData: TestFormData = {
        id: 1,
        name: '張三',
      };

      act(() => {
        result.current.open(editData);
      });

      expect(result.current.isEdit).toBe(true);
    });

    it('應該在無 id 字段時判斷為新增模式', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      act(() => {
        result.current.open();
      });

      expect(result.current.isEdit).toBe(false);
    });

    it('應該在有其他 ID 字段時判斷為編輯模式（如 goodsId）', () => {
      interface GoodsFormData {
        goodsId?: number;
        name: string;
      }

      const goodsDefaultData: GoodsFormData = {
        name: '',
      };

      const { result } = renderHook(() => useModal({ defaultFormData: goodsDefaultData }));

      const editData: GoodsFormData = {
        goodsId: 1,
        name: '商品A',
      };

      act(() => {
        result.current.open(editData);
      });

      expect(result.current.isEdit).toBe(true);
    });
  });

  describe('handleSubmit 方法', () => {
    it('應該調用 onSubmit 回調', async () => {
      const onSubmit = vi.fn().mockResolvedValue(undefined);
      const { result } = renderHook(() => useModal({ defaultFormData, onSubmit }));

      const editData: TestFormData = {
        id: 1,
        name: '張三',
        age: 25,
      };

      act(() => {
        result.current.open(editData);
      });

      await act(async () => {
        await result.current.handleSubmit();
      });

      expect(onSubmit).toHaveBeenCalledWith(editData);
      expect(onSubmit).toHaveBeenCalledTimes(1);
    });

    it('應該在提交成功後關閉 Modal', async () => {
      const onSubmit = vi.fn().mockResolvedValue(undefined);
      const { result } = renderHook(() => useModal({ defaultFormData, onSubmit }));

      act(() => {
        result.current.open();
      });

      expect(result.current.visible).toBe(true);

      await act(async () => {
        await result.current.handleSubmit();
      });

      await waitFor(() => {
        expect(result.current.visible).toBe(false);
      });
    });

    it('應該在提交時設置 loading 狀態', async () => {
      let resolveSubmit: any;
      const onSubmit = vi.fn(
        () =>
          new Promise(resolve => {
            resolveSubmit = resolve;
          })
      );

      const { result } = renderHook(() => useModal({ defaultFormData, onSubmit }));

      act(() => {
        result.current.open();
      });

      // 開始提交
      act(() => {
        result.current.handleSubmit();
      });

      // loading 應該為 true
      expect(result.current.loading).toBe(true);

      // 完成提交
      await act(async () => {
        resolveSubmit();
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });
    });

    it('應該在提交失敗時保持 Modal 打開', async () => {
      const onSubmit = vi.fn().mockRejectedValue(new Error('提交失敗'));
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const { result } = renderHook(() => useModal({ defaultFormData, onSubmit }));

      act(() => {
        result.current.open();
      });

      expect(result.current.visible).toBe(true);

      await act(async () => {
        try {
          await result.current.handleSubmit();
        } catch (error) {
          // 預期拋出錯誤
        }
      });

      // Modal 應該保持打開
      expect(result.current.visible).toBe(true);

      consoleErrorSpy.mockRestore();
    });

    it('應該在未提供 onSubmit 時顯示警告', async () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const { result } = renderHook(() => useModal({ defaultFormData }));

      act(() => {
        result.current.open();
      });

      await act(async () => {
        await result.current.handleSubmit();
      });

      expect(consoleWarnSpy).toHaveBeenCalledWith('useModal: onSubmit callback is not provided');

      consoleWarnSpy.mockRestore();
    });
  });

  describe('狀態更新', () => {
    it('應該允許手動設置 visible', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      act(() => {
        result.current.setVisible(true);
      });

      expect(result.current.visible).toBe(true);
    });

    it('應該允許手動設置 formData', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      const newData: TestFormData = {
        id: 1,
        name: '李四',
        age: 30,
      };

      act(() => {
        result.current.setFormData(newData);
      });

      expect(result.current.formData).toEqual(newData);
    });

    it('應該允許手動設置 loading', () => {
      const { result } = renderHook(() => useModal({ defaultFormData }));

      act(() => {
        result.current.setLoading(true);
      });

      expect(result.current.loading).toBe(true);
    });
  });

  describe('完整工作流', () => {
    it('應該支持完整的新增流程', async () => {
      const onOpen = vi.fn();
      const onClose = vi.fn();
      const onSubmit = vi.fn().mockResolvedValue(undefined);

      const { result } = renderHook(() =>
        useModal({
          defaultFormData,
          onOpen,
          onClose,
          onSubmit,
        })
      );

      // 1. 打開 Modal（新增模式）
      act(() => {
        result.current.open();
      });

      expect(result.current.visible).toBe(true);
      expect(result.current.isEdit).toBe(false);
      expect(onOpen).toHaveBeenCalled();

      // 2. 填寫表單
      act(() => {
        result.current.setFormData({
          name: '新用戶',
          age: 20,
        });
      });

      // 3. 提交
      await act(async () => {
        await result.current.handleSubmit();
      });

      expect(onSubmit).toHaveBeenCalledWith({
        name: '新用戶',
        age: 20,
      });

      // 4. 提交成功後應該關閉 Modal
      await waitFor(() => {
        expect(result.current.visible).toBe(false);
      });

      expect(onClose).toHaveBeenCalled();
    });

    it('應該支持完整的編輯流程', async () => {
      const onOpen = vi.fn();
      const onClose = vi.fn();
      const onSubmit = vi.fn().mockResolvedValue(undefined);

      const { result } = renderHook(() =>
        useModal({
          defaultFormData,
          onOpen,
          onClose,
          onSubmit,
        })
      );

      const editData: TestFormData = {
        id: 1,
        name: '張三',
        age: 25,
      };

      // 1. 打開 Modal（編輯模式）
      act(() => {
        result.current.open(editData);
      });

      expect(result.current.visible).toBe(true);
      expect(result.current.isEdit).toBe(true);
      expect(result.current.formData).toEqual(editData);
      expect(onOpen).toHaveBeenCalledWith(editData);

      // 2. 修改表單
      act(() => {
        result.current.setFormData({
          ...result.current.formData,
          age: 26,
        });
      });

      // 3. 提交
      await act(async () => {
        await result.current.handleSubmit();
      });

      expect(onSubmit).toHaveBeenCalledWith({
        id: 1,
        name: '張三',
        age: 26,
      });

      // 4. 提交成功後應該關閉 Modal
      await waitFor(() => {
        expect(result.current.visible).toBe(false);
      });

      expect(onClose).toHaveBeenCalled();
    });
  });
});
