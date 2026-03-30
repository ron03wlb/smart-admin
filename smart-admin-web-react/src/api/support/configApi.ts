/**
 * Config API
 * 配置 API 層
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type { ConfigVO, ConfigFormData, ConfigQueryForm } from '@/views/support/config/types';

/**
 * 配置 API
 */
export const configApi = {
  /**
   * 分頁查詢配置列表
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPage: (form: ConfigQueryForm): Promise<ResponseDTO<PageResult<ConfigVO>>> => {
    return request.post('/support/config/query', form);
  },

  /**
   * 新增配置
   * @param form 表單數據
   * @returns 響應結果
   */
  addConfig: (form: ConfigFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/config/add', form);
  },

  /**
   * 更新配置
   * @param form 表單數據
   * @returns 響應結果
   */
  updateConfig: (form: ConfigFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/config/update', form);
  },

  /**
   * 根據 Key 查詢配置
   * @param configKey 配置 Key
   * @returns 配置詳情
   */
  queryByKey: (configKey: string): Promise<ResponseDTO<ConfigVO>> => {
    return request.get(`/support/config/queryByKey?configKey=${configKey}`);
  },
};
