/**
 * Operate Log API
 * 操作日誌 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type { OperateLogVO, OperateLogQueryForm } from '@/views/support/operate-log/types';

/**
 * 操作日誌 API
 */
export const operateLogApi = {
  /**
   * 分頁查詢操作日誌
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPage: (form: OperateLogQueryForm): Promise<ResponseDTO<PageResult<OperateLogVO>>> => {
    return request.post('/support/operateLog/page/query', form);
  },

  /**
   * 查詢操作日誌詳情
   *
   * @param id 日誌ID
   * @returns 詳情數據
   */
  detail: (id: number): Promise<ResponseDTO<OperateLogVO>> => {
    return request.get(`/support/operateLog/detail/${id}`);
  },

  /**
   * 分頁查詢當前登錄人的操作日誌
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPageLogin: (form: OperateLogQueryForm): Promise<ResponseDTO<PageResult<OperateLogVO>>> => {
    return request.post('/support/operateLog/page/query/login', form);
  },
};
