/**
 * Cache API
 * 緩存 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';

export const cacheApi = {
  /**
   * 獲取所有緩存名稱
   */
  getAllCacheNames: (): Promise<ResponseDTO<string[]>> => {
    return request.get('/support/cache/names');
  },

  /**
   * 移除某個緩存
   */
  remove: (cacheName: string): Promise<ResponseDTO<void>> => {
    return request.get(`/support/cache/remove/${cacheName}`);
  },

  /**
   * 獲取某個緩存的所有 key
   */
  getKeys: (cacheName: string): Promise<ResponseDTO<string[]>> => {
    return request.get(`/support/cache/keys/${cacheName}`);
  },
};
