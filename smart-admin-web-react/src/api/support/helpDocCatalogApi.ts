/**
 * Help Doc Catalog API
 * 幫助文檔目錄 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';
import type { HelpDocCatalogVO, HelpDocCatalogFormData } from '@/views/support/help-doc/types';

export const helpDocCatalogApi = {
  /**
   * 獲取全部目錄列表
   */
  getAll: (): Promise<ResponseDTO<HelpDocCatalogVO[]>> => {
    return request.get('/support/helpDoc/helpDocCatalog/getAll');
  },

  /**
   * 添加目錄
   */
  add: (data: HelpDocCatalogFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/helpDoc/helpDocCatalog/add', data);
  },

  /**
   * 更新目錄
   */
  update: (data: HelpDocCatalogFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/helpDoc/helpDocCatalog/update', data);
  },

  /**
   * 刪除目錄
   */
  delete: (helpDocCatalogId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/support/helpDoc/helpDocCatalog/delete/${helpDocCatalogId}`);
  },
};
