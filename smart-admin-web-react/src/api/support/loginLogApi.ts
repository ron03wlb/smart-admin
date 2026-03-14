/**
 * Login Log API
 * 登錄日誌 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import request from '@/utils/request';
import { ResponseDTO, PageResult } from '@/api/types/response';
import type { LoginLogVO, LoginLogQueryForm } from '@/views/support/login-log/types';

/**
 * 登錄日誌 API
 */
export const loginLogApi = {
  /**
   * 分頁查詢登錄日誌
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPage: (form: LoginLogQueryForm): Promise<ResponseDTO<PageResult<LoginLogVO>>> => {
    return request.post('/support/loginLog/page/query', form);
  },

  /**
   * 分頁查詢當前登錄人的登錄日誌
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPageLogin: (form: LoginLogQueryForm): Promise<ResponseDTO<PageResult<LoginLogVO>>> => {
    return request.post('/support/loginLog/page/query/login', form);
  },
};
