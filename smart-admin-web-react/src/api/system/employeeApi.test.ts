/**
 * Employee API Unit Tests
 * 員工 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { employeeApi } from './employeeApi';
import type { EmployeeVO, EmployeeQueryForm, EmployeeAddForm, EmployeeUpdateForm } from '@/views/system/employee/types';
import type { ResponseDTO, PageResult } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('employeeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryAll Tests ====================

  describe('queryAll', () => {
    it('should call GET /employee/queryAll', async () => {
      const mockResponse: ResponseDTO<EmployeeVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            employeeId: 1,
            actualName: '張三',
            gender: 1,
            loginName: 'zhangsan',
            phone: '13800138000',
            email: 'zhangsan@example.com',
            administratorFlag: false,
            disabledFlag: false,
            leaveFlag: false,
            departmentId: 1,
            departmentName: '技術部',
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await employeeApi.queryAll();

      expect(request.get).toHaveBeenCalledWith('/employee/queryAll');
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== queryEmployee Tests ====================

  describe('queryEmployee', () => {
    it('should call POST /employee/query with query params', async () => {
      const queryForm: EmployeeQueryForm = {
        keyword: '張三',
        disabledFlag: false,
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<EmployeeVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          pageNum: 1,
          pageSize: 10,
          total: 1,
          pages: 1,
          list: [
            {
              employeeId: 1,
              actualName: '張三',
              gender: 1,
              loginName: 'zhangsan',
              phone: '13800138000',
              administratorFlag: false,
              disabledFlag: false,
              leaveFlag: false,
              departmentId: 1,
            },
          ],
          emptyFlag: false,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await employeeApi.queryEmployee(queryForm);

      expect(request.post).toHaveBeenCalledWith('/employee/query', queryForm);
      expect(result).toEqual(mockResponse);
      expect(result.data.list).toHaveLength(1);
    });

    it('should handle empty result', async () => {
      const queryForm: EmployeeQueryForm = {
        keyword: 'nonexistent',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<EmployeeVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          pageNum: 1,
          pageSize: 10,
          total: 0,
          pages: 0,
          list: [],
          emptyFlag: true,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await employeeApi.queryEmployee(queryForm);

      expect(result.data.list).toHaveLength(0);
      expect(result.data.emptyFlag).toBe(true);
    });
  });

  // ==================== queryEmployeeByDeptId Tests ====================

  describe('queryEmployeeByDeptId', () => {
    it('should call GET /employee/getAllEmployeeByDepartmentId/:id', async () => {
      const departmentId = 5;

      const mockResponse: ResponseDTO<EmployeeVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            employeeId: 1,
            actualName: '張三',
            gender: 1,
            loginName: 'zhangsan',
            phone: '13800138000',
            administratorFlag: false,
            disabledFlag: false,
            leaveFlag: false,
            departmentId: 5,
            departmentName: '研發部',
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await employeeApi.queryEmployeeByDeptId(departmentId);

      expect(request.get).toHaveBeenCalledWith(`/employee/getAllEmployeeByDepartmentId/${departmentId}`);
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== addEmployee Tests ====================

  describe('addEmployee', () => {
    it('should call POST /employee/add and return password', async () => {
      const addForm: EmployeeAddForm = {
        actualName: '李四',
        phone: '13900139000',
        departmentId: 1,
        loginName: 'lisi',
        email: 'lisi@example.com',
        gender: 1,
        disabledFlag: 0,
        leaveFlag: 0,
        roleIdList: [1, 2],
      };

      const mockResponse: ResponseDTO<string> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: 'Abc123!@#', // 初始密碼
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await employeeApi.addEmployee(addForm);

      expect(request.post).toHaveBeenCalledWith('/employee/add', addForm);
      expect(result.data).toBe('Abc123!@#');
    });
  });

  // ==================== updateEmployee Tests ====================

  describe('updateEmployee', () => {
    it('should call POST /employee/update', async () => {
      const updateForm: EmployeeUpdateForm = {
        employeeId: 1,
        actualName: '張三（更新）',
        phone: '13800138000',
        departmentId: 2,
        loginName: 'zhangsan',
        email: 'zhangsan.new@example.com',
        gender: 1,
        disabledFlag: 0,
        leaveFlag: 0,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await employeeApi.updateEmployee(updateForm);

      expect(request.post).toHaveBeenCalledWith('/employee/update', updateForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== deleteEmployee Tests ====================

  describe('deleteEmployee', () => {
    it('should call GET /employee/delete/:id', async () => {
      const employeeId = 10;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await employeeApi.deleteEmployee(employeeId);

      expect(request.get).toHaveBeenCalledWith(`/employee/delete/${employeeId}`);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== batchDeleteEmployee Tests ====================

  describe('batchDeleteEmployee', () => {
    it('should call POST /employee/update/batch/delete with ID list', async () => {
      const employeeIdList = [1, 2, 3, 4, 5];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await employeeApi.batchDeleteEmployee(employeeIdList);

      expect(request.post).toHaveBeenCalledWith('/employee/update/batch/delete', employeeIdList);
      expect(result.ok).toBe(true);
    });

    it('should handle empty ID list', async () => {
      const employeeIdList: number[] = [];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await employeeApi.batchDeleteEmployee(employeeIdList);

      expect(request.post).toHaveBeenCalledWith('/employee/update/batch/delete', employeeIdList);
    });
  });

  // ==================== updateDisabled Tests ====================

  describe('updateDisabled', () => {
    it('should call GET /employee/update/disabled/:id', async () => {
      const employeeId = 3;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await employeeApi.updateDisabled(employeeId);

      expect(request.get).toHaveBeenCalledWith(`/employee/update/disabled/${employeeId}`);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== resetPassword Tests ====================

  describe('resetPassword', () => {
    it('should call GET /employee/update/password/reset/:id and return new password', async () => {
      const employeeId = 7;

      const mockResponse: ResponseDTO<string> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: 'NewPass123!', // 新密碼
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await employeeApi.resetPassword(employeeId);

      expect(request.get).toHaveBeenCalledWith(`/employee/update/password/reset/${employeeId}`);
      expect(result.data).toBe('NewPass123!');
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in queryEmployee', async () => {
      const queryForm: EmployeeQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockError = new Error('Network Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(employeeApi.queryEmployee(queryForm)).rejects.toThrow('Network Error');
    });

    it('should handle API errors in addEmployee', async () => {
      const addForm: EmployeeAddForm = {
        actualName: '測試',
        phone: '13800138000',
        departmentId: 1,
        loginName: 'test',
        gender: 1,
        disabledFlag: 0,
        leaveFlag: 0,
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(employeeApi.addEmployee(addForm)).rejects.toThrow('Validation Error');
    });
  });
});
