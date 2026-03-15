/**
 * Heart Beat API Test
 * 心跳記錄 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { heartBeatApi } from './heartBeatApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('heartBeatApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('queryList', () => {
    it('應該調用正確的 API 端點', async () => {
      const mockParams = {
        keywords: 'test',
        startDate: '2026-01-01',
        endDate: '2026-01-31',
        pageNum: 1,
        pageSize: 10,
      };

      await heartBeatApi.queryList(mockParams);

      expect(request.post).toHaveBeenCalledWith('/support/heartBeat/query', mockParams);
    });
  });
});
