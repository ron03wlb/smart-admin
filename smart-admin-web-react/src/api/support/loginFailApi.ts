/**
 * Login Fail API
 * 登錄失敗 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import request from '@/utils/request';
import { ResponseDTO, PageResult } from '@/api/types/response';
import type { LoginFailVO, LoginFailQueryForm } from '@/views/support/login-fail/types';

/**
 * 登錄失敗 API
 */
export const loginFailApi = {
  /**
   * 分頁查詢登錄失敗記錄
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPage: (form: LoginFailQueryForm): Promise<ResponseDTO<PageResult<LoginFailVO>>> => {
    return request.post('/support/protect/loginFail/queryPage', form);
  },

  /**
   * 批量解除鎖定（刪除鎖定記錄）
   *
   * @param idList ID 列表
   * @returns 操作結果
   */
  batchDelete: (idList: number[]): Promise<ResponseDTO<string>> => {
    return request.post('/support/protect/loginFail/batchDelete', idList);
  },
};
