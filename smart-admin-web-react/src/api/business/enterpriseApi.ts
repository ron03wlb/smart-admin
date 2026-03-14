/**
 * Enterprise Management API
 * 企業管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  EnterpriseVO,
  EnterpriseQueryForm,
  EnterpriseAddForm,
  EnterpriseUpdateForm,
} from '@/views/business/enterprise/types';

/**
 * 企業管理 API
 */
export const enterpriseApi = {
  /**
   * 分頁查詢企業列表
   * @param params 查詢參數
   */
  pageQuery: (params: EnterpriseQueryForm): Promise<ResponseDTO<PageResult<EnterpriseVO>>> => {
    return request.post('/oa/enterprise/page/query', params);
  },

  /**
   * 查詢企業詳情
   * @param enterpriseId 企業ID
   */
  detail: (enterpriseId: number): Promise<ResponseDTO<EnterpriseVO>> => {
    return request.get(`/oa/enterprise/get/${enterpriseId}`);
  },

  /**
   * 新增企業
   * @param params 新增表單數據
   */
  create: (params: EnterpriseAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/oa/enterprise/create', params);
  },

  /**
   * 更新企業
   * @param params 更新表單數據
   */
  update: (params: EnterpriseUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/oa/enterprise/update', params);
  },

  /**
   * 刪除企業
   * @param enterpriseId 企業ID
   */
  delete: (enterpriseId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/oa/enterprise/delete/${enterpriseId}`);
  },

  /**
   * 查詢企業列表（不分頁）
   * @param type 企業類型（可選）
   */
  queryList: (type?: number): Promise<ResponseDTO<EnterpriseVO[]>> => {
    const query = type ? `?type=${type}` : '';
    return request.get(`/oa/enterprise/query/list${query}`);
  },
};
