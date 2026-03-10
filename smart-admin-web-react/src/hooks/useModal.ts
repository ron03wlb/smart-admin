/**
 * useModal Hook
 * Modal/Drawer 邏輯 Hook
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/goods/components/goods-form-modal.vue
 *
 * 功能：
 * - 顯示/隱藏狀態管理
 * - 表單數據管理
 * - 打開/關閉方法
 * - 提交處理
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { useState, useCallback } from 'react';

export interface UseModalOptions<TFormData> {
  /**
   * 表單默認值
   */
  defaultFormData?: TFormData;

  /**
   * 打開 Modal 時的回調
   * @param formData 表單數據（編輯時傳入）
   */
  onOpen?: (formData?: TFormData) => void;

  /**
   * 關閉 Modal 時的回調
   */
  onClose?: () => void;

  /**
   * 提交表單的回調
   * @param formData 表單數據
   * @returns Promise（可以是 API 調用）
   */
  onSubmit?: (formData: TFormData) => Promise<void>;
}

export interface UseModalResult<TFormData> {
  /**
   * Modal 是否可見
   */
  visible: boolean;

  /**
   * 設置 Modal 可見性
   */
  setVisible: React.Dispatch<React.SetStateAction<boolean>>;

  /**
   * 表單數據
   */
  formData: TFormData;

  /**
   * 設置表單數據
   */
  setFormData: React.Dispatch<React.SetStateAction<TFormData>>;

  /**
   * 是否編輯模式（根據是否有 ID 字段判斷）
   */
  isEdit: boolean;

  /**
   * 加載狀態（提交時）
   */
  loading: boolean;

  /**
   * 設置加載狀態
   */
  setLoading: React.Dispatch<React.SetStateAction<boolean>>;

  /**
   * 打開 Modal
   * @param data 表單數據（編輯時傳入）
   */
  open: (data?: TFormData) => void;

  /**
   * 關閉 Modal
   */
  close: () => void;

  /**
   * 處理提交
   */
  handleSubmit: () => Promise<void>;

  /**
   * 重置表單
   */
  reset: () => void;
}

/**
 * Modal/Drawer 邏輯 Hook
 */
export function useModal<TFormData extends Record<string, any>>(
  options: UseModalOptions<TFormData> = {}
): UseModalResult<TFormData> {
  const { defaultFormData = {} as TFormData, onOpen, onClose, onSubmit } = options;

  // Modal 可見性
  const [visible, setVisible] = useState(false);

  // 表單數據
  const [formData, setFormData] = useState<TFormData>(defaultFormData);

  // 加載狀態
  const [loading, setLoading] = useState(false);

  /**
   * 判斷是否編輯模式
   * 通常編輯模式會有 id 字段
   */
  const isEdit = useCallback(() => {
    const hasId = 'id' in formData && formData.id !== undefined && formData.id !== null;
    const hasIdVariant =
      Object.keys(formData).some(key => key.toLowerCase().endsWith('id')) &&
      Object.values(formData).some(value => value !== undefined && value !== null);
    return hasId || hasIdVariant;
  }, [formData]);

  /**
   * 打開 Modal
   */
  const open = useCallback(
    (data?: TFormData) => {
      // 重置表單為默認值
      setFormData(defaultFormData);

      // 如果傳入數據，則合併到表單
      if (data) {
        setFormData(prev => ({ ...prev, ...data }));
      }

      setVisible(true);

      // 執行打開回調
      if (onOpen) {
        onOpen(data);
      }
    },
    [defaultFormData, onOpen]
  );

  /**
   * 關閉 Modal
   */
  const close = useCallback(() => {
    setVisible(false);

      // 重置表單
    setFormData(defaultFormData);

    // 執行關閉回調
    if (onClose) {
      onClose();
    }
  }, [defaultFormData, onClose]);

  /**
   * 重置表單
   */
  const reset = useCallback(() => {
    setFormData(defaultFormData);
  }, [defaultFormData]);

  /**
   * 處理提交
   */
  const handleSubmit = useCallback(async () => {
    if (!onSubmit) {
      console.warn('useModal: onSubmit callback is not provided');
      return;
    }

    setLoading(true);
    try {
      await onSubmit(formData);
      // 提交成功後關閉 Modal
      close();
    } catch (error) {
      console.error('提交失敗:', error);
      // 不關閉 Modal，讓用戶可以修改後重試
      throw error;
    } finally {
      setLoading(false);
    }
  }, [formData, onSubmit, close]);

  return {
    visible,
    setVisible,
    formData,
    setFormData,
    isEdit: isEdit(),
    loading,
    setLoading,
    open,
    close,
    handleSubmit,
    reset,
  };
}
