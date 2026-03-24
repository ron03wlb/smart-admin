/**
 * Department API Unit Tests
 * 部門 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { departmentApi } from './departmentApi';
import type {
  DepartmentVO,
  DepartmentAddForm,
  DepartmentUpdateForm,
} from '@/views/system/department/types';
import type { ResponseDTO } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('departmentApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryAllDepartment Tests ====================

  describe('queryAllDepartment', () => {
    it('should call GET /department/listAll', async () => {
      const mockResponse: ResponseDTO<DepartmentVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            departmentId: 1,
            departmentName: '技術部',
            parentId: 0,
            managerId: 1,
            managerName: '張三',
            sort: 100,
            createTime: '2026-01-01 10:00:00',
            updateTime: '2026-01-02 10:00:00',
          },
          {
            departmentId: 2,
            departmentName: '前端組',
            parentId: 1,
            managerId: 2,
            managerName: '李四',
            sort: 50,
            createTime: '2026-01-03 10:00:00',
            updateTime: '2026-01-04 10:00:00',
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await departmentApi.queryAllDepartment();

      expect(request.get).toHaveBeenCalledWith('/department/listAll');
      expect(result).toEqual(mockResponse);
      expect(result.data).toHaveLength(2);
    });

    it('should handle empty department list', async () => {
      const mockResponse: ResponseDTO<DepartmentVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await departmentApi.queryAllDepartment();

      expect(result.data).toHaveLength(0);
    });
  });

  // ==================== queryDepartmentTree Tests ====================

  describe('queryDepartmentTree', () => {
    it('should call GET /department/treeList', async () => {
      const mockResponse: ResponseDTO<DepartmentVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            departmentId: 1,
            departmentName: '技術部',
            parentId: 0,
            sort: 100,
            children: [
              {
                departmentId: 2,
                departmentName: '前端組',
                parentId: 1,
                sort: 50,
              },
              {
                departmentId: 3,
                departmentName: '後端組',
                parentId: 1,
                sort: 40,
              },
            ],
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await departmentApi.queryDepartmentTree();

      expect(request.get).toHaveBeenCalledWith('/department/treeList');
      expect(result).toEqual(mockResponse);
      expect(result.data[0].children).toHaveLength(2);
    });
  });

  // ==================== addDepartment Tests ====================

  describe('addDepartment', () => {
    it('should call POST /department/add', async () => {
      const addForm: DepartmentAddForm = {
        departmentName: '新部門',
        parentId: 1,
        managerId: 5,
        sort: 30,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await departmentApi.addDepartment(addForm);

      expect(request.post).toHaveBeenCalledWith('/department/add', addForm);
      expect(result.ok).toBe(true);
    });

    it('should handle add with minimal fields', async () => {
      const addForm: DepartmentAddForm = {
        departmentName: '最小部門',
        parentId: 0,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await departmentApi.addDepartment(addForm);

      expect(request.post).toHaveBeenCalledWith('/department/add', addForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== updateDepartment Tests ====================

  describe('updateDepartment', () => {
    it('should call POST /department/update', async () => {
      const updateForm: DepartmentUpdateForm = {
        departmentId: 10,
        departmentName: '更新後的部門',
        parentId: 2,
        managerId: 8,
        sort: 60,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await departmentApi.updateDepartment(updateForm);

      expect(request.post).toHaveBeenCalledWith('/department/update', updateForm);
      expect(result.ok).toBe(true);
    });

    it('should handle update with optional fields', async () => {
      const updateForm: DepartmentUpdateForm = {
        departmentId: 5,
        departmentName: '簡化更新',
        parentId: 0,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await departmentApi.updateDepartment(updateForm);

      expect(result.ok).toBe(true);
    });
  });

  // ==================== deleteDepartment Tests ====================

  describe('deleteDepartment', () => {
    it('should call GET /department/delete/:id', async () => {
      const departmentId = 15;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await departmentApi.deleteDepartment(departmentId);

      expect(request.get).toHaveBeenCalledWith(`/department/delete/${departmentId}`);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in queryAllDepartment', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(departmentApi.queryAllDepartment()).rejects.toThrow('Network Error');
    });

    it('should handle API errors in addDepartment', async () => {
      const addForm: DepartmentAddForm = {
        departmentName: '測試',
        parentId: 0,
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(departmentApi.addDepartment(addForm)).rejects.toThrow('Validation Error');
    });

    it('should handle API errors in deleteDepartment', async () => {
      const mockError = new Error('Department has children');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(departmentApi.deleteDepartment(999)).rejects.toThrow('Department has children');
    });
  });

  // ==================== Tree Structure Tests ====================

  describe('Tree Structure Data', () => {
    it('should handle nested tree structure correctly', async () => {
      const mockResponse: ResponseDTO<DepartmentVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            departmentId: 1,
            departmentName: '總部',
            parentId: 0,
            sort: 100,
            children: [
              {
                departmentId: 2,
                departmentName: '技術部',
                parentId: 1,
                sort: 90,
                children: [
                  {
                    departmentId: 3,
                    departmentName: '前端組',
                    parentId: 2,
                    sort: 50,
                  },
                ],
              },
            ],
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await departmentApi.queryDepartmentTree();

      expect(result.data[0].children).toHaveLength(1);
      expect(result.data[0].children![0].children).toHaveLength(1);
      expect(result.data[0].children![0].children![0].departmentName).toBe('前端組');
    });
  });
});
