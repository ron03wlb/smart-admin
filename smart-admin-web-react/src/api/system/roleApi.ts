/**
 * Role Management API
 * 角色管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  RoleVO,
  RoleAddForm,
  RoleUpdateForm,
  RoleMenuSelectedVO,
  RoleMenuUpdateForm,
  RoleEmployeeQueryForm,
  RoleEmployeeVO,
  RoleEmployeeBatchForm,
  DataScopeVO,
  DataScopeUpdateForm,
} from '@/views/system/role/types';

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

  // ==================== 角色-菜單權限 API ====================

  /**
   * 獲取角色已選菜單
   * @param roleId 角色ID
   */
  getRoleSelectedMenu: (roleId: number): Promise<ResponseDTO<RoleMenuSelectedVO>> => {
    return request.get(`/role/getRoleSelectedMenu/${roleId}`);
  },

  /**
   * 更新角色菜單權限
   * @param params 角色菜單更新表單
   */
  updateRoleMenu: (params: RoleMenuUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/role/menu/updateRoleMenu', params);
  },

  // ==================== 角色-員工管理 API ====================

  /**
   * 查詢角色員工列表（分頁）
   * @param params 查詢表單
   */
  queryRoleEmployee: (params: RoleEmployeeQueryForm): Promise<ResponseDTO<PageResult<RoleEmployeeVO>>> => {
    return request.post('/role/employee/queryRoleEmployee', params);
  },

  /**
   * 獲取角色所有員工
   * @param roleId 角色ID
   */
  getRoleAllEmployee: (roleId: number): Promise<ResponseDTO<RoleEmployeeVO[]>> => {
    return request.get(`/role/employee/getAllEmployee/${roleId}`);
  },

  /**
   * 批量添加角色員工
   * @param params 批量添加表單
   */
  batchAddRoleEmployee: (params: RoleEmployeeBatchForm): Promise<ResponseDTO<void>> => {
    return request.post('/role/employee/batchAdd', params);
  },

  /**
   * 刪除員工角色
   * @param employeeId 員工ID
   * @param roleId 角色ID
   */
  deleteEmployeeRole: (employeeId: number, roleId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/role/employee/remove/${employeeId}/${roleId}`);
  },

  /**
   * 批量移除角色員工
   * @param params 批量移除表單
   */
  batchRemoveRoleEmployee: (params: RoleEmployeeBatchForm): Promise<ResponseDTO<void>> => {
    return request.post('/role/employee/batchRemove', params);
  },

  // ==================== 角色-數據範圍 API ====================

  /**
   * 獲取數據範圍列表
   */
  getDataScopeList: (): Promise<ResponseDTO<DataScopeVO[]>> => {
    return request.get('/role/dataScope/list');
  },

  /**
   * 根據角色ID獲取數據範圍
   * @param roleId 角色ID
   */
  getDataScopeByRoleId: (roleId: number): Promise<ResponseDTO<DataScopeVO[]>> => {
    return request.get(`/role/dataScope/getRoleDataScopeList/${roleId}`);
  },

  /**
   * 更新數據範圍
   * @param params 數據範圍更新表單
   */
  updateDataScope: (params: DataScopeUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/role/dataScope/update', params);
  },
};
