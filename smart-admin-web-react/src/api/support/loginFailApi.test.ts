/**
 * Login Fail API Unit Tests
 * 登錄失敗 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { loginFailApi } from './loginFailApi';
import type { LoginFailVO, LoginFailQueryForm } from '@/views/support/login-fail/types';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('loginFailApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryPage 測試 ====================

  describe('queryPage', () => {
    it('should call POST /support/protect/loginFail/queryPage with correct parameters', async () => {
      const queryForm: LoginFailQueryForm = {
        loginName: 'testUser',
        lockFlag: 1,
        loginLockBeginTimeBegin: '2024-01-01',
        loginLockBeginTimeEnd: '2024-12-31',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginFailApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/protect/loginFail/queryPage', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle empty query form', async () => {
      const queryForm: LoginFailQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginFailApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/protect/loginFail/queryPage', queryForm);
      expect(result.code).toBe(200);
    });

    it('should return login fail list data correctly', async () => {
      const queryForm: LoginFailQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockLoginFailList: LoginFailVO[] = [
        {
          loginFailId: 1,
          userId: 100,
          userName: '張三',
          userType: 1,
          loginName: 'zhangsan',
          loginFailCount: 5,
          lockFlag: 1,
          loginLockBeginTime: '2024-03-12T10:00:00',
          createTime: '2024-03-12T09:00:00',
          updateTime: '2024-03-12T10:00:00',
        },
      ];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: mockLoginFailList,
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginFailApi.queryPage(queryForm);

      expect(result.data.list).toHaveLength(1);
      expect(result.data.list[0].loginName).toBe('zhangsan');
      expect(result.data.list[0].lockFlag).toBe(1);
      expect(result.data.total).toBe(1);
    });
  });

  // ==================== batchDelete 測試 ====================

  describe('batchDelete', () => {
    it('should call POST /support/protect/loginFail/batchDelete with correct parameters', async () => {
      const idList = [1, 2, 3];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Batch unlock successful',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginFailApi.batchDelete(idList);

      expect(request.post).toHaveBeenCalledWith('/support/protect/loginFail/batchDelete', idList);
      expect(result).toEqual(mockResponse);
    });

    it('should handle single ID deletion', async () => {
      const idList = [1];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Unlock successful',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginFailApi.batchDelete(idList);

      expect(request.post).toHaveBeenCalledWith('/support/protect/loginFail/batchDelete', idList);
      expect(result.code).toBe(200);
    });

    it('should return success message on successful unlock', async () => {
      const idList = [1, 2];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Unlock successful',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginFailApi.batchDelete(idList);

      expect(result.ok).toBe(true);
      expect(result.data).toBe('Unlock successful');
    });
  });
});
