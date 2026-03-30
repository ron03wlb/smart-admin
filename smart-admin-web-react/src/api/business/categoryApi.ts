/**
 * Category API
 * 分類 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/business/category/category-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';
import type {
  CategoryVO,
  CategoryTreeQueryForm,
  CategoryAddForm,
  CategoryUpdateForm,
} from '@/views/business/category/types';

/**
 * 分類 API
 */
export const categoryApi = {
  /**
   * 查詢分類樹
   * @param params 查詢參數（包含 categoryType）
   */
  queryCategoryTree: (params: CategoryTreeQueryForm): Promise<ResponseDTO<CategoryVO[]>> => {
    return request.post('/category/tree', params);
  },

  /**
   * 添加分類
   * @param params 分類信息
   */
  addCategory: (params: CategoryAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/category/add', params);
  },

  /**
   * 更新分類信息
   * @param params 分類信息
   */
  updateCategory: (params: CategoryUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/category/update', params);
  },

  /**
   * 刪除分類
   * @param categoryId 分類 ID
   */
  deleteCategory: (categoryId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/category/delete/${categoryId}`);
  },
};

/**
 * 命名導出：queryCategoryTree 函數
 * 用於 CategoryTreeSelect 組件
 */
export const queryCategoryTree = categoryApi.queryCategoryTree;

/**
 * 類型別名：CategoryTreeNode
 * 實際上就是 CategoryVO（包含 children 屬性用於樹形結構）
 */
export type CategoryTreeNode = CategoryVO;
