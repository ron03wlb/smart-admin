/**
 * Employee API
 * 員工 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/system/employee-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import request from '@/utils/request';
import { ResponseDTO } from '@/types/response';

/**
 * 員工基本信息
 */
export interface EmployeeVO {
  employeeId: number;
  actualName: string;
  loginName: string;
  phone?: string;
  departmentId: number;
  departmentName?: string;
  isDisabled: boolean;
  isLeave: boolean;
  remark?: string;
}

/**
 * 員工 API
 */
export const employeeApi = {
  /**
   * 查詢所有員工
   */
  queryAll: (): Promise<ResponseDTO<EmployeeVO[]>> => {
    return request.get('/employee/queryAll');
  },

  /**
   * 查詢員工（分頁）
   * @param params 查詢參數
   */
  queryEmployee: (params: any): Promise<ResponseDTO<any>> => {
    return request.post('/employee/query', params);
  },

  /**
   * 根據部門 ID 查詢員工
   * @param departmentId 部門 ID
   */
  queryEmployeeByDeptId: (departmentId: number): Promise<ResponseDTO<EmployeeVO[]>> => {
    return request.get(`/employee/getAllEmployeeByDepartmentId/${departmentId}`);
  },

  /**
   * 添加員工
   * @param params 員工信息
   */
  addEmployee: (params: any): Promise<ResponseDTO<void>> => {
    return request.post('/employee/add', params);
  },

  /**
   * 更新員工信息
   * @param params 員工信息
   */
  updateEmployee: (params: any): Promise<ResponseDTO<void>> => {
    return request.post('/employee/update', params);
  },

  /**
   * 刪除員工
   * @param employeeId 員工 ID
   */
  deleteEmployee: (employeeId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/employee/delete/${employeeId}`);
  },

  /**
   * 批量刪除員工
   * @param employeeIdList 員工 ID 列表
   */
  batchDeleteEmployee: (employeeIdList: number[]): Promise<ResponseDTO<void>> => {
    return request.post('/employee/update/batch/delete', employeeIdList);
  },

  /**
   * 更新員工禁用狀態
   * @param employeeId 員工 ID
   */
  updateDisabled: (employeeId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/employee/update/disabled/${employeeId}`);
  },

  /**
   * 重置員工密碼
   * @param employeeId 員工 ID
   */
  resetPassword: (employeeId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/employee/update/password/reset/${employeeId}`);
  },
};
