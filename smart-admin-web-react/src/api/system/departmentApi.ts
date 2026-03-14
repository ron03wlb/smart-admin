/**
 * Department Management API
 * 部門管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';
import type {
  DepartmentVO,
  DepartmentAddForm,
  DepartmentUpdateForm,
} from '@/views/system/department/types';

/**
 * 部門管理 API
 */
export const departmentApi = {
  /**
   * 查詢所有部門列表（扁平結構）
   */
  queryAllDepartment: (): Promise<ResponseDTO<DepartmentVO[]>> => {
    return request.get('/department/listAll');
  },

  /**
   * 查詢部門樹（樹形結構）
   */
  queryDepartmentTree: (): Promise<ResponseDTO<DepartmentVO[]>> => {
    return request.get('/department/treeList');
  },

  /**
   * 新增部門
   * @param params 新增表單數據
   */
  addDepartment: (params: DepartmentAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/department/add', params);
  },

  /**
   * 更新部門
   * @param params 更新表單數據
   */
  updateDepartment: (params: DepartmentUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/department/update', params);
  },

  /**
   * 刪除部門
   * @param departmentId 部門ID
   */
  deleteDepartment: (departmentId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/department/delete/${departmentId}`);
  },
};
