/**
 * Operate Log API Unit Tests
 * 操作日誌 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { operateLogApi } from './operateLogApi';
import type { OperateLogVO, OperateLogQueryForm } from '@/views/support/operate-log/types';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('operateLogApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryPage 測試 ====================

  describe('queryPage', () => {
    it('should call POST /support/operateLog/page/query with correct parameters', async () => {
      const queryForm: OperateLogQueryForm = {
        keywords: 'test',
        requestKeywords: 'api',
        userName: 'admin',
        successFlag: 1,
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

      const result = await operateLogApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/operateLog/page/query', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should return operate log list data correctly', async () => {
      const queryForm: OperateLogQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockOperateLogList: OperateLogVO[] = [
        {
          operateLogId: 1,
          operateUserId: 100,
          operateUserType: 1,
          operateUserName: '張三',
          module: '系統管理',
          content: '登錄系統',
          url: '/api/login',
          method: 'POST',
          param: '{"username":"zhangsan"}',
          response: '{"ok":true,"msg":"Success"}',
          ip: '192.168.1.100',
          ipRegion: '中國 北京',
          userAgent: 'Mozilla/5.0',
          successFlag: 1,
          createTime: '2024-03-13T10:00:00',
          updateTime: '2024-03-13T10:00:00',
        },
      ];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: mockOperateLogList,
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await operateLogApi.queryPage(queryForm);

      expect(result.data.list).toHaveLength(1);
      expect(result.data.list[0].operateUserName).toBe('張三');
      expect(result.data.list[0].successFlag).toBe(1);
      expect(result.data.total).toBe(1);
    });
  });

  // ==================== detail 測試 ====================

  describe('detail', () => {
    it('should call GET /support/operateLog/detail/:id with correct parameters', async () => {
      const operateLogId = 1;

      const mockDetailData: OperateLogVO = {
        operateLogId: 1,
        operateUserId: 100,
        operateUserType: 1,
        operateUserName: '張三',
        module: '系統管理',
        content: '登錄系統',
        url: '/api/login',
        method: 'POST',
        param: '{"username":"zhangsan"}',
        response: '{"ok":true,"msg":"Success"}',
        ip: '192.168.1.100',
        ipRegion: '中國 北京',
        userAgent: 'Mozilla/5.0',
        successFlag: 1,
        createTime: '2024-03-13T10:00:00',
        updateTime: '2024-03-13T10:00:00',
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: mockDetailData,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await operateLogApi.detail(operateLogId);

      expect(request.get).toHaveBeenCalledWith('/support/operateLog/detail/1');
      expect(result).toEqual(mockResponse);
      expect(result.data.operateLogId).toBe(1);
    });

    it('should return detail data with all fields', async () => {
      const operateLogId = 2;

      const mockDetailData: OperateLogVO = {
        operateLogId: 2,
        operateUserId: 200,
        operateUserType: 2,
        operateUserName: '李四',
        module: '商品管理',
        content: '添加商品',
        url: '/api/goods/add',
        method: 'POST',
        param: '{"name":"測試商品"}',
        response: '{"ok":true,"msg":"Success","data":{"id":123}}',
        ip: '192.168.1.200',
        ipRegion: '中國 上海',
        userAgent: 'Chrome/120.0',
        successFlag: 1,
        failReason: undefined,
        createTime: '2024-03-13T11:00:00',
        updateTime: '2024-03-13T11:00:00',
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: mockDetailData,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await operateLogApi.detail(operateLogId);

      expect(result.data.param).toBe('{"name":"測試商品"}');
      expect(result.data.response).toContain('Success');
    });
  });

  // ==================== queryPageLogin 測試 ====================

  describe('queryPageLogin', () => {
    it('should call POST /support/operateLog/page/query/login with correct parameters', async () => {
      const queryForm: OperateLogQueryForm = {
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

      const result = await operateLogApi.queryPageLogin(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/operateLog/page/query/login', queryForm);
      expect(result).toEqual(mockResponse);
    });
  });
});
