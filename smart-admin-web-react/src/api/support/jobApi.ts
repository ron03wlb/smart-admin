/**
 * Job Management API
 * 定時任務 API 封裝
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/job-api.ts
 * 後端接口：AdminSmartJobController.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/types/common';
import type {
  JobVO,
  JobQueryForm,
  JobAddForm,
  JobUpdateForm,
  JobEnabledUpdateForm,
  JobExecuteForm,
} from '@/views/support/job/types';

/**
 * Job API
 */
export const jobApi = {
  /**
   * 分頁查詢
   * @param params 查詢參數
   */
  queryJob: (params: JobQueryForm): Promise<ResponseDTO<PageResult<JobVO>>> => {
    return request.post('/support/job/query', params);
  },

  /**
   * 查詢詳情
   * @param jobId 任務 ID
   */
  queryJobInfo: (jobId: number): Promise<ResponseDTO<JobVO>> => {
    return request.get(`/support/job/${jobId}`);
  },

  /**
   * 新增
   * @param params 新增參數
   */
  addJob: (params: JobAddForm): Promise<ResponseDTO<string>> => {
    return request.post('/support/job/add', params);
  },

  /**
   * 更新
   * @param params 更新參數
   */
  updateJob: (params: JobUpdateForm): Promise<ResponseDTO<string>> => {
    return request.post('/support/job/update', params);
  },

  /**
   * 更新啟用狀態
   * @param params 更新參數
   */
  updateJobEnabled: (params: JobEnabledUpdateForm): Promise<ResponseDTO<string>> => {
    return request.post('/support/job/update/enabled', params);
  },

  /**
   * 立即執行
   * @param params 執行參數
   */
  executeJob: (params: JobExecuteForm): Promise<ResponseDTO<string>> => {
    return request.post('/support/job/execute', params);
  },

  /**
   * 刪除
   * @param jobId 任務 ID
   */
  deleteJob: (jobId: number): Promise<ResponseDTO<string>> => {
    return request.get(`/support/job/delete?jobId=${jobId}`);
  },
};
