/**
 * Role Management API
 * 角色管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';
import type { RoleVO, RoleAddForm, RoleUpdateForm } from '@/views/system/role/types';

/**
 * 角色管理 API
 */
export const roleApi = {
  /**
   * 查詢所有角色列表
   */
  queryAll: (): Promise<ResponseDTO<RoleVO[]>> => {
    return request.get('/role/getAll');
  },

  /**
   * 新增角色
   * @param params 新增表單數據
   */
  addRole: (params: RoleAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/role/add', params);
  },

  /**
   * 更新角色
   * @param params 更新表單數據
   */
  updateRole: (params: RoleUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/role/update', params);
  },

  /**
   * 刪除角色
   * @param roleId 角色ID
   */
  deleteRole: (roleId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/role/delete/${roleId}`);
  },
};
