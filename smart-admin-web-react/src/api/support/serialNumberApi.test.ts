/**
 * Serial Number API Tests
 * 單號生成器 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { serialNumberApi } from './serialNumberApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('serialNumberApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('應該調用正確的 GET 端點', async () => {
      const mockData = [
        {
          serialNumberId: 1,
          businessName: '訂單號',
          format: 'ORDER{yyyy}{MM}{dd}{nnnnnn}',
          ruleType: '每日',
          initNumber: 1,
          stepRandomRange: 1,
        },
      ];

      vi.mocked(request.get).mockResolvedValue({
        ok: true,
        code: 1,
        msg: '操作成功',
        data: mockData,
      });

      const result = await serialNumberApi.getAll();

      expect(request.get).toHaveBeenCalledWith('/support/serialNumber/all');
      expect(result.data).toEqual(mockData);
    });
  });

  describe('generate', () => {
    it('應該調用正確的 POST 端點並傳遞表單數據', async () => {
      const form = {
        serialNumberId: 1,
        count: 5,
      };

      const mockData = [
        'ORDER202603140000001',
        'ORDER202603140000002',
        'ORDER202603140000003',
        'ORDER202603140000004',
        'ORDER202603140000005',
      ];

      vi.mocked(request.post).mockResolvedValue({
        ok: true,
        code: 1,
        msg: '生成成功',
        data: mockData,
      });

      const result = await serialNumberApi.generate(form);

      expect(request.post).toHaveBeenCalledWith('/support/serialNumber/generate', form);
      expect(result.data).toEqual(mockData);
      expect(result.data).toHaveLength(5);
    });
  });

  describe('queryRecord', () => {
    it('應該調用正確的 POST 端點並返回分頁結果', async () => {
      const form = {
        serialNumberId: 1,
        pageNum: 1,
        pageSize: 10,
      };

      const mockData = {
        list: [
          {
            serialNumberId: 1,
            recordDate: '2026-03-14',
            count: 10,
            lastNumber: 10,
            lastTime: '2026-03-14 15:30:00',
          },
        ],
        total: 1,
        pageNum: 1,
        pageSize: 10,
      };

      vi.mocked(request.post).mockResolvedValue({
        ok: true,
        code: 1,
        msg: '操作成功',
        data: mockData,
      });

      const result = await serialNumberApi.queryRecord(form);

      expect(request.post).toHaveBeenCalledWith('/support/serialNumber/queryRecord', form);
      expect(result.data).toEqual(mockData);
      expect(result.data.list).toHaveLength(1);
    });
  });
});
