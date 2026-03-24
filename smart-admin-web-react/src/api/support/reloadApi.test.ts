/**
 * Reload API Tests
 * 重載 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { reloadApi } from './reloadApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('reloadApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('queryList', () => {
    it('應該調用 GET /support/reload/query', async () => {
      const mockResponse = {
        code: 200,
        data: [
          {
            tag: 'test',
            identification: 'id1',
            args: 'args1',
            createTime: '2024-01-01',
            updateTime: '2024-01-02',
          },
        ],
        ok: true,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await reloadApi.queryList();

      expect(request.get).toHaveBeenCalledWith('/support/reload/query');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('reload', () => {
    it('應該調用 POST /support/reload/update 並傳遞表單數據', async () => {
      const mockFormData = {
        tag: 'test-tag',
        identification: 'test-id',
        args: 'test-args',
      };

      const mockResponse = {
        code: 200,
        data: null,
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await reloadApi.reload(mockFormData);

      expect(request.post).toHaveBeenCalledWith('/support/reload/update', mockFormData);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('queryReloadResult', () => {
    it('應該調用 GET /support/reload/result/:tag', async () => {
      const tag = 'test-tag';
      const mockResponse = {
        code: 200,
        data: [{ id: 1, tag: 'test-tag', args: 'args', result: true, createTime: '2024-01-01' }],
        ok: true,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await reloadApi.queryReloadResult(tag);

      expect(request.get).toHaveBeenCalledWith(`/support/reload/result/${tag}`);
      expect(result).toEqual(mockResponse);
    });
  });
});
