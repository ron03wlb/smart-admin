/**
 * Employee API
 * 員工 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/system/employee-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  EmployeeVO,
  EmployeeQueryForm,
  EmployeeAddForm,
  EmployeeUpdateForm,
} from '@/views/system/employee/types';

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
  queryEmployee: (params: EmployeeQueryForm): Promise<ResponseDTO<PageResult<EmployeeVO>>> => {
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
   * @returns 返回新員工的初始密碼
   */
  addEmployee: (params: EmployeeAddForm): Promise<ResponseDTO<string>> => {
    return request.post('/employee/add', params);
  },

  /**
   * 更新員工信息
   * @param params 員工信息
   */
  updateEmployee: (params: EmployeeUpdateForm): Promise<ResponseDTO<void>> => {
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
   * @returns 返回新密碼
   */
  resetPassword: (employeeId: number): Promise<ResponseDTO<string>> => {
    return request.get(`/employee/update/password/reset/${employeeId}`);
  },

  /**
   * 獲取單個員工信息
   * @param employeeId 員工 ID
   */
  getEmployee: (employeeId: number): Promise<ResponseDTO<EmployeeVO>> => {
    return request.get(`/employee/${employeeId}`);
  },

  /**
   * 更新員工密碼
   * @param params 密碼信息
   */
  updateEmployeePassword: (params: {
    oldPassword: string;
    newPassword: string;
  }): Promise<ResponseDTO<void>> => {
    return request.post('/employee/update/password', params);
  },

  /**
   * 獲取密碼複雜度配置
   */
  getPasswordComplexityEnabled: (): Promise<ResponseDTO<boolean>> => {
    return request.get('/employee/security/password/complexity/enabled');
  },
};

/**
 * 重新導出類型：用於測試和其他模塊
 */
export type { EmployeeVO, EmployeeQueryForm, EmployeeAddForm, EmployeeUpdateForm };
