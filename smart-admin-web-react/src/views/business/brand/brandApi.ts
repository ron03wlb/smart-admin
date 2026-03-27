/**
 * Brand Module API
 * 品牌模塊 API 層
 *
 * 參考：後端 BrandController.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import type { ResponseDTO, PageResult } from '@/types/response';
import type { BrandVO, BrandQueryForm, BrandAddForm, BrandUpdateForm } from './types';
import { postRequest, getRequest } from '@/utils/request';

/**
 * Brand API
 */
export const brandApi = {
  /**
   * Query brands with pagination
   * 分頁查詢品牌
   */
  queryBrand(queryForm: BrandQueryForm): Promise<ResponseDTO<PageResult<BrandVO>>> {
    return postRequest('/brand/query', queryForm);
  },

  /**
   * Add brand
   * 新增品牌
   */
  addBrand(addForm: BrandAddForm): Promise<ResponseDTO<string>> {
    return postRequest('/brand/add', addForm);
  },

  /**
   * Update brand
   * 更新品牌
   */
  updateBrand(updateForm: BrandUpdateForm): Promise<ResponseDTO<string>> {
    return postRequest('/brand/update', updateForm);
  },

  /**
   * Batch delete brands
   * 批量刪除品牌
   */
  batchDelete(brandIdList: number[]): Promise<ResponseDTO<string>> {
    return postRequest('/brand/batchDelete', brandIdList);
  },

  /**
   * Get brand by ID
   * 根據ID獲取品牌
   */
  getById(brandId: number): Promise<ResponseDTO<BrandVO>> {
    return getRequest(`/brand/get/${brandId}`);
  },
};
