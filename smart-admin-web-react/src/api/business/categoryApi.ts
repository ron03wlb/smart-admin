/**
 * Category API
 * 分類 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/business/category/category-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { getRequest, postRequest } from '@/utils/request';
import { ResponseDTO } from '@/api/types/response';

/**
 * 分類查詢參數
 */
export interface CategoryQueryForm {
  /** 分類類型 */
  categoryType: number;
}

/**
 * 分類樹節點
 */
export interface CategoryTreeNode {
  /** 分類ID */
  categoryId: number;
  /** 分類名稱 */
  categoryName: string;
  /** 父級ID */
  parentId: number;
  /** 分類類型 */
  categoryType: number;
  /** 排序 */
  sort?: number;
  /** 子節點 */
  children?: CategoryTreeNode[];

  // TreeSelect 需要的屬性
  value: number;
  title: string;
  key: number;
}

/**
 * 分類表單
 */
export interface CategoryForm {
  /** 分類ID（編輯時） */
  categoryId?: number;
  /** 分類名稱 */
  categoryName: string;
  /** 父級ID */
  parentId: number;
  /** 分類類型 */
  categoryType: number;
  /** 排序 */
  sort?: number;
}

/**
 * 查詢分類層級樹
 */
export function queryCategoryTree(params: CategoryQueryForm): Promise<ResponseDTO<CategoryTreeNode[]>> {
  return postRequest('/category/tree', params);
}

/**
 * 添加分類
 */
export function addCategory(data: CategoryForm): Promise<ResponseDTO<void>> {
  return postRequest('/category/add', data);
}

/**
 * 更新分類
 */
export function updateCategory(data: CategoryForm): Promise<ResponseDTO<void>> {
  return postRequest('/category/update', data);
}

/**
 * 刪除分類
 */
export function deleteCategoryById(categoryId: number): Promise<ResponseDTO<void>> {
  return getRequest(`/category/delete/${categoryId}`);
}
