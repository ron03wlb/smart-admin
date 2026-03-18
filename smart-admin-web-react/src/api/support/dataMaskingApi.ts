/**
 * Data Masking API
 * 數據脫敏 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/data-masking-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';

/**
 * 數據脫敏示例 VO
 */
export interface DataMaskingVO {
  userId: number;
  other: string;
  phone: string;
  idCard: string;
  password: string;
  email: string;
  carLicense: string;
  bankCard: string;
  address: string;
}

/**
 * 數據脫敏 API
 */
export const dataMaskingApi = {
  /**
   * 查詢脫敏數據示例
   */
  query: (): Promise<ResponseDTO<DataMaskingVO[]>> => {
    return request.get('/support/dataMasking/demo/query');
  },
};
