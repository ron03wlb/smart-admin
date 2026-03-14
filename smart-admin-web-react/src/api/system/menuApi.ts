/**
 * Menu Management API
 * 菜單管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';
import type {
  MenuVO,
  MenuAddForm,
  MenuUpdateForm,
} from '@/views/system/menu/types';

/**
 * 菜單管理 API
 */
export const menuApi = {
  /**
   * 查詢所有菜單列表（扁平結構）
   */
  queryMenu: (): Promise<ResponseDTO<MenuVO[]>> => {
    return request.get('/menu/query');
  },

  /**
   * 查詢菜單樹（樹形結構）
   * @param onlyMenu 是否只查詢菜單（不包含功能點）
   */
  queryMenuTree: (onlyMenu?: boolean): Promise<ResponseDTO<MenuVO[]>> => {
    return request.get(`/menu/tree?onlyMenu=${onlyMenu || false}`);
  },

  /**
   * 新增菜單
   * @param params 新增表單數據
   */
  addMenu: (params: MenuAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/menu/add', params);
  },

  /**
   * 更新菜單
   * @param params 更新表單數據
   */
  updateMenu: (params: MenuUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/menu/update', params);
  },

  /**
   * 批量刪除菜單
   * @param menuIdList 菜單ID列表
   */
  batchDeleteMenu: (menuIdList: number[]): Promise<ResponseDTO<void>> => {
    return request.get(`/menu/batchDelete?menuIdList=${menuIdList.join(',')}`);
  },

  /**
   * 獲取所有授權 URL
   */
  getAuthUrl: (): Promise<ResponseDTO<string[]>> => {
    return request.get('/menu/auth/url');
  },
};
