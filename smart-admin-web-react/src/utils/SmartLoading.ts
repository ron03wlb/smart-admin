/**
 * SmartLoading 工具類
 * 全局加載指示器
 *
 * 參考：Vue 版本 smart-admin-web/src/components/framework/smart-loading/index.ts
 *
 * 用法：
 * ```typescript
 * SmartLoading.show(); // 顯示 loading
 * SmartLoading.hide(); // 隱藏 loading
 * ```
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { store } from '@/store';
import { showLoading, hideLoading } from '@/store/slices/spinSlice';

export const SmartLoading = {
  /**
   * 顯示全局 loading
   */
  show: () => {
    store.dispatch(showLoading());

    // 調整 z-index（與 Vue 版本保持一致）
    try {
      const spins = document.querySelector('.ant-spin-nested-loading');
      if (spins) {
        (spins as HTMLElement).style.zIndex = '1001';
      }
    } catch (error) {
      console.error('SmartLoading show 操作失敗:', error);
    }
  },

  /**
   * 隱藏全局 loading
   */
  hide: () => {
    store.dispatch(hideLoading());

    // 恢復 z-index（與 Vue 版本保持一致）
    try {
      const spins = document.querySelector('.ant-spin-nested-loading');
      if (spins) {
        (spins as HTMLElement).style.zIndex = '999';
      }
    } catch (error) {
      console.error('SmartLoading hide 操作失敗:', error);
    }
  },
};
