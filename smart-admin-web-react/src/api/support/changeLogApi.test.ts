/**
 * ChangeLog API Tests
 * 系統更新日誌 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import request from '@/utils/request';
import { changeLogApi } from './changeLogApi';
import type { ChangeLogQueryForm, ChangeLogAddForm, ChangeLogUpdateForm } from '@/views/support/change-log/types';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe('changeLogApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('queryPage', () => {
    it('should call POST /support/changeLog/queryPage with correct params', async () => {
      const params: ChangeLogQueryForm = {
        type: 1,
        keyword: 'test',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse = {
        code: 1,
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await changeLogApi.queryPage(params);

      expect(request.post).toHaveBeenCalledWith('/support/changeLog/queryPage', params);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getDetail', () => {
    it('should call GET /support/changeLog/getDetail/:id', async () => {
      const changeLogId = 123;
      const mockResponse = {
        code: 1,
        data: {
          changeLogId: 123,
          updateVersion: 'v1.0.0',
          type: 1,
          publishAuthor: 'Admin',
          publicDate: '2026-03-13',
          content: 'Test content',
        },
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await changeLogApi.getDetail(changeLogId);

      expect(request.get).toHaveBeenCalledWith(`/support/changeLog/getDetail/${changeLogId}`);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('add', () => {
    it('should call POST /support/changeLog/add with correct params', async () => {
      const params: ChangeLogAddForm = {
        updateVersion: 'v1.0.0',
        type: 1,
        publishAuthor: 'Admin',
        publicDate: '2026-03-13',
        content: 'Test content',
        link: 'https://example.com',
      };

      const mockResponse = { code: 1, data: null };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await changeLogApi.add(params);

      expect(request.post).toHaveBeenCalledWith('/support/changeLog/add', params);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('update', () => {
    it('should call POST /support/changeLog/update with correct params', async () => {
      const params: ChangeLogUpdateForm = {
        changeLogId: 123,
        updateVersion: 'v1.0.1',
        type: 2,
        publishAuthor: 'Admin',
        publicDate: '2026-03-13',
        content: 'Updated content',
        link: 'https://example.com',
      };

      const mockResponse = { code: 1, data: null };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await changeLogApi.update(params);

      expect(request.post).toHaveBeenCalledWith('/support/changeLog/update', params);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('delete', () => {
    it('should call GET /support/changeLog/delete/:id', async () => {
      const changeLogId = 123;
      const mockResponse = { code: 1, data: null };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await changeLogApi.delete(changeLogId);

      expect(request.get).toHaveBeenCalledWith(`/support/changeLog/delete/${changeLogId}`);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('batchDelete', () => {
    it('should call POST /support/changeLog/batchDelete with ID list', async () => {
      const idList = [123, 456, 789];
      const mockResponse = { code: 1, data: null };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await changeLogApi.batchDelete(idList);

      expect(request.post).toHaveBeenCalledWith('/support/changeLog/batchDelete', idList);
      expect(result).toEqual(mockResponse);
    });
  });
});
