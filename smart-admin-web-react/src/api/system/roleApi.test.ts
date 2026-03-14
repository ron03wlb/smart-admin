/**
 * Role API Unit Tests
 * 角色 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { roleApi } from './roleApi';
import type { RoleVO, RoleAddForm, RoleUpdateForm } from '@/views/system/role/types';
import type { ResponseDTO } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('roleApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryAll Tests ====================

  describe('queryAll', () => {
    it('should call GET /role/getAll', async () => {
      const mockResponse: ResponseDTO<RoleVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            roleId: 1,
            roleName: '超級管理員',
            roleCode: 'admin',
            remark: '擁有所有權限',
            createTime: '2026-03-11 10:00:00',
            updateTime: '2026-03-11 10:00:00',
          },
          {
            roleId: 2,
            roleName: '普通用戶',
            roleCode: 'user',
            remark: '普通用戶權限',
            createTime: '2026-03-11 11:00:00',
            updateTime: '2026-03-11 11:00:00',
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await roleApi.queryAll();

      expect(request.get).toHaveBeenCalledWith('/role/getAll');
      expect(result).toEqual(mockResponse);
      expect(result.data).toHaveLength(2);
    });

    it('should handle empty role list', async () => {
      const mockResponse: ResponseDTO<RoleVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await roleApi.queryAll();

      expect(result.data).toHaveLength(0);
    });
  });

  // ==================== addRole Tests ====================

  describe('addRole', () => {
    it('should call POST /role/add', async () => {
      const addForm: RoleAddForm = {
        roleName: '測試角色',
        roleCode: 'test_role',
        remark: '這是一個測試角色',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await roleApi.addRole(addForm);

      expect(request.post).toHaveBeenCalledWith('/role/add', addForm);
      expect(result.ok).toBe(true);
    });

    it('should handle add role without remark', async () => {
      const addForm: RoleAddForm = {
        roleName: '簡單角色',
        roleCode: 'simple_role',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await roleApi.addRole(addForm);

      expect(request.post).toHaveBeenCalledWith('/role/add', addForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== updateRole Tests ====================

  describe('updateRole', () => {
    it('should call POST /role/update', async () => {
      const updateForm: RoleUpdateForm = {
        roleId: 5,
        roleName: '更新後的角色',
        roleCode: 'updated_role',
        remark: '更新後的備註',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await roleApi.updateRole(updateForm);

      expect(request.post).toHaveBeenCalledWith('/role/update', updateForm);
      expect(result.ok).toBe(true);
    });

    it('should handle update role without remark', async () => {
      const updateForm: RoleUpdateForm = {
        roleId: 10,
        roleName: '修改的角色',
        roleCode: 'modified_role',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await roleApi.updateRole(updateForm);

      expect(request.post).toHaveBeenCalledWith('/role/update', updateForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== deleteRole Tests ====================

  describe('deleteRole', () => {
    it('should call GET /role/delete/:roleId', async () => {
      const roleId = 99;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await roleApi.deleteRole(roleId);

      expect(request.get).toHaveBeenCalledWith('/role/delete/99');
      expect(result.ok).toBe(true);
    });

    it('should handle delete role with different ID', async () => {
      const roleId = 123;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await roleApi.deleteRole(roleId);

      expect(request.get).toHaveBeenCalledWith('/role/delete/123');
      expect(result.ok).toBe(true);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in queryAll', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(roleApi.queryAll()).rejects.toThrow('Network Error');
    });

    it('should handle API errors in addRole', async () => {
      const addForm: RoleAddForm = {
        roleName: '測試',
        roleCode: 'test',
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(roleApi.addRole(addForm)).rejects.toThrow('Validation Error');
    });

    it('should handle API errors in updateRole', async () => {
      const updateForm: RoleUpdateForm = {
        roleId: 1,
        roleName: '測試',
        roleCode: 'test',
      };

      const mockError = new Error('Update Failed');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(roleApi.updateRole(updateForm)).rejects.toThrow('Update Failed');
    });

    it('should handle API errors in deleteRole', async () => {
      const mockError = new Error('Role has associated users');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(roleApi.deleteRole(1)).rejects.toThrow('Role has associated users');
    });
  });
});
