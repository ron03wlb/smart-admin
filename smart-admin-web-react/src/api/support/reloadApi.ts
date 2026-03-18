/**
 * Reload API
 * 重載 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';
import type { ReloadVO, ReloadFormData, ReloadResultVO } from '@/views/support/reload/types';

export const reloadApi = {
  /**
   * 查詢 reload 列表
   */
  queryList: (): Promise<ResponseDTO<ReloadVO[]>> => {
    return request.get('/support/reload/query');
  },

  /**
   * 執行 reload
   */
  reload: (data: ReloadFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/reload/update', data);
  },

  /**
   * 獲取 reload 結果
   */
  queryReloadResult: (tag: string): Promise<ResponseDTO<ReloadResultVO[]>> => {
    return request.get(`/support/reload/result/${tag}`);
  },
};
