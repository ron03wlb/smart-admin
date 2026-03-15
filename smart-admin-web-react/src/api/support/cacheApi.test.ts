/**
 * Cache API Test
 * 緩存 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { cacheApi } from './cacheApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('cacheApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAllCacheNames', () => {
    it('應該調用正確的 API 端點', async () => {
      await cacheApi.getAllCacheNames();

      expect(request.get).toHaveBeenCalledWith('/support/cache/names');
    });
  });

  describe('remove', () => {
    it('應該調用正確的 API 端點', async () => {
      const cacheName = 'testCache';

      await cacheApi.remove(cacheName);

      expect(request.get).toHaveBeenCalledWith(`/support/cache/remove/${cacheName}`);
    });
  });

  describe('getKeys', () => {
    it('應該調用正確的 API 端點', async () => {
      const cacheName = 'testCache';

      await cacheApi.getKeys(cacheName);

      expect(request.get).toHaveBeenCalledWith(`/support/cache/keys/${cacheName}`);
    });
  });
});
