/**
 * ChangeLog Management API
 * 系統更新日誌 API 封裝
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/change-log-api.ts
 * 後端接口：ChangeLogController.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/types/common';
import type {
  ChangeLogVO,
  ChangeLogQueryForm,
  ChangeLogAddForm,
  ChangeLogUpdateForm,
} from '@/views/support/change-log/types';

/**
 * ChangeLog API
 */
export const changeLogApi = {
  /**
   * 分頁查詢
   * @param params 查詢參數
   */
  queryPage: (params: ChangeLogQueryForm): Promise<ResponseDTO<PageResult<ChangeLogVO>>> => {
    return request.post('/support/changeLog/queryPage', params);
  },

  /**
   * 獲取詳情
   * @param changeLogId 更新日誌 ID
   */
  getDetail: (changeLogId: number): Promise<ResponseDTO<ChangeLogVO>> => {
    return request.get(`/support/changeLog/getDetail/${changeLogId}`);
  },

  /**
   * 新增
   * @param params 新增參數
   */
  add: (params: ChangeLogAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/support/changeLog/add', params);
  },

  /**
   * 更新
   * @param params 更新參數
   */
  update: (params: ChangeLogUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/support/changeLog/update', params);
  },

  /**
   * 刪除
   * @param changeLogId 更新日誌 ID
   */
  delete: (changeLogId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/support/changeLog/delete/${changeLogId}`);
  },

  /**
   * 批量刪除
   * @param idList ID 列表
   */
  batchDelete: (idList: number[]): Promise<ResponseDTO<void>> => {
    return request.post('/support/changeLog/batchDelete', idList);
  },
};
