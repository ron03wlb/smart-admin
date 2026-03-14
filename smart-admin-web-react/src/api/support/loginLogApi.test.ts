/**
 * Login Log API Unit Tests
 * 登錄日誌 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { loginLogApi } from './loginLogApi';
import type { LoginLogVO, LoginLogQueryForm } from '@/views/support/login-log/types';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('loginLogApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryPage 測試 ====================

  describe('queryPage', () => {
    it('should call POST /support/loginLog/page/query with correct parameters', async () => {
      const queryForm: LoginLogQueryForm = {
        userName: 'testUser',
        ip: '192.168.1.1',
        startDate: '2024-01-01',
        endDate: '2024-12-31',
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

      const result = await loginLogApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/loginLog/page/query', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle empty query form', async () => {
      const queryForm: LoginLogQueryForm = {
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

      const result = await loginLogApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/loginLog/page/query', queryForm);
      expect(result.code).toBe(200);
    });

    it('should return login log list data correctly', async () => {
      const queryForm: LoginLogQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockLoginLogList: LoginLogVO[] = [
        {
          loginLogId: 1,
          userId: 100,
          userName: '張三',
          userType: 1,
          loginIp: '192.168.1.100',
          loginIpRegion: '中國 北京',
          userAgent: 'Mozilla/5.0',
          loginResult: 0,
          remark: '登錄成功',
          createTime: '2024-03-12T10:00:00',
        },
      ];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: mockLoginLogList,
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginLogApi.queryPage(queryForm);

      expect(result.data.list).toHaveLength(1);
      expect(result.data.list[0].userName).toBe('張三');
      expect(result.data.list[0].loginResult).toBe(0);
      expect(result.data.total).toBe(1);
    });
  });

  // ==================== queryPageLogin 測試 ====================

  describe('queryPageLogin', () => {
    it('should call POST /support/loginLog/page/query/login with correct parameters', async () => {
      const queryForm: LoginLogQueryForm = {
        userName: 'currentUser',
        startDate: '2024-01-01',
        endDate: '2024-12-31',
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

      const result = await loginLogApi.queryPageLogin(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/loginLog/page/query/login', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle minimal query form', async () => {
      const queryForm: LoginLogQueryForm = {
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

      const result = await loginLogApi.queryPageLogin(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/loginLog/page/query/login', queryForm);
      expect(result.code).toBe(200);
    });

    it('should return current user login log list correctly', async () => {
      const queryForm: LoginLogQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockLoginLogList: LoginLogVO[] = [
        {
          loginLogId: 2,
          userId: 200,
          userName: '李四',
          userType: 1,
          loginIp: '192.168.1.200',
          loginIpRegion: '中國 上海',
          userAgent: 'Chrome/120.0',
          loginResult: 0,
          remark: '個人登錄記錄',
          createTime: '2024-03-12T11:00:00',
        },
      ];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: mockLoginLogList,
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await loginLogApi.queryPageLogin(queryForm);

      expect(result.data.list).toHaveLength(1);
      expect(result.data.list[0].userName).toBe('李四');
      expect(result.data.list[0].remark).toBe('個人登錄記錄');
      expect(result.data.total).toBe(1);
    });
  });
});
